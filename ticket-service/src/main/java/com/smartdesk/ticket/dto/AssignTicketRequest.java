package com.smartdesk.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTicketRequest(@NotNull UUID agentId, @NotBlank @Email String agentEmail, String agentName) {}
