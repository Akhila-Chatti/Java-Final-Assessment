package com.smartdesk.gateway.controller;

import com.smartdesk.gateway.dto.*;
import com.smartdesk.gateway.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** URLs and JSON follow INTEGRATION_SPEC. No passwords anywhere: an emailed OTP proves who you are. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    /** New user: creates an inactive EMPLOYEE and emails an activation OTP. */
    @PostMapping("/register/request")
    public ApiResponse requestRegistration(@Valid @RequestBody RegisterRequest r) {
        return auth.requestRegistration(r.fullName(), r.email());
    }

    /** OTP correct -> account active + token. */
    @PostMapping("/register/verify")
    public TokenResponse verifyRegistration(@Valid @RequestBody OtpRequest r) {
        return auth.verifyRegistration(r.email(), r.otp());
    }

    /** Login step 1: emails a login OTP to an active account. */
    @PostMapping("/otp/request")
    public ApiResponse requestOtp(@Valid @RequestBody LoginRequest r) {
        return auth.requestLogin(r.email());
    }

    /** Login step 2: OTP -> token. */
    @PostMapping("/otp/verify")
    public TokenResponse verifyOtp(@Valid @RequestBody OtpRequest r) {
        return auth.verifyLogin(r.email(), r.otp());
    }
}
