package com.smartdesk.ticket.service;

import com.smartdesk.ticket.dto.ManagerSummaryResponse;
import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.repository.TicketRepository;
import com.smartdesk.ticket.security.Caller;
import com.smartdesk.ticket.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TicketRepository tickets;

    /** Managers always get their own department; admins may pick one or see all. */
    @Transactional(readOnly = true)
    public ManagerSummaryResponse summary(Caller c, Department requested) {
        c.requireAny(Role.MANAGER, Role.ADMIN);
        Department dept = c.role() == Role.ADMIN ? requested : c.department();
        return new ManagerSummaryResponse(toMap(tickets.countByStatus(dept)), toMap(tickets.countByCategory(dept)),
                tickets.avgResolutionHours(dept == null ? null : dept.name()));
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> r[0].toString(), r -> (Long) r[1]));
    }
}
