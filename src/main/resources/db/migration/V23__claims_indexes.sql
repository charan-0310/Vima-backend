-- Claims Phase 1: Indexes for query performance
-- claims table
CREATE INDEX IF NOT EXISTS idx_claims_organization_id ON claims.claims(organization_id);
CREATE INDEX IF NOT EXISTS idx_claims_employee_id ON claims.claims(employee_id);
CREATE INDEX IF NOT EXISTS idx_claims_policy_id ON claims.claims(policy_id);
CREATE INDEX IF NOT EXISTS idx_claims_internal_status ON claims.claims(internal_status);
CREATE INDEX IF NOT EXISTS idx_claims_claim_number ON claims.claims(claim_number);
CREATE INDEX IF NOT EXISTS idx_claims_date_of_submission ON claims.claims(date_of_submission);
CREATE INDEX IF NOT EXISTS idx_claims_insurer_claim_ref ON claims.claims(insurer_claim_ref);
CREATE INDEX IF NOT EXISTS idx_claims_insurer_claim_number ON claims.claims(insurer_claim_number);

-- claim_queries
CREATE INDEX IF NOT EXISTS idx_claim_queries_claim_id ON claims.claim_queries(claim_id);
CREATE INDEX IF NOT EXISTS idx_claim_queries_query_status ON claims.claim_queries(query_status);

-- claim_deductions
CREATE INDEX IF NOT EXISTS idx_claim_deductions_claim_id ON claims.claim_deductions(claim_id);

-- claim_audit_log
CREATE INDEX IF NOT EXISTS idx_claim_audit_log_claim_id ON claims.claim_audit_log(claim_id);
CREATE INDEX IF NOT EXISTS idx_claim_audit_log_created_at ON claims.claim_audit_log(created_at);
