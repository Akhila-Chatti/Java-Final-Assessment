package com.smartdesk.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequest(@NotBlank(message = "Email is required") @Email(message = "Must be a valid email address") String email,
                         @NotBlank(message = "OTP is required") @Pattern(regexp = "\\d{6}", message = "OTP must be a 6-digit numeric code") String otp) {}
