package com.smartdesk.ticket.repository;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Ticket;
import com.smartdesk.ticket.entity.TicketStatus;
import com.smartdesk.ticket.entity.TriageSource;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

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
    Page<Ticket> findByDepartmentNameInAndStatusAndLinkedFalse(Collection<Department> departments, TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"analysis", "assignment"})
    @Query("select t from Ticket t join t.assignment a where a.agentId = :agentId and t.status in :statuses")
    Page<Ticket> findAssigned(UUID agentId, Collection<TicketStatus> statuses, Pageable pageable);

    /** Reports of the same problem made in [since, at) that were still unresolved at `at`, oldest first. */
    @Query("""
            select p from Ticket p
            where p.departmentName = :dept and p.issueKey = :key and p.createdAt >= :since and p.createdAt < :at
              and (p.resolvedAt is null or p.resolvedAt > :at)
            order by p.createdAt""")
    List<Ticket> activeReports(Department dept, String key, Instant since, Instant at, Limit limit);

    /** A null key never matches: no AI label, no incident. */
    default Optional<Ticket> earliestActiveReport(Department dept, String key, Instant since, Instant at) {
        return key == null ? Optional.empty() : activeReports(dept, key, since, at, Limit.of(1)).stream().findFirst();
    }

    List<Ticket> findByDepartmentNameAndIssueKeyAndCreatedAtAfterOrderByCreatedAt(Department dept, String key, Instant after);

    /** Re-triage work list: saved by the fallback (AI down) and still OPEN. */
    List<Ticket> findTop20ByAnalysisSourceAndStatus(TriageSource source, TicketStatus status);

    /** Today's real workload for a department (linked duplicates excluded). */
    long countByDepartmentNameAndLinkedFalseAndCreatedAtGreaterThanEqual(Department department, Instant since);

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
