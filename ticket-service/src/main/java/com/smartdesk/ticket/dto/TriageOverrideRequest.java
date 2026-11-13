package com.smartdesk.ticket.dto;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.entity.Priority;
import jakarta.validation.constraints.NotNull;

public record TriageOverrideRequest(@NotNull Department category, @NotNull Priority priority) {}
