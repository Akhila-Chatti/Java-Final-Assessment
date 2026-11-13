package com.smartdesk.ticket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets",
        indexes = {
                @Index(name = "idx_tickets_dept_status_created", columnList = "department_name,status,created_at"), // pool, dept lists
                @Index(name = "idx_tickets_creator_created", columnList = "created_by,created_at"),                 // my tickets
                @Index(name = "idx_tickets_dept_created", columnList = "department_name,created_at"),               // today's inflow (claim limit)
                @Index(name = "idx_tickets_incident", columnList = "department_name,issue_key,status"),             // duplicate lookup
                @Index(name = "idx_tickets_parent", columnList = "parent_ticket_id"),
                @Index(name = "idx_tickets_retriage", columnList = "needs_retriage,status")},
        uniqueConstraints = @UniqueConstraint(name = "uk_tickets_idempotency", columnNames = {"created_by", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ticketId;

    /** "SD-1048"; a Postgres sequence fills it on insert. */
    @Generated
    @Column(name = "ticket_number", columnDefinition = "bigserial", insertable = false, updatable = false)
    private Long ticketNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private UUID createdBy; // logical reference to users in the auth DB

    @Column(nullable = false)
    private String requesterEmail;

    private String requesterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Department departmentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status = TicketStatus.OPEN;

    /** AI label of the underlying problem ("wifi-outage"). Same key + department = same incident. */
    @Column(length = 60)
    private String issueKey;

    /** Set when folded into an existing incident; the ticket then mirrors the incident's status. */
    private UUID parentTicketId;

    private int reportCount = 1;

    private boolean needsRetriage;

    @Column(length = 100)
    private String idempotencyKey;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant resolvedAt;

    @Version
    private long version; // two agents claiming at once -> second gets 409

    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL)
    private AiAnalysis analysis;

    @OneToOne(mappedBy = "ticket", cascade = CascadeType.ALL)
    private TicketAssignment assignment;

    public String getCode() {
        return "SD-" + ticketNumber;
    }

    public boolean isAssignedTo(UUID userId) {
        return assignment != null && assignment.getUserId().equals(userId);
    }

    public boolean isLinked() {
        return parentTicketId != null;
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
        this.needsRetriage = source == TriageSource.FALLBACK;
    }

    public void assign(TicketAssignment newAssignment) {
        newAssignment.setTicket(this);
        assignment = newAssignment;
    }
}
