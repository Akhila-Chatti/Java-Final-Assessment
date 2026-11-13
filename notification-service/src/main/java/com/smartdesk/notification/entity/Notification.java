package com.smartdesk.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Delivery audit log. */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_recipient_created", columnList = "recipient_user_id,created_at"))
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID notificationId;

    @Column(nullable = false, unique = true) // idempotency: a retried event is stored and sent once
    private UUID eventId;

    @Column(nullable = false)
    private UUID recipientUserId;

    @Column(nullable = false)
    private String recipientEmail;

    private Long ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventType eventType;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @CreationTimestamp
    private Instant createdAt;
}
