package com.smartdesk.ticket.service;

import com.smartdesk.ticket.entity.AiAnalysis;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketStatus;
import com.smartdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static com.smartdesk.ticket.dto.client.NotificationType.STATUS_CHANGED;

/**
 * "Everyone reports the wifi": duplicates join one incident instead of flooding the pool.
 * Nothing is stored for this. A ticket is linked when an earlier report with the same department + issueKey
 * arrived at most WINDOW before it and was still unresolved (Ticket.linked). The incident is the first
 * ticket of that chain.
 */
@Service
@RequiredArgsConstructor
public class IncidentService {

    /** Must match the '4 hours' in Ticket's @Formula columns. */
    static final Duration WINDOW = Duration.ofHours(4);

    private final TicketRepository tickets;
    private final Notifier notifier;

    /** Must run inside the create transaction, before the insert. Returns true when the ticket joined an incident. */
    public boolean linkIfDuplicate(Ticket ticket) {
        Instant now = Instant.now();
        Optional<Ticket> incident = tickets.earliestActiveReport(ticket.getDepartmentName(), ticket.getIssueKey(), now.minus(WINDOW), now)
                .map(this::incidentOf);
        incident.ifPresent(i -> join(ticket, i));
        return incident.isPresent();
    }

    private void join(Ticket ticket, Ticket incident) {
        ticket.setStatus(incident.getStatus());
        AiAnalysis a = incident.getAnalysis();
        // ponytail: last-writer-wins on priority; escalation only goes up, so a lost update is harmless
        a.setPriority(a.getPriority().escalateFor(incident.getReportCount() + 1));
    }

    /** The first ticket of the incident this ticket belongs to; the ticket itself when it is not linked. */
    public Ticket incidentOf(Ticket ticket) {
        Ticket current = ticket;
        while (current.isLinked()) // each step goes strictly back in time, so this ends
            current = tickets.earliestActiveReport(current.getDepartmentName(), current.getIssueKey(),
                    current.getCreatedAt().minus(WINDOW), current.getCreatedAt()).orElseThrow();
        return current;
    }

    /** For the API's parentTicketId: null for a standalone ticket. */
    public Long incidentIdOf(Ticket ticket) {
        return ticket.isLinked() ? incidentOf(ticket).getTicketId() : null;
    }

    /** Linked tickets mirror their incident; requesters hear once, when it is resolved. */
    // ponytail: scans later same-problem tickets until the next incident starts; fine at helpdesk volume
    public void syncLinked(Ticket incident) {
        if (incident.isLinked() || incident.getIssueKey() == null) return;
        for (Ticket later : tickets.findByDepartmentNameAndIssueKeyAndCreatedAtAfterOrderByCreatedAt(
                incident.getDepartmentName(), incident.getIssueKey(), incident.getCreatedAt())) {
            if (!later.isLinked()) break; // a new incident of the same problem starts here
            later.setStatus(incident.getStatus());
            later.setResolvedAt(incident.getResolvedAt());
            if (incident.getStatus() == TicketStatus.RESOLVED)
                notifier.toRequester(STATUS_CHANGED, later, Map.of("status", "RESOLVED", "actor", "incident " + incident.getCode()));
        }
    }
}
