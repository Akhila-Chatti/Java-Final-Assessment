package com.smartdesk.ticket.service;

import com.smartdesk.ticket.dto.ClaimLimitResponse;
import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.exception.DailyLimitExceededException;
import com.smartdesk.ticket.repository.TicketAssignmentRepository;
import com.smartdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Daily per-agent cap so nobody hoards the pool into their bucket. The cap grows with the day's inflow:
 *
 *   fairShare = ceil(department tickets raised today / agents active in the department)
 *   limit     = clamp(fairShare + buffer, base, max)
 *
 * Quiet day (10 tickets, 4 agents): fair share 3 -> limit = base 5.
 * Wifi-storm day (120 tickets, 4 agents): fair share 30 -> limit = max 25.
 */
@Service
@RequiredArgsConstructor
public class AssignmentLimitService {

    private final TicketRepository tickets;
    private final TicketAssignmentRepository assignments;

    @Value("${smartdesk.claim-limit.base}")
    private int base;
    @Value("${smartdesk.claim-limit.buffer}")
    private int buffer;
    @Value("${smartdesk.claim-limit.max}")
    private int max;
    @Value("${smartdesk.claim-limit.active-agent-lookback}")
    private Duration lookback;
    @Value("${smartdesk.claim-limit.zone}")
    private ZoneId zone;

    public ClaimLimitResponse status(UUID agentId, Department department) {
        Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        long inflow = tickets.countByDepartmentNameAndLinkedFalseAndCreatedAtGreaterThanEqual(department, startOfDay);
        long agents = assignments.countActiveAgents(department, Instant.now().minus(lookback));
        int limit = dailyLimit(inflow, agents, base, buffer, max);
        long used = assignments.countByAgentIdAndAssignedAtGreaterThanEqual(agentId, startOfDay);
        return new ClaimLimitResponse(used, limit, Math.max(0, limit - used), inflow, agents);
    }

    /** Called on every claim and every manager assignment. */
    // ponytail: two simultaneous claims by one agent can overshoot by 1; add a per-agent row lock if that matters
    public void requireCapacity(UUID agentId, Department department) {
        ClaimLimitResponse s = status(agentId, department);
        if (s.remaining() == 0) throw new DailyLimitExceededException(s.used(), s.limit());
    }

    static int dailyLimit(long inflowToday, long activeAgents, int base, int buffer, int max) {
        long agents = Math.max(1, activeAgents);
        long fairShare = (inflowToday + agents - 1) / agents; // ceil without floating point
        return (int) Math.min(max, Math.max(base, fairShare + buffer));
    }
}
