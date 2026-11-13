package com.smartdesk.ticket.service;

import org.junit.jupiter.api.Test;

import static com.smartdesk.ticket.service.AssignmentLimitService.dailyLimit;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentLimitServiceTest {

    // base 5, buffer 2, max 25
    @Test
    void quietDayUsesBase() {
        assertEquals(5, dailyLimit(10, 4, 5, 2, 25)); // fair share 3 + 2 = 5
    }

    @Test
    void limitGrowsWithInflow() {
        assertEquals(12, dailyLimit(40, 4, 5, 2, 25)); // fair share 10 + 2
        assertEquals(13, dailyLimit(41, 4, 5, 2, 25)); // ceil(41/4)=11 + 2
    }

    @Test
    void neverAboveMax() {
        assertEquals(25, dailyLimit(500, 4, 5, 2, 25));
    }

    @Test
    void noActiveAgentsYetCountsAsOne() {
        assertEquals(10, dailyLimit(8, 0, 5, 2, 25));
    }
}
