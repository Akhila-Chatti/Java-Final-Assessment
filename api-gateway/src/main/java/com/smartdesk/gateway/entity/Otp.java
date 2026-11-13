package com.smartdesk.gateway.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otps", indexes = @Index(name = "idx_otps_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class Otp {

    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String codeHash; // BCrypt, never the plain code

    @Column(nullable = false)
    private Instant expiresAt;

    private int attempts;

    private boolean used;

    public Otp(UUID userId, String codeHash, Instant expiresAt) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public boolean usable() {
        return !used && attempts < MAX_ATTEMPTS && Instant.now().isBefore(expiresAt);
    }
}
