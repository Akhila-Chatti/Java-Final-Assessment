package com.smartdesk.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** role/department sent by a client are ignored on purpose: self sign-up is always EMPLOYEE. */
public record RegisterRequest(@NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
                              @NotBlank(message = "Email is required") @Email(message = "Must be a valid email address") String email) {}
