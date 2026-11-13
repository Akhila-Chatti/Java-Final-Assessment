package com.smartdesk.ticket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Incidents are not stored: tickets with the same department + issueKey that arrive within 4 hours of an
 * earlier, still unresolved report belong to the same incident (see IncidentService). The 4 hours here
 * must match IncidentService.WINDOW.
 */
@Entity
@Table(name = "tickets",
        indexes = {
                @Index(name = "idx_tickets_dept_status_created", columnList = "department_name,status,created_at"), // pool, dept lists
                @Index(name = "idx_tickets_creator_created", columnList = "created_by,created_at"),                 // my tickets
                @Index(name = "idx_tickets_dept_created", columnList = "department_name,created_at"),               // today's inflow (claim limit)
                @Index(name = "idx_tickets_incident", columnList = "department_name,issue_key,created_at")},        // incident grouping
        uniqueConstraints = @UniqueConstraint(name = "uk_tickets_idempotency", columnNames = {"created_by", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    /** Shown as "SD-1048"; Postgres numbers it on insert. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private UUID createdBy; // the requester; name/email come from the auth service (UserDirectory)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Department departmentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status = TicketStatus.OPEN;

    /** AI label of the underlying problem ("wifi-outage"). Same key + department = same incident. */
    @Column(length = 60)
    private String issueKey;

    @Column(length = 100)
    private String idempotencyKey;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant resolvedAt;

    /** Not a column: an earlier report of the same problem was still unresolved when this one arrived. */
    @Formula("(exists (select 1 from tickets p where p.department_name = department_name and p.issue_key = issue_key"
            + " and p.created_at < created_at and created_at - p.created_at <= '4 hours'"
            + " and (p.resolved_at is null or p.resolved_at > created_at)))")
    private boolean linked;

    /** Not a column: this ticket + the later reports of the same problem made while it was unresolved. */
    @Formula("(select count(*) + 1 from tickets c where c.department_name = department_name and c.issue_key = issue_key"
            + " and c.created_at > created_at and c.created_at - created_at <= '4 hours'"
            + " and (resolved_at is null or c.created_at < resolved_at))")
    private int reportCount;

    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL)
    private AiAnalysis analysis;

    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL)
    private TicketAssignment assignment;

    public String getCode() {
        return "SD-" + ticketId;
    }

    public boolean isAssignedTo(UUID userId) {
        return assignment != null && assignment.getAgentId().equals(userId);
    }

    /** Saved by the fallback while the AI was down, and not re-classified (or overridden) since. */
    public boolean isNeedsRetriage() {
        return analysis != null && analysis.getSource() == TriageSource.FALLBACK;
    }

    public void moveTo(TicketStatus next) {
        status = status.moveTo(next);
        if (next == TicketStatus.RESOLVED) resolvedAt = Instant.now();
    }

    /** The category decides which department pool the ticket lands in. */
    public void applyTriage(Department category, Priority priority, String summary, String issueKey, TriageSource source) {
        if (analysis == null) {
            analysis = new AiAnalysis();
            analysis.setTicket(this);
        }
        analysis.setCategory(category);
        analysis.setPriority(priority);
        analysis.setAiSummary(summary);
        analysis.setSource(source);
        analysis.setCompletedAt(Instant.now());
        this.departmentName = category;
        this.issueKey = issueKey;
    }

    public void assign(TicketAssignment newAssignment) {
        newAssignment.setTicket(this);
        assignment = newAssignment;
    }
}
