package com.smartdesk.notification.dto;

import com.smartdesk.notification.entity.DeliveryStatus;
import com.smartdesk.notification.entity.EventType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, Long ticketId, EventType eventType, String subject, String message,
                                   DeliveryStatus deliveryStatus, Instant createdAt) {}
