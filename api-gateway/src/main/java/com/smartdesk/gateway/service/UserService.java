package com.smartdesk.gateway.service;

import com.smartdesk.gateway.dto.RoleChangeRequest;
import com.smartdesk.gateway.entity.AppUser;
import com.smartdesk.gateway.entity.Department;
import com.smartdesk.gateway.entity.Role;
import com.smartdesk.gateway.exception.ApiException;
import com.smartdesk.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Role hierarchy:
 *  ADMIN   -> any role + department for anyone except themselves.
 *  MANAGER -> turns EMPLOYEEs into AGENTs of their own department, or demotes their own agents.
 * A new role is active from the user's next login (the role lives inside the JWT).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;

    /** Managers see their department plus plain employees (so they can recruit agents). */
    public List<AppUser> list(Jwt caller, Role role, Department department) {
        Department scope = roleOf(caller) == Role.ADMIN || role == Role.EMPLOYEE ? department : departmentOf(caller);
        return users.search(role, scope);
    }

    public AppUser changeRole(Jwt caller, UUID userId, RoleChangeRequest change) {
        if (change.role().needsDepartment() != (change.department() != null))
            throw ApiException.badRequest("AGENT and MANAGER need a department; EMPLOYEE and ADMIN must not have one");
        AppUser target = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (!mayGrant(caller, target, change)) throw ApiException.forbidden("You cannot grant this role");
        target.setRoleName(change.role());
        target.setDepartmentName(change.department());
        return users.save(target);
    }

    private static boolean mayGrant(Jwt caller, AppUser target, RoleChangeRequest change) {
        if (caller.getSubject().equals(target.getUserId().toString())) return false; // nobody edits their own role
        return switch (roleOf(caller)) {
            case ADMIN -> true;
            case MANAGER -> {
                Department own = departmentOf(caller);
                boolean grantAllowed = change.role() == Role.EMPLOYEE || change.role() == Role.AGENT && change.department() == own;
                boolean targetInReach = target.getRoleName() == Role.EMPLOYEE
                        || target.getRoleName() == Role.AGENT && target.getDepartmentName() == own;
                yield grantAllowed && targetInReach;
            }
            default -> false;
        };
    }

    private static Role roleOf(Jwt jwt) {
        return Role.valueOf(jwt.getClaimAsString("role"));
    }

    private static Department departmentOf(Jwt jwt) {
        return Department.valueOf(jwt.getClaimAsString("dept"));
    }
}
