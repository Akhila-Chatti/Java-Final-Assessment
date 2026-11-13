package com.smartdesk.ticket.service;

import com.smartdesk.ticket.entity.AiAnalysis;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketStatus;
import com.smartdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static com.smartdesk.ticket.dto.client.NotificationType.STATUS_CHANGED;

/** "Everyone reports the wifi": duplicates join one incident instead of flooding the pool. */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final TicketRepository tickets;
    private final Notifier notifier;

    @Value("${smartdesk.dedup-window}")
    private Duration dedupWindow;

    /** Must run inside the create transaction. */
    public void linkIfDuplicate(Ticket ticket) {
        tickets.openIncident(ticket.getDepartmentName(), ticket.getIssueKey(), Instant.now().minus(dedupWindow))
                .ifPresent(incident -> link(ticket, incident));
    }

    private void link(Ticket ticket, Ticket incident) {
        ticket.setParentTicketId(incident.getTicketId());
        ticket.setStatus(incident.getStatus());
        tickets.incrementReports(incident.getTicketId());
        AiAnalysis a = incident.getAnalysis();
        // ponytail: last-writer-wins on priority; escalation only goes up, so a lost update is harmless
        a.setPriority(a.getPriority().escalateFor(incident.getReportCount() + 1));
    }

    /** Linked tickets mirror their incident; requesters hear once, when it is resolved. */
    public void syncLinked(Ticket incident) {
        for (Ticket child : tickets.findByParentTicketId(incident.getTicketId())) {
            child.setStatus(incident.getStatus());
            child.setResolvedAt(incident.getResolvedAt());
            if (incident.getStatus() == TicketStatus.RESOLVED)
                notifier.toRequester(STATUS_CHANGED, child, Map.of("status", "RESOLVED", "actor", "incident " + incident.getCode()));
        }
    }
}
