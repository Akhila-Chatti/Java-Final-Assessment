package com.smartdesk.ticket.dto;

import com.smartdesk.ticket.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(@NotNull TicketStatus status) {}
