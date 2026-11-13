package com.smartdesk.ticket.entity;

import com.smartdesk.ticket.exception.InvalidStatusTransitionException;

import java.util.Map;
import java.util.Set;

/**
 * The whole lifecycle in one table; any other jump is rejected.
 * OPEN -> IN_PROGRESS -> WAITING_ON_EMPLOYEE -> RESOLVED -> CLOSED
 *               ^                 |
 *               +--- rejected ----+
 */
public enum TicketStatus {
    OPEN, IN_PROGRESS, WAITING_ON_EMPLOYEE, RESOLVED, CLOSED;

    private static final Map<TicketStatus, Set<TicketStatus>> NEXT = Map.of(
            OPEN, Set.of(IN_PROGRESS),
            IN_PROGRESS, Set.of(WAITING_ON_EMPLOYEE),
            WAITING_ON_EMPLOYEE, Set.of(RESOLVED, IN_PROGRESS),
            RESOLVED, Set.of(CLOSED),
            CLOSED, Set.of());

    public TicketStatus moveTo(TicketStatus next) {
        if (!NEXT.get(this).contains(next)) throw new InvalidStatusTransitionException(this, next);
        return next;
    }
}
