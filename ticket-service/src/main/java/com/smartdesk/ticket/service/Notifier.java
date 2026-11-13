package com.smartdesk.ticket.service;

import com.smartdesk.ticket.dto.client.NotificationEvent;
import com.smartdesk.ticket.dto.client.NotificationType;
import com.smartdesk.ticket.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Publishes events; NotificationPublisher delivers them after commit. */
@Component
@RequiredArgsConstructor
public class Notifier {

    private final ApplicationEventPublisher events;

    public void toRequester(NotificationType type, Ticket t, Map<String, String> params) {
        send(type, t.getCreatedBy(), t, params);
    }

    public void toAgent(NotificationType type, Ticket t, Map<String, String> params) {
        send(type, t.getAssignment().getAgentId(), t, params);
    }

    /** Email and name are left empty here; NotificationPublisher looks them up after commit, off the request thread. */
    private void send(NotificationType type, UUID to, Ticket t, Map<String, String> extra) {
        Map<String, String> params = new HashMap<>(extra);
        params.put("code", t.getCode());
        params.put("title", t.getTitle());
        events.publishEvent(new NotificationEvent(UUID.randomUUID(), type, to, null, t.getTicketId(), params));
    }
}
