package com.smartdesk.ticket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** 1:1 with tickets; the unique ticket_id is a second guard against double-claiming. */
@Entity
@Table(name = "ticket_assignments",
        indexes = @Index(name = "idx_assignments_agent_day", columnList = "user_id,assigned_at")) // per-agent daily count
@Getter
@Setter
@NoArgsConstructor
public class TicketAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID assignmentId;

    @OneToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Column(nullable = false)
    private UUID userId; // the agent

    @Column(nullable = false)
    private String agentEmail;

    private String agentName;

    @Column(nullable = false)
    private UUID assignedBy;

    @CreationTimestamp
    private Instant assignedAt;

    private Instant completedAt;
}
