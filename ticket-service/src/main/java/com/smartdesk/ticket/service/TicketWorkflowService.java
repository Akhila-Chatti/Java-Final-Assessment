package com.smartdesk.ticket.service;

import com.smartdesk.ticket.dto.AssignTicketRequest;
import com.smartdesk.ticket.dto.ClaimLimitResponse;
import com.smartdesk.ticket.dto.TriageOverrideRequest;
import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketAssignment;
import com.smartdesk.ticket.entity.TicketStatus;
import com.smartdesk.ticket.entity.TriageSource;
import com.smartdesk.ticket.exception.ApiException;
import com.smartdesk.ticket.security.Caller;
import com.smartdesk.ticket.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

import static com.smartdesk.ticket.dto.client.NotificationType.*;
import static com.smartdesk.ticket.entity.TicketStatus.*;

/** Pool -> bucket -> fix -> employee confirmation -> close. */
@Service
@RequiredArgsConstructor
public class TicketWorkflowService {

    /** Who may set which status via PATCH /status. Everything else goes through claim/assign/verify. */
    private static final Map<TicketStatus, BiPredicate<Caller, Ticket>> PATCH_RULES = Map.of(
            WAITING_ON_EMPLOYEE, (c, t) -> t.isAssignedTo(c.id()),
            CLOSED, (c, t) -> t.getCreatedBy().equals(c.id()) || c.manages(t.getDepartmentName()));

    private final TicketAccess access;
    private final AssignmentLimitService limits;
    private final IncidentService incidents;
    private final Notifier notifier;

    /** Agent takes a pool ticket into their own bucket (subject to the daily limit). */
    @Transactional
    public Ticket claim(Caller c, UUID id) {
        c.requireAny(Role.AGENT);
        Ticket t = access.load(id);
        if (!c.worksIn(t.getDepartmentName())) throw ApiException.forbidden("Ticket belongs to another department");
        return startWork(t, c.id(), c.email(), c.name(), c);
    }

    /** Manager pushes a pool ticket to a specific agent (same daily limit applies to that agent). */
    @Transactional
    public Ticket assign(Caller c, UUID id, AssignTicketRequest req) {
        c.requireAny(Role.MANAGER, Role.ADMIN);
        Ticket t = access.load(id);
        if (!c.manages(t.getDepartmentName())) throw ApiException.forbidden("Ticket belongs to another department");
        return startWork(t, req.agentId(), req.agentEmail(), req.agentName(), c);
    }

    /** Single entry point for claim and assign, so the limit can't be bypassed. */
    private Ticket startWork(Ticket t, UUID agentId, String agentEmail, String agentName, Caller by) {
        TicketAccess.requireStandalone(t);
        if (t.getDepartmentName() == Department.UNCLASSIFIED) throw ApiException.conflict("Set a category first (PATCH /triage)");
        limits.requireCapacity(agentId, t.getDepartmentName());
        t.moveTo(IN_PROGRESS); // only OPEN tickets pass; a racing second claim fails on @Version
        TicketAssignment a = new TicketAssignment();
        a.setUserId(agentId);
        a.setAgentEmail(agentEmail);
        a.setAgentName(agentName);
        a.setAssignedBy(by.id());
        t.assign(a);
        incidents.syncLinked(t);
        notifier.toAgent(TICKET_ASSIGNED, t, Map.of("priority", t.getAnalysis().getPriority().name()));
        notifier.toRequester(STATUS_CHANGED, t, Map.of("status", t.getStatus().name(), "actor", by.name()));
        return t;
    }

    public ClaimLimitResponse myLimit(Caller c) {
        c.requireAny(Role.AGENT);
        return limits.status(c.id(), c.department());
    }

    @Transactional
    public Ticket changeStatus(Caller c, UUID id, TicketStatus next) {
        BiPredicate<Caller, Ticket> rule = PATCH_RULES.get(next);
        if (rule == null) throw ApiException.badRequest(next + " is set via /claim, /assign or /verify");
        Ticket t = access.loadVisible(c, id);
        if (!rule.test(c, t)) throw ApiException.forbidden("You cannot move this ticket to " + next);
        t.moveTo(next);
        incidents.syncLinked(t);
        if (next == WAITING_ON_EMPLOYEE) notifier.toRequester(VERIFICATION_REQUESTED, t, Map.of("actor", c.name()));
        else notifier.toRequester(STATUS_CHANGED, t, Map.of("status", next.name(), "actor", c.name()));
        return t;
    }

    /** Only the requester confirms the fix (RESOLVED) or rejects it (back to IN_PROGRESS). */
    @Transactional
    public Ticket verify(Caller c, UUID id, boolean accepted) {
        Ticket t = access.load(id);
        if (!t.getCreatedBy().equals(c.id())) throw ApiException.forbidden("Only the requester can confirm a resolution");
        TicketAccess.requireStandalone(t);
        t.moveTo(accepted ? RESOLVED : IN_PROGRESS);
        if (accepted) t.getAssignment().setCompletedAt(Instant.now());
        incidents.syncLinked(t);
        notifier.toAgent(STATUS_CHANGED, t, Map.of("status", t.getStatus().name(), "actor", c.name()));
        return t;
    }

    /** Human override of the AI; re-routes an OPEN ticket to another department pool. */
    @Transactional
    public Ticket overrideTriage(Caller c, UUID id, TriageOverrideRequest req) {
        if (req.category() == Department.UNCLASSIFIED) throw ApiException.badRequest("Pick a real category");
        Ticket t = access.load(id);
        boolean allowed = c.manages(t.getDepartmentName()) || c.role() == Role.MANAGER && t.getDepartmentName() == Department.UNCLASSIFIED;
        if (!allowed) throw ApiException.forbidden("Not your department");
        if (t.getStatus() != OPEN) throw ApiException.conflict("Only OPEN tickets can be re-routed");
        t.applyTriage(req.category(), req.priority(), t.getAnalysis().getAiSummary(), t.getIssueKey(), TriageSource.MANUAL);
        return t;
    }
}
