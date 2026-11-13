package com.smartdesk.gateway.config;

import com.smartdesk.gateway.entity.AppUser;
import com.smartdesk.gateway.entity.Role;
import com.smartdesk.gateway.repository.UserRepository;
import com.smartdesk.gateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Inserts the one hard-coded ADMIN on first start, so somebody can promote others. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository users;

    @Value("${smartdesk.admin.email}")
    private String email;

    @Override
    public void run(ApplicationArguments args) {
        String normalized = AuthService.normalize(email);
        if (users.existsByEmail(normalized)) return;
        users.save(new AppUser("System Admin", normalized, Role.ADMIN));
        log.info("Seeded initial admin account {}", normalized);
    }
}
