-- Claims Phase 1: claim_queries (1:N with claims), API-ready nullable insurer_sys_id
CREATE TABLE claims.claim_queries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claims.claims(id) ON DELETE CASCADE,

    insurer_sys_id VARCHAR(100),

    -- Query
    query_text TEXT,
    query_date DATE,
    query_status VARCHAR(50) NOT NULL DEFAULT 'OPEN',

    -- Response
    response_text TEXT,
    response_date DATE,
    responded_by UUID REFERENCES admin.admin_users(id),
    response_remark TEXT,
    courier_name VARCHAR(255),
    pod_number VARCHAR(100),
    num_documents_attached INTEGER,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE claims.claim_queries IS '1:N queries and responses per claim; insurer_sys_id nullable Phase 1';
