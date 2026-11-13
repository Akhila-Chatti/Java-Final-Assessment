package com.smartdesk.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "Email is required") @Email(message = "Must be a valid email address") String email) {}
