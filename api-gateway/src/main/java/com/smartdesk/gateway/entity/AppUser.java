package com.smartdesk.gateway.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_role_dept", columnList = "role_name,department_name"))
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role roleName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Department departmentName;

    /** PENDING until the registration OTP is verified; only ACTIVE accounts can log in. */
    public static final String ACTIVE = "ACTIVE";
    public static final String PENDING = "PENDING";

    @Column(nullable = false, length = 20)
    private String accountStatus = ACTIVE;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public AppUser(String name, String email, Role role) {
        this.name = name;
        this.email = email;
        this.roleName = role;
    }
}
