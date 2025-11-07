-- V4__insert_default_roles_and_permissions.sql
-- ============================================================

-- Insert System Roles
INSERT INTO roles (name, description, is_system_role) VALUES
                                                          ('ROLE_SUPER_ADMIN', 'Super administrator with cross-tenant access', true),
                                                          ('ROLE_ADMIN', 'Tenant administrator with full access', true),
                                                          ('ROLE_USER', 'Standard user with limited access', true),
                                                          ('ROLE_AGENT', 'Support agent role', true),
                                                          ('ROLE_SALES', 'Sales representative role', true)
    ON CONFLICT (name) DO NOTHING;

-- Insert Permissions
INSERT INTO permissions (name, resource, action, description) VALUES
-- User Management
('USER_CREATE', 'USER', 'CREATE', 'Create new users'),
('USER_READ', 'USER', 'READ', 'View user details'),
('USER_UPDATE', 'USER', 'UPDATE', 'Update user information'),
('USER_DELETE', 'USER', 'DELETE', 'Delete users'),
('USER_MANAGE', 'USER', 'MANAGE', 'Full user management'),

-- Customer Management
('CUSTOMER_CREATE', 'CUSTOMER', 'CREATE', 'Create customers'),
('CUSTOMER_READ', 'CUSTOMER', 'READ', 'View customers'),
('CUSTOMER_UPDATE', 'CUSTOMER', 'UPDATE', 'Update customers'),
('CUSTOMER_DELETE', 'CUSTOMER', 'DELETE', 'Delete customers'),

-- Sales Management
('OPPORTUNITY_CREATE', 'OPPORTUNITY', 'CREATE', 'Create opportunities'),
('OPPORTUNITY_READ', 'OPPORTUNITY', 'READ', 'View opportunities'),
('OPPORTUNITY_UPDATE', 'OPPORTUNITY', 'UPDATE', 'Update opportunities'),
('OPPORTUNITY_DELETE', 'OPPORTUNITY', 'DELETE', 'Delete opportunities'),

-- Ticket Management
('TICKET_CREATE', 'TICKET', 'CREATE', 'Create support tickets'),
('TICKET_READ', 'TICKET', 'READ', 'View tickets'),
('TICKET_UPDATE', 'TICKET', 'UPDATE', 'Update tickets'),
('TICKET_DELETE', 'TICKET', 'DELETE', 'Delete tickets'),

-- Analytics
('ANALYTICS_READ', 'ANALYTICS', 'READ', 'View analytics dashboard'),

-- Tenant Management (Pour Super Admin uniquement)
('TENANT_MANAGE', 'TENANT', 'MANAGE', 'Manage tenant settings'),

-- Role Management (Pour Admin et Super Admin)
('ROLE_READ', 'ROLE', 'READ', 'View roles and permissions'),
('ROLE_MANAGE', 'ROLE', 'MANAGE', 'Create, update, delete roles and assign permissions')
    ON CONFLICT (name) DO NOTHING;

-- Assign Permissions to Roles
DO $$
DECLARE
super_admin_id BIGINT;
    admin_id BIGINT;
    user_id BIGINT;
    agent_id BIGINT;
    sales_id BIGINT;
BEGIN
    -- Get role IDs
SELECT id INTO super_admin_id FROM roles WHERE name = 'ROLE_SUPER_ADMIN';
SELECT id INTO admin_id FROM roles WHERE name = 'ROLE_ADMIN';
SELECT id INTO user_id FROM roles WHERE name = 'ROLE_USER';
SELECT id INTO agent_id FROM roles WHERE name = 'ROLE_AGENT';
SELECT id INTO sales_id FROM roles WHERE name = 'ROLE_SALES';

-- SUPER_ADMIN: All permissions (y compris TENANT_MANAGE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT super_admin_id, id FROM permissions
    ON CONFLICT DO NOTHING;

-- ADMIN: All except cross-tenant (reçoit ROLE_MANAGE mais pas TENANT_MANAGE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT admin_id, id FROM permissions WHERE name != 'TENANT_MANAGE'
ON CONFLICT DO NOTHING;

-- USER: Read-only (sur les ressources principales)
INSERT INTO role_permissions (role_id, permission_id)
SELECT user_id, id FROM permissions
WHERE action = 'READ' AND resource IN ('CUSTOMER', 'OPPORTUNITY', 'TICKET', 'ANALYTICS')
ON CONFLICT DO NOTHING;

-- AGENT: Tickets (full) + Customers (read/update/create)
INSERT INTO role_permissions (role_id, permission_id)
SELECT agent_id, id FROM permissions
WHERE (resource = 'TICKET') OR (resource = 'CUSTOMER' AND action IN ('READ', 'UPDATE', 'CREATE'))
    ON CONFLICT DO NOTHING;

-- SALES: Opportunities (full) + Customers (full)
INSERT INTO role_permissions (role_id, permission_id)
SELECT sales_id, id FROM permissions
WHERE resource IN ('OPPORTUNITY', 'CUSTOMER')
    ON CONFLICT DO NOTHING;
END $$;