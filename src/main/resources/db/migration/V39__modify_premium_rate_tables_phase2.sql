-- BE-01 Phase 2: Premium rate tables with Phase 2 columns (pricing_model, sum_insured_amount, family_size_min/max, rate_source, gst_inclusive, gst_percentage, is_deleted)
CREATE TABLE IF NOT EXISTS cpc.premium_rate_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,
    policy_id BIGINT REFERENCES cpc.policies(policy_id),
    product_type VARCHAR(50) NOT NULL,
    member_type VARCHAR(50),
    age_band_min INTEGER,
    age_band_max INTEGER,
    rate DECIMAL(12, 2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    pricing_model VARCHAR(30),
    sum_insured_amount DECIMAL(15, 2),
    family_size_min INTEGER,
    family_size_max INTEGER,
    rate_source VARCHAR(50),
    gst_inclusive BOOLEAN DEFAULT FALSE,
    gst_percentage DECIMAL(5, 2),
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_premium_rate_org ON cpc.premium_rate_tables(organization_id);
CREATE INDEX IF NOT EXISTS idx_premium_rate_policy ON cpc.premium_rate_tables(policy_id);
CREATE INDEX IF NOT EXISTS idx_premium_rate_effective ON cpc.premium_rate_tables(effective_from, effective_to);
