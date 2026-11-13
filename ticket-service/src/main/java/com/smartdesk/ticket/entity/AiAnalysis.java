package com.smartdesk.ticket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** 1:1 with tickets (unique ticket_id); re-triage and overrides update this row. */
@Entity
@Table(name = "ai_analysis_results", indexes = @Index(name = "idx_analysis_category", columnList = "category"))
@Getter
@Setter
@NoArgsConstructor
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resultId;

    @OneToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Department category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Column(length = 500)
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TriageSource source;

    private Instant completedAt;
}
