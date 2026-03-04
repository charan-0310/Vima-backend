-- Add organization_id to audit_events for tenant-scoped filtering and reporting
ALTER TABLE audit.audit_events
    ADD COLUMN organization_id UUID NULL;

CREATE INDEX idx_audit_events_organization_id ON audit.audit_events(organization_id);

COMMENT ON COLUMN audit.audit_events.organization_id IS 'Organization context for the audited operation (from request/entity when available)';
