package com.smartdesk.notification.service;

import com.smartdesk.notification.entity.DeliveryStatus;
import com.smartdesk.notification.entity.Notification;
import com.smartdesk.notification.repository.NotificationRepository;
import com.smartdesk.notification.sender.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Background email sending: the caller already got its 202. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final EmailSender email;
    private final NotificationRepository repo;

    @Async
    public void deliver(Notification n, String body) {
        DeliveryStatus status = DeliveryStatus.SENT;
        try {
            email.send(n.getRecipientEmail(), n.getSubject(), body);
        } catch (RuntimeException e) {
            status = DeliveryStatus.FAILED;
            log.error("Email to {} failed", n.getRecipientEmail(), e);
        }
        repo.updateStatus(n.getNotificationId(), status);
    }
}
