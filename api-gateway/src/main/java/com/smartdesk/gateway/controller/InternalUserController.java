package com.smartdesk.gateway.controller;

import com.smartdesk.gateway.entity.AppUser;
import com.smartdesk.gateway.exception.ApiException;
import com.smartdesk.gateway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Service-to-service only: other services look up a user's name and email (users live in this service's DB).
 * Needs the shared X-Internal-Key; the JWT filter chain lets /internal/** through for that reason.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    public record UserContact(UUID id, String name, String email) {}

    private final UserRepository users;
    private final byte[] internalKey;

    public InternalUserController(UserRepository users, @Value("${smartdesk.internal-key}") String internalKey) {
        this.users = users;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/{id}")
    public UserContact get(@RequestHeader(name = "X-Internal-Key", required = false) String key, @PathVariable UUID id) {
        if (key == null || !MessageDigest.isEqual(internalKey, key.getBytes(StandardCharsets.UTF_8)))
            throw ApiException.forbidden("Internal endpoint");
        AppUser u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        return new UserContact(u.getUserId(), u.getName(), u.getEmail());
    }
}
