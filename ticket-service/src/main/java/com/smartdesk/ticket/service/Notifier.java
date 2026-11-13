package com.smartdesk.ticket.service;

import com.smartdesk.ticket.dto.client.NotificationEvent;
import com.smartdesk.ticket.dto.client.NotificationType;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Publishes events; NotificationPublisher delivers them after commit. */
@Component
@RequiredArgsConstructor
public class Notifier {

    private final ApplicationEventPublisher events;

    public void toRequester(NotificationType type, Ticket t, Map<String, String> params) {
        send(type, t.getCreatedBy(), t.getRequesterEmail(), t.getRequesterName(), t, params);
    }

    public void toAgent(NotificationType type, Ticket t, Map<String, String> params) {
        TicketAssignment a = t.getAssignment();
        send(type, a.getUserId(), a.getAgentEmail(), a.getAgentName(), t, params);
    }

    private void send(NotificationType type, UUID to, String email, String name, Ticket t, Map<String, String> extra) {
        Map<String, String> params = new HashMap<>(extra);
        params.put("name", Objects.toString(name, ""));
        params.put("code", t.getCode());
        params.put("title", t.getTitle());
        events.publishEvent(new NotificationEvent(UUID.randomUUID(), type, to, email, t.getTicketId(), params));
    }
}
