package com.smartdesk.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/** OTP requests answer with this (INTEGRATION_SPEC). otp is filled only when smartdesk.otp.expose-in-response=true (demo/Postman). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(String message, int status, LocalDateTime timestamp, String otp) {

    public static ApiResponse ok(String message, String otp) {
        return new ApiResponse(message, 200, LocalDateTime.now(), otp);
    }
}
