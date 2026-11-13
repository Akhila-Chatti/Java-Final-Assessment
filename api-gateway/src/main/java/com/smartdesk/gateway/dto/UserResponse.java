package com.smartdesk.gateway.dto;

import com.smartdesk.gateway.entity.Department;
import com.smartdesk.gateway.entity.Role;

import java.time.Instant;
import java.util.UUID;

/** Field names follow INTEGRATION_SPEC (fullName, isRootAdmin, isActive). */
public record UserResponse(UUID id, String fullName, String email, Role role, Department department,
                           boolean isRootAdmin, boolean isActive, Instant createdAt) {}
