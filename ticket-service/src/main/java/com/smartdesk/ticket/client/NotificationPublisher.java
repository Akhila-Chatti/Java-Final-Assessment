package com.smartdesk.ticket.client;

import com.smartdesk.ticket.dto.client.NotificationEvent;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends only AFTER the ticket change commits (no emails about rolled-back changes), on a background
 * thread (the HTTP request never waits), retried 3x (safe thanks to eventId dedup on the other side).
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final NotificationClient client;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    @Retry(name = "notify")
    // ponytail: after 3 failed retries the email is only logged; add a transactional outbox if every email must survive
    public void on(NotificationEvent event) {
        client.send(event);
    }
}
