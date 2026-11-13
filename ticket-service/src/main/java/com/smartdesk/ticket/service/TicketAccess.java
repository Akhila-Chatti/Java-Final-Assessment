package com.smartdesk.ticket.service;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.exception.ApiException;
import com.smartdesk.ticket.repository.TicketRepository;
import com.smartdesk.ticket.security.Caller;
import com.smartdesk.ticket.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/** One place for "load + who may see it", shared by every service. */
@Component
@RequiredArgsConstructor
public class TicketAccess {

    private final TicketRepository tickets;

    public Ticket load(Long id) {
        return tickets.findById(id).orElseThrow(() -> ApiException.notFound("Ticket not found"));
    }

    /** Requester, staff of the ticket's department, managers (UNCLASSIFIED) and admins. Others get 404, not 403. */
    public Ticket loadVisible(Caller c, Long id) {
        Ticket t = load(id);
        boolean visible = t.getCreatedBy().equals(c.id())
                || c.role() == Role.ADMIN
                || c.worksIn(t.getDepartmentName())
                || c.role() == Role.MANAGER && t.getDepartmentName() == Department.UNCLASSIFIED;
        if (!visible) throw ApiException.notFound("Ticket not found");
        return t;
    }

    public static void requireStandalone(Ticket t) {
        if (t.isLinked()) throw ApiException.conflict("Linked to an incident of the same problem; act on the incident");
    }
}
