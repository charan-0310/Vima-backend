-- Claims Phase 1: claim_settlement (1:1 with claims)
CREATE TABLE claims.claim_settlement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL UNIQUE REFERENCES claims.claims(id) ON DELETE CASCADE,

    -- Amounts
    claimed_amount DECIMAL(18, 2),
    gross_sanctioned_amount DECIMAL(18, 2),
    net_sanctioned_amount DECIMAL(18, 2),
    total_disallowed_amount DECIMAL(18, 2),
    deduction_amount DECIMAL(18, 2),
    copay_amount DECIMAL(18, 2),
    amount_paid DECIMAL(18, 2),

    -- Payment
    payment_mode VARCHAR(50),
    cheque_number VARCHAR(100),
    cheque_date DATE,
    payment_date DATE,
    payment_reference VARCHAR(255),

    settlement_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE claims.claim_settlement IS '1:1 settlement details for a claim';
