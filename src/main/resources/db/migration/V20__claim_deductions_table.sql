-- Claims Phase 1: claim_deductions (1:N with claims), API-ready nullable insurer_sys_id
CREATE TABLE claims.claim_deductions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claims.claims(id) ON DELETE CASCADE,

    insurer_sys_id VARCHAR(100),
    deduction_details TEXT,
    deduction_amount DECIMAL(18, 2),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE claims.claim_deductions IS '1:N deduction line items per claim; insurer_sys_id nullable Phase 1';
