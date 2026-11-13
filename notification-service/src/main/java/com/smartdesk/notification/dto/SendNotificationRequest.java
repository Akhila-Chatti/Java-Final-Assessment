package com.smartdesk.notification.dto;

import com.smartdesk.notification.entity.EventType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(@NotNull UUID eventId, @NotNull EventType eventType, @NotNull UUID recipientUserId,
                                      @NotBlank @Email String recipientEmail, Long ticketId, Map<String, String> params) {}
