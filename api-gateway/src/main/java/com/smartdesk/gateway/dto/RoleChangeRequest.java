package com.smartdesk.gateway.dto;

import com.smartdesk.gateway.entity.Department;
import com.smartdesk.gateway.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(@NotNull Role role, Department department) {}
