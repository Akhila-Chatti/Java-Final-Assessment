package com.smartdesk.gateway.service;

import com.smartdesk.gateway.client.NotificationClient;
import com.smartdesk.gateway.dto.ApiResponse;
import com.smartdesk.gateway.dto.TokenResponse;
import com.smartdesk.gateway.entity.AppUser;
import com.smartdesk.gateway.entity.Otp;
import com.smartdesk.gateway.entity.Role;
import com.smartdesk.gateway.exception.ApiException;
import com.smartdesk.gateway.mapper.UserMapper;
import com.smartdesk.gateway.repository.OtpRepository;
import com.smartdesk.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Passwordless: register -> OTP -> active account; login -> OTP -> JWT.
 * Not @Transactional on purpose: a wrong code must still save the failed-attempt counter, which a rollback would undo.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration TOKEN_TTL = Duration.ofHours(8);

    private final UserRepository users;
    private final OtpRepository otps;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwt;
    private final NotificationClient notifications;

    @Value("${smartdesk.otp.expose-in-response}")
    private boolean exposeOtp;

    /** Self sign-up is always EMPLOYEE; admins/managers promote later. A pending account can ask again (new OTP). */
    public ApiResponse requestRegistration(String fullName, String email) {
        String normalized = normalize(email);
        AppUser user = users.findByEmail(normalized).orElseGet(() -> new AppUser(fullName.strip(), normalized, Role.EMPLOYEE));
        if (AppUser.ACTIVE.equals(user.getAccountStatus()) && user.getUserId() != null)
            throw ApiException.badRequest("An active account already exists with this email address. Please sign in instead.");
        user.setName(fullName.strip());
        user.setAccountStatus(AppUser.PENDING);
        return sendOtp(users.save(user), "Verification code sent to your email.");
    }

    public TokenResponse verifyRegistration(String email, String code) {
        AppUser user = users.findByEmail(normalize(email))
                .filter(u -> AppUser.PENDING.equals(u.getAccountStatus()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or verification code."));
        consumeOtp(user, code);
        user.setAccountStatus(AppUser.ACTIVE);
        return tokenFor(users.save(user));
    }

    public ApiResponse requestLogin(String email) {
        AppUser user = active(email).orElseThrow(() -> ApiException.unauthorized("Email is not registered"));
        return sendOtp(user, "Verification code sent to your email.");
    }

    public TokenResponse verifyLogin(String email, String code) {
        AppUser user = active(email).orElseThrow(() -> ApiException.unauthorized("Invalid email or OTP."));
        consumeOtp(user, code);
        return tokenFor(user);
    }

    private Optional<AppUser> active(String email) {
        return users.findByEmail(normalize(email)).filter(u -> AppUser.ACTIVE.equals(u.getAccountStatus()));
    }

    /** Fresh OTP (older ones deleted), stored only as a BCrypt hash, emailed via the Notification Service. */
    private ApiResponse sendOtp(AppUser user, String message) {
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        otps.deleteByUserId(user.getUserId());
        otps.save(new Otp(user.getUserId(), encoder.encode(code), Instant.now().plus(OTP_TTL)));
        notifications.sendOtp(user, code);
        return ApiResponse.ok(message, exposeOtp ? code : null);
    }

    /** Max Otp.MAX_ATTEMPTS wrong tries, then the code is locked and a new one must be requested. */
    private void consumeOtp(AppUser user, String code) {
        Otp otp = otps.findFirstByUserIdOrderByExpiresAtDesc(user.getUserId()).filter(Otp::usable)
                .orElseThrow(() -> ApiException.unauthorized("No active OTP found or it has expired. Please request a new OTP."));
        if (encoder.matches(code, otp.getCodeHash())) {
            otp.setUsed(true);
            otps.save(otp);
            return;
        }
        otp.setAttempts(otp.getAttempts() + 1);
        otps.save(otp);
        int remaining = Otp.MAX_ATTEMPTS - otp.getAttempts();
        throw ApiException.unauthorized(remaining > 0
                ? "Invalid OTP. Attempts remaining: " + remaining
                : "Maximum verification attempts exceeded. Please request a new OTP.");
    }

    private TokenResponse tokenFor(AppUser user) {
        return new TokenResponse(issueToken(user), "Bearer", UserMapper.toResponse(user));
    }

    private String issueToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRoleName().name())
                .claims(c -> { if (user.getDepartmentName() != null) c.put("dept", user.getDepartmentName().name()); })
                .build();
        return jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
