package com.smartdesk.ticket.dto;

import jakarta.validation.constraints.NotNull;

/** accepted=true -> RESOLVED, false -> back to the agent (IN_PROGRESS). */
public record VerificationRequest(@NotNull Boolean accepted) {}
