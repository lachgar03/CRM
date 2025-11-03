-- V1__create_tenants_table.sql
-- ============================================================
CREATE TABLE IF NOT EXISTS tenants (
                                       id BIGSERIAL PRIMARY KEY,

    -- Identity
                                       name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(63) NOT NULL UNIQUE,
    schema_name VARCHAR(63) UNIQUE,

    -- Subscription
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
    subscription_status VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    trial_ends_at TIMESTAMP,
    subscription_started_at TIMESTAMP,
    max_users INTEGER DEFAULT 5,
    max_storage_gb INTEGER DEFAULT 1,

    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'PROVISIONING',

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    activated_at TIMESTAMP,
    deactivated_at TIMESTAMP,

    -- Contact
    primary_contact_email VARCHAR(255),
    primary_contact_name VARCHAR(255),
    company_size VARCHAR(50),
    industry VARCHAR(100),

    -- Configuration
    settings JSONB DEFAULT '{}',
    features JSONB DEFAULT '{}',

    -- Constraints
    CONSTRAINT chk_subdomain_format CHECK (
                                              subdomain ~* '^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$'
                                          ),
    CONSTRAINT chk_subscription_plan CHECK (
                                               subscription_plan IN ('FREE', 'BASIC', 'PRO', 'ENTERPRISE')
    ),
    CONSTRAINT chk_status CHECK (
                                    status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED', 'PROVISIONING_FAILED')
    )
    );

CREATE UNIQUE INDEX idx_tenants_subdomain_lower ON tenants(LOWER(subdomain));
CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_created ON tenants(created_at DESC);

COMMENT ON TABLE tenants IS 'Master tenant registry (shared across all tenants)';