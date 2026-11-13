package com.smartdesk.gateway.entity;

public enum Role {
    EMPLOYEE, AGENT, MANAGER, ADMIN;

    /** Agents and managers belong to one department; employees and admins to none. */
    public boolean needsDepartment() {
        return this == AGENT || this == MANAGER;
    }
}
