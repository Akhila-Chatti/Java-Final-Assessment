package com.smartdesk.ticket.repository;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.TicketAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface TicketAssignmentRepository extends JpaRepository<TicketAssignment, UUID> {

    /** Tickets this agent took today (uses idx_assignments_agent_day). */
    long countByUserIdAndAssignedAtGreaterThanEqual(UUID userId, Instant since);

    /** Agents who worked this department recently = the people today's inflow is shared between. */
    @Query("select count(distinct a.userId) from TicketAssignment a where a.ticket.departmentName = :dept and a.assignedAt >= :since")
    long countActiveAgents(Department dept, Instant since);
}
