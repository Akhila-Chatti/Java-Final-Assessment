package com.smartdesk.gateway.repository;

import com.smartdesk.gateway.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {

    Optional<Otp> findFirstByUserIdOrderByExpiresAtDesc(UUID userId);

    @Transactional
    void deleteByUserId(UUID userId);
}
