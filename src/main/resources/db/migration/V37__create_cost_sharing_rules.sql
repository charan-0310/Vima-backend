-- BE-01 Phase 2: Cost-sharing rules per company and plan type
CREATE TABLE cpc.cost_sharing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,
    plan_type VARCHAR(50) NOT NULL,
    coverage_category VARCHAR(50) NOT NULL,
    employer_share_type VARCHAR(20) NOT NULL,
    employer_share_value DECIMAL(12, 2) NOT NULL,
    excess_allowed BOOLEAN DEFAULT FALSE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_cost_sharing_rule UNIQUE (company_id, plan_type, coverage_category, effective_from),
    CONSTRAINT valid_employer_share_type CHECK (employer_share_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT valid_percentage CHECK (employer_share_type != 'PERCENTAGE' OR (employer_share_value >= 0 AND employer_share_value <= 100))
);

CREATE INDEX idx_cost_sharing_company ON cpc.cost_sharing_rules(company_id);
CREATE INDEX idx_cost_sharing_plan ON cpc.cost_sharing_rules(plan_type);
CREATE INDEX idx_cost_sharing_effective ON cpc.cost_sharing_rules(effective_from, effective_to);
