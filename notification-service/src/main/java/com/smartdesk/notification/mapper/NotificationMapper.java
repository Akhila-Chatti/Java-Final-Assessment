package com.smartdesk.notification.mapper;

import com.smartdesk.notification.dto.NotificationResponse;
import com.smartdesk.notification.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {}

    public static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getNotificationId(), n.getTicketId(), n.getEventType(), n.getSubject(),
                n.getMessage(), n.getDeliveryStatus(), n.getCreatedAt());
    }
}
