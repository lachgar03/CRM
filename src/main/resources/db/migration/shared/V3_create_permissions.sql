CREATE TABLE IF NOT EXISTS permissions (
                                           id BIGSERIAL PRIMARY KEY,
                                           name VARCHAR(100) NOT NULL UNIQUE,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_permission_name CHECK (name ~ '^[A-Z_]+$'),
    CONSTRAINT chk_action CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'MANAGE'))
    );

CREATE INDEX idx_permissions_resource ON permissions(resource);
CREATE INDEX idx_permissions_action ON permissions(action);
CREATE UNIQUE INDEX idx_permissions_resource_action ON permissions(resource, action);

COMMENT ON TABLE permissions IS 'Global permissions for RBAC';
        -- Role-Permission mapping

CREATE TABLE IF NOT EXISTS role_permissions (
                                                role_id BIGINT NOT NULL,
                                                permission_id BIGINT NOT NULL,
                                                granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
    );

CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);