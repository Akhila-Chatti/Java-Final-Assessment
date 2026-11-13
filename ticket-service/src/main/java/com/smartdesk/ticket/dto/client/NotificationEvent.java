package com.smartdesk.ticket.dto.client;

import java.util.Map;
import java.util.UUID;

/** eventId is the idempotency key: the Notification Service stores and sends a retried event only once. */
public record NotificationEvent(UUID eventId, NotificationType eventType, UUID recipientUserId, String recipientEmail,
                                Long ticketId, Map<String, String> params) {}
