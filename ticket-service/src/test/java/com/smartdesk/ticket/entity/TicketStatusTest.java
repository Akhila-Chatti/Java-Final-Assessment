package com.smartdesk.ticket.entity;

import com.smartdesk.ticket.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static com.smartdesk.ticket.entity.TicketStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class TicketStatusTest {

    @Test
    void happyPath() {
        assertEquals(CLOSED, OPEN.moveTo(IN_PROGRESS).moveTo(WAITING_ON_EMPLOYEE).moveTo(RESOLVED).moveTo(CLOSED));
    }

    @Test
    void illegalJumpsAreRejected() {
        assertThrows(InvalidStatusTransitionException.class, () -> OPEN.moveTo(CLOSED));
        assertThrows(InvalidStatusTransitionException.class, () -> IN_PROGRESS.moveTo(RESOLVED)); // needs employee confirmation
    }

    @Test
    void rejectedFixGoesBackToAgent() {
        assertEquals(IN_PROGRESS, WAITING_ON_EMPLOYEE.moveTo(IN_PROGRESS));
    }

    @Test
    void reportStormEscalatesPriority() {
        assertEquals(Priority.HIGH, Priority.LOW.escalateFor(5));
        assertEquals(Priority.CRITICAL, Priority.MEDIUM.escalateFor(15));
        assertEquals(Priority.CRITICAL, Priority.CRITICAL.escalateFor(1));
    }
}
