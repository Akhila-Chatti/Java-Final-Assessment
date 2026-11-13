package com.smartdesk.ticket.service;

import com.smartdesk.ticket.client.TriageGateway;
import com.smartdesk.ticket.dto.CreateTicketRequest;
import com.smartdesk.ticket.dto.client.TriageOutcome;
import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketStatus;
import com.smartdesk.ticket.repository.TicketRepository;
import com.smartdesk.ticket.security.Caller;
import com.smartdesk.ticket.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.smartdesk.ticket.dto.client.NotificationType.TICKET_CREATED;

/** Create + read side. */
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository tickets;
    private final TicketAccess access;
    private final TriageGateway triage;
    private final IncidentService incidents;
    private final Notifier notifier;
    private final TransactionTemplate tx;

    /** Not @Transactional: the slow AI call must not hold a DB connection. Only the insert is transactional. */
    public CreateResult create(Caller c, CreateTicketRequest req, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Ticket> replay = tickets.findByCreatedByAndIdempotencyKey(c.id(), idempotencyKey);
            if (replay.isPresent()) return new CreateResult(replay.get(), false);
        }
        TriageOutcome r = triage.triage(req.title(), req.description(), c.name());
        Ticket ticket = new Ticket();
        ticket.setTitle(req.title().strip());
        ticket.setDescription(req.description().strip());
        ticket.setCreatedBy(c.id());
        ticket.setIdempotencyKey(idempotencyKey);
        ticket.applyTriage(r.category(), r.priority(), r.summary(), r.issueKey(), r.source());
        boolean linked;
        try {
            linked = Boolean.TRUE.equals(tx.execute(s -> {
                boolean joined = incidents.linkIfDuplicate(ticket);
                tickets.saveAndFlush(ticket);
                return joined;
            }));
        } catch (DataIntegrityViolationException sameKeyRace) { // two concurrent requests, one Idempotency-Key
            if (idempotencyKey == null) throw sameKeyRace;
            return new CreateResult(tickets.findByCreatedByAndIdempotencyKey(c.id(), idempotencyKey).orElseThrow(), false);
        }
        String note = linked ? " Others reported the same problem, so it is being handled as one incident." : "";
        notifier.toRequester(TICKET_CREATED, ticket, Map.of(
                "category", ticket.getAnalysis().getCategory().name(),
                "priority", ticket.getAnalysis().getPriority().name(),
                "note", note));
        return new CreateResult(tickets.findById(ticket.getTicketId()).orElseThrow(), true); // re-read: fills the computed linked/reportCount
    }

    /** Employee: own tickets. Agent/manager: department. Admin: all. mine=true: own tickets for anyone. */
    @Transactional(readOnly = true)
    public Page<Ticket> list(Caller c, boolean mine, TicketStatus status, Department category, Pageable page) {
        UUID owner = mine || c.role() == Role.EMPLOYEE ? c.id() : null;
        Department dept = owner == null && c.role() != Role.ADMIN ? c.department() : null;
        return tickets.search(owner, dept, status, category, page);
    }

    /** Unassigned department pool; linked duplicates are hidden behind their incident. */
    @Transactional(readOnly = true)
    public Page<Ticket> pool(Caller c, Pageable page) {
        c.requireAny(Role.AGENT, Role.MANAGER, Role.ADMIN);
        Collection<Department> scope = switch (c.role()) {
            case ADMIN -> EnumSet.allOf(Department.class);
            case MANAGER -> EnumSet.of(c.department(), Department.UNCLASSIFIED);
            default -> EnumSet.of(c.department());
        };
        return tickets.findByDepartmentNameInAndStatusAndLinkedFalse(scope, TicketStatus.OPEN, page);
    }

    /** The agent's own bucket. */
    @Transactional(readOnly = true)
    public Page<Ticket> assignedToMe(Caller c, Pageable page) {
        return tickets.findAssigned(c.id(), List.of(TicketStatus.IN_PROGRESS, TicketStatus.WAITING_ON_EMPLOYEE), page);
    }

    @Transactional(readOnly = true)
    public Ticket get(Caller c, Long id) {
        return access.loadVisible(c, id);
    }
}
