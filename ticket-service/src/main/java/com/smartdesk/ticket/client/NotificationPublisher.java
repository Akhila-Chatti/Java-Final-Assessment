package com.smartdesk.ticket.client;

import com.smartdesk.ticket.dto.client.NotificationEvent;
import com.smartdesk.ticket.dto.client.UserContact;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Sends only AFTER the ticket change commits (no emails about rolled-back changes), on a background
 * thread (the HTTP request never waits), retried 3x (safe thanks to eventId dedup on the other side).
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final NotificationClient client;
    private final UserDirectory users;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    @Retry(name = "notify")
    // ponytail: after 3 failed retries the email is only logged; add a transactional outbox if every email must survive
    public void on(NotificationEvent event) {
        UserContact to = users.find(event.recipientUserId());
        if (to == null) throw new IllegalStateException("No contact details for user " + event.recipientUserId()); // -> retried
        Map<String, String> params = new HashMap<>(event.params());
        params.put("name", Objects.toString(to.name(), ""));
        client.send(new NotificationEvent(event.eventId(), event.eventType(), event.recipientUserId(), to.email(), event.ticketId(), params));
    }
}
