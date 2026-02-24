-- Claims Phase 1: claim_audit_log (1:N with claims), API-ready nullable fields
CREATE TABLE claims.claim_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claims.claims(id) ON DELETE CASCADE,

    action VARCHAR(100) NOT NULL,
    old_status VARCHAR(100),
    new_status VARCHAR(100),
    actor_id UUID REFERENCES admin.admin_users(id),
    actor_role VARCHAR(100),
    details JSONB,

    -- API-ready (nullable)
    correlation_id VARCHAR(100),
    api_endpoint VARCHAR(500),
    api_response_status INTEGER,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE claims.claim_audit_log IS '1:N audit trail per claim; correlation_id, api_endpoint, api_response_status for API tracing';
