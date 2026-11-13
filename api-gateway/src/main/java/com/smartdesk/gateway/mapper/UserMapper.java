package com.smartdesk.gateway.mapper;

import com.smartdesk.gateway.dto.UserResponse;
import com.smartdesk.gateway.entity.AppUser;
import com.smartdesk.gateway.entity.Role;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(AppUser u) {
        return new UserResponse(u.getUserId(), u.getName(), u.getEmail(), u.getRoleName(), u.getDepartmentName(),
                u.getRoleName() == Role.ADMIN, AppUser.ACTIVE.equals(u.getAccountStatus()), u.getCreatedAt());
    }
}
