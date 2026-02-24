-- Add employee response fields to claim_queries (employee adds remarks/docs for admin to forward)
ALTER TABLE claims.claim_queries
    ADD COLUMN IF NOT EXISTS employee_remarks TEXT,
    ADD COLUMN IF NOT EXISTS employee_response_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN claims.claim_queries.employee_remarks IS 'Remarks/documents description added by employee for query response';
COMMENT ON COLUMN claims.claim_queries.employee_response_at IS 'When employee submitted response (remarks/docs)';
