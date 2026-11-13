package com.smartdesk.ticket.repository;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<TicketStatus> ACTIVE = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

    Optional<Ticket> findByCreatedByAndIdempotencyKey(UUID createdBy, String idempotencyKey);

    // EntityGraph: analysis + assignment in the same SQL, no N+1 on list pages
    @EntityGraph(attributePaths = {"analysis", "assignment"})
    @Query("""
            select t from Ticket t left join t.analysis a
            where (:createdBy is null or t.createdBy = :createdBy)
              and (:dept is null or t.departmentName = :dept)
              and (:status is null or t.status = :status)
              and (:category is null or a.category = :category)""")
    Page<Ticket> search(UUID createdBy, Department dept, TicketStatus status, Department category, Pageable pageable);

    @EntityGraph(attributePaths = {"analysis", "assignment"})
    Page<Ticket> findByDepartmentNameInAndStatusAndParentTicketIdIsNull(Collection<Department> departments, TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"analysis", "assignment"})
    @Query("select t from Ticket t join t.assignment a where a.userId = :agentId and t.status in :statuses")
    Page<Ticket> findAssigned(UUID agentId, Collection<TicketStatus> statuses, Pageable pageable);

    Optional<Ticket> findFirstByDepartmentNameAndIssueKeyAndParentTicketIdIsNullAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
            Department department, String issueKey, Collection<TicketStatus> statuses, Instant since);

    /** A null key never matches (Spring Data would otherwise turn it into IS NULL). */
    default Optional<Ticket> openIncident(Department department, String issueKey, Instant since) {
        return issueKey == null
                ? Optional.empty()
                : findFirstByDepartmentNameAndIssueKeyAndParentTicketIdIsNullAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(department, issueKey, ACTIVE, since);
    }

    List<Ticket> findByParentTicketId(UUID parentTicketId);

    List<Ticket> findTop20ByNeedsRetriageTrueAndStatus(TicketStatus status);

    /** Today's real workload for a department (linked duplicates excluded). */
    long countByDepartmentNameAndParentTicketIdIsNullAndCreatedAtGreaterThanEqual(Department department, Instant since);

    /** Atomic SQL increment: safe when many people report the same outage at once. */
    @Modifying
    @Query("update Ticket t set t.reportCount = t.reportCount + 1 where t.ticketId = :id")
    void incrementReports(UUID id);

    @Query("select t.status, count(t) from Ticket t where (:dept is null or t.departmentName = :dept) group by t.status")
    List<Object[]> countByStatus(Department dept);

    @Query("select a.category, count(a) from AiAnalysis a where (:dept is null or a.ticket.departmentName = :dept) group by a.category")
    List<Object[]> countByCategory(Department dept);

    @Query(nativeQuery = true, value = """
            select avg(extract(epoch from (resolved_at - created_at))) / 3600
            from tickets
            where resolved_at is not null and (cast(:dept as text) is null or department_name = cast(:dept as text))""")
    Double avgResolutionHours(String dept);
}
