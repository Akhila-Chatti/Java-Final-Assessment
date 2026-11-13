package com.smartdesk.ticket.security;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.exception.ApiException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** The authenticated user, from the X-User-* headers the API gateway sets after verifying the JWT. */
public record Caller(UUID id, String email, String name, Role role, Department department) {

    /** Department staff: agents and managers of that department. */
    public boolean worksIn(Department d) {
        return (role == Role.AGENT || role == Role.MANAGER) && department == d;
    }

    public boolean manages(Department d) {
        return role == Role.ADMIN || role == Role.MANAGER && department == d;
    }

    public void requireAny(Role... allowed) {
        if (!List.of(allowed).contains(role)) throw ApiException.forbidden("Requires role " + Arrays.toString(allowed));
    }
}
