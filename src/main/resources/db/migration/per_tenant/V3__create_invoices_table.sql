-- V3_create_invoices_table.sql
-- Creates the 'invoices' table for a specific tenant.

CREATE TABLE IF NOT EXISTS invoices (
                                        id BIGSERIAL PRIMARY KEY,
                                        customer_id BIGINT NOT NULL,
                                        invoice_number VARCHAR(100) NOT NULL UNIQUE,

    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SENT, PAID, VOID
    amount_due NUMERIC(12, 2) NOT NULL,
    amount_paid NUMERIC(12, 2) DEFAULT 0.00,

    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign key to the tenant's customers table
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'VOID', 'OVERDUE'))
    );

CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
COMMENT ON TABLE invoices IS 'Tenant-specific billing invoices';