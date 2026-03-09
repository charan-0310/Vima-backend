-- BE-01 Phase 2: Company enrollment config with parent/in-law coverage settings (parent_coverage_enabled, in_law_coverage_enabled, max_parents, max_in_laws, parent_age_limit)
CREATE TABLE IF NOT EXISTS cpc.company_enrollment_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    parent_coverage_enabled BOOLEAN DEFAULT FALSE,
    in_law_coverage_enabled BOOLEAN DEFAULT FALSE,
    max_parents INTEGER DEFAULT 0,
    max_in_laws INTEGER DEFAULT 0,
    parent_age_limit INTEGER,
    CONSTRAINT unique_company_enrollment_config_org UNIQUE (organization_id)
);

CREATE INDEX IF NOT EXISTS idx_company_enrollment_config_org ON cpc.company_enrollment_config(organization_id);
