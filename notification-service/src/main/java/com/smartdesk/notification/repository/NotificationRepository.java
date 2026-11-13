package com.smartdesk.notification.repository;

import com.smartdesk.notification.entity.DeliveryStatus;
import com.smartdesk.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    @Transactional
    @Modifying
    @Query("update Notification n set n.deliveryStatus = :status where n.notificationId = :id")
    void updateStatus(UUID id, DeliveryStatus status);
}
