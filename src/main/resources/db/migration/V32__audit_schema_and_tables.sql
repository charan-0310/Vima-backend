-- AOP + Async audit: single audit_events table + audit_pending for failure handling
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schema_name VARCHAR(63),
    table_name VARCHAR(100),
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    old_snapshot JSONB,
    new_snapshot JSONB,
    user_id UUID,
    user_email VARCHAR(255),
    user_role VARCHAR(100),
    correlation_id VARCHAR(100),
    ip_address VARCHAR(45),
    action_source VARCHAR(20) DEFAULT 'WEB',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_events_entity ON audit.audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_events_schema_table ON audit.audit_events(schema_name, table_name);
CREATE INDEX idx_audit_events_created_at ON audit.audit_events(created_at);
CREATE INDEX idx_audit_events_user_id ON audit.audit_events(user_id);
CREATE INDEX idx_audit_events_correlation_id ON audit.audit_events(correlation_id);

COMMENT ON TABLE audit.audit_events IS 'AOP+Async audit trail; one row per auditable operation';

-- Dead-letter / pending table for failed async writes (retry by job or manual replay)
CREATE TABLE audit.audit_pending (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_pending_status ON audit.audit_pending(status);
CREATE INDEX idx_audit_pending_created_at ON audit.audit_pending(created_at);

COMMENT ON TABLE audit.audit_pending IS 'Failed audit writes for retry/replay; payload matches audit_events shape';
