package com.smartdesk.ticket.dto;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Priority;
import com.smartdesk.ticket.entity.TicketStatus;
import com.smartdesk.ticket.entity.TriageSource;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(UUID id, String code, String title, String description, TicketStatus status,
                             Department department, Department category, Priority priority, String summary,
                             TriageSource triageSource, UUID requesterId, String requesterName,
                             UUID assigneeId, String assigneeName, UUID parentTicketId, int reportCount,
                             Instant createdAt, Instant resolvedAt) {}
