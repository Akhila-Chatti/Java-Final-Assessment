package com.smartdesk.gateway.controller;

import com.smartdesk.gateway.dto.RoleChangeRequest;
import com.smartdesk.gateway.dto.UserResponse;
import com.smartdesk.gateway.entity.Department;
import com.smartdesk.gateway.entity.Role;
import com.smartdesk.gateway.mapper.UserMapper;
import com.smartdesk.gateway.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** ADMIN and MANAGER only (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService users;

    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal Jwt caller,
                                   @RequestParam(required = false) Role role,
                                   @RequestParam(required = false) Department department) {
        return users.list(caller, role, department).stream().map(UserMapper::toResponse).toList();
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@AuthenticationPrincipal Jwt caller, @PathVariable UUID id, @Valid @RequestBody RoleChangeRequest change) {
        return UserMapper.toResponse(users.changeRole(caller, id, change));
    }
}
