-- BE-01 Phase 2: Top-up and super top-up plan options per company
CREATE TABLE cpc.topup_plan_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,
    policy_id BIGINT REFERENCES cpc.policies(policy_id),
    plan_type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    insurer_name VARCHAR(255),
    deductible_amount DECIMAL(12, 2) NOT NULL,
    sum_insured_options JSONB NOT NULL,
    pricing_model VARCHAR(20) NOT NULL,
    covers_dependents BOOLEAN DEFAULT TRUE,
    covers_parents BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT valid_topup_type CHECK (plan_type IN ('TOP_UP', 'SUPER_TOP_UP')),
    CONSTRAINT valid_topup_pricing CHECK (pricing_model IN ('AGE_BANDED', 'FLAT'))
);

CREATE INDEX idx_topup_options_company ON cpc.topup_plan_options(company_id);
CREATE INDEX idx_topup_options_active ON cpc.topup_plan_options(is_active) WHERE is_active = TRUE;
