\connect smartdesk_auth_db

-- =====================================================================
-- AUTH DATABASE  (owner: api-gateway)
-- =====================================================================

-- Users and their place in the hierarchy: ADMIN > MANAGER (one dept) > AGENT (one dept) > EMPLOYEE
CREATE TABLE users (
    user_id          UUID         PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    role_name        VARCHAR(20)  NOT NULL,
    department_name  VARCHAR(20),                                 -- only for AGENT / MANAGER
    account_status   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ,
    CONSTRAINT uk_users_email   UNIQUE (email),
    CONSTRAINT ck_users_role    CHECK (role_name IN ('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN')),
    CONSTRAINT ck_users_dept    CHECK (department_name IN ('IT', 'HR', 'OPERATIONS')),
    CONSTRAINT ck_users_dept_by_role CHECK (
        (role_name IN ('AGENT', 'MANAGER')) = (department_name IS NOT NULL))
);
CREATE INDEX idx_users_role_dept ON users (role_name, department_name);   -- "list IT agents"

-- One-time login codes (step 2 of login)
CREATE TABLE otps (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    code_hash   VARCHAR(255) NOT NULL,                            -- BCrypt of the 6-digit code
    expires_at  TIMESTAMPTZ  NOT NULL,                            -- 5 minutes
    attempts    INTEGER      NOT NULL DEFAULT 0,                  -- locked after 5 wrong tries
    used        BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_otps_user ON otps (user_id);
