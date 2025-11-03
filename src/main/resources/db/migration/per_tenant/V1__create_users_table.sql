-- V1_create_users_table.sql
-- Creates the main 'users' table for a specific tenant.

CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,

    -- Spring Security UserDetails fields
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
    );

CREATE INDEX idx_users_email ON users(email);
COMMENT ON TABLE users IS 'Tenant-specific user accounts';

-- ============================================================
-- User Roles (Element Collection)
-- ============================================================
-- This table stores the mapping of user IDs to (public) role IDs.
-- It lives in the tenant schema.
-- Note: 'role_id' does NOT have a foreign key constraint to the
-- public.roles table. This is an intentionally loose coupling
-- common in this multi-tenant pattern.

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role_id BIGINT NOT NULL,

                                          PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
COMMENT ON TABLE user_roles IS 'Mapping of tenant users to global role IDs';