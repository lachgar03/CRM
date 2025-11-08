CREATE TABLE IF NOT EXISTS roles (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT chk_role_name CHECK (name ~ '^ROLE_[A-Z_]+$')
    );

CREATE INDEX idx_roles_name ON roles(name);

COMMENT ON TABLE roles IS 'Global roles shared across all tenants';
COMMENT ON COLUMN roles.is_system_role IS 'System roles cannot be deleted';
