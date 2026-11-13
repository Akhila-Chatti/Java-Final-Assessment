package com.smartdesk.notification.service;

import com.smartdesk.notification.dto.SendNotificationRequest;
import com.smartdesk.notification.entity.Notification;
import com.smartdesk.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final NotificationDispatcher dispatcher;

    /** Store as PENDING, then send in the background. */
    public void accept(SendNotificationRequest r) {
        Map<String, String> params = r.params() == null ? Map.of() : r.params();
        String body = r.eventType().body(params);
        Notification n = new Notification();
        n.setEventId(r.eventId());
        n.setRecipientUserId(r.recipientUserId());
        n.setRecipientEmail(r.recipientEmail());
        n.setTicketId(r.ticketId());
        n.setEventType(r.eventType());
        n.setSubject(r.eventType().subject(params));
        n.setMessage(r.eventType().isSensitive() ? "[redacted]" : body); // OTP codes never stored
        try {
            repo.saveAndFlush(n);
        } catch (DataIntegrityViolationException alreadyHandled) {
            return; // same eventId retried: already stored and sent
        }
        dispatcher.deliver(n, body);
    }

    public Page<Notification> mine(UUID userId, Pageable page) {
        return repo.findByRecipientUserIdOrderByCreatedAtDesc(userId, page);
    }
}
