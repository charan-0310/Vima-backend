-- Phase 2: Bring product_catalog forward (subset of Phase 3 schema)
CREATE TABLE cpc.product_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,

    product_type VARCHAR(50) NOT NULL,
    -- GMC_BASE, GMC_UPGRADE, GPA, GTL, TOP_UP, SUPER_TOP_UP, PARENT_COVER, PARENT_IN_LAW_COVER, etc.

    name VARCHAR(255) NOT NULL,

    is_mandatory BOOLEAN DEFAULT FALSE,

    pricing_model VARCHAR(30),
    -- AGE_BANDED, FLAT, FAMILY_FLOATER — premium engine and premium_rate_tables key off this

    coverage_options JSONB,
    -- e.g. [{"tier":"basic","sumInsured":300000,"label":"3 Lakh"}, ...]
    -- For top-up: include deductibleAmount, sumInsuredOptions as needed

    covered_relationships JSONB,
    -- e.g. ["SELF","SPOUSE","CHILD1","CHILD2","CHILD3","CHILD4"] for ESC
    -- ["FATHER","MOTHER"] for PARENT_COVER; ["FATHER_IN_LAW","MOTHER_IN_LAW"] for PARENT_IN_LAW_COVER
    -- ["SELF"] for GPA/GTL. Drives which dependents get mapped to this policy.

    display_order INTEGER DEFAULT 0,

    policy_id BIGINT REFERENCES cpc.policies(policy_id),
    -- Each product links to one insurer policy

    grade_filter JSONB,
    -- Optional: ["L3","L4"] = only those grades; null = all grades

    is_active BOOLEAN DEFAULT TRUE,

    effective_from DATE NOT NULL,
    effective_to DATE,
    -- Enables Year 1 vs Year 2 catalog entries to coexist during renewal

    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_product_catalog_org ON cpc.product_catalog(organization_id);
CREATE INDEX idx_product_catalog_type ON cpc.product_catalog(product_type);
CREATE INDEX idx_product_catalog_active ON cpc.product_catalog(organization_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_product_catalog_effective ON cpc.product_catalog(effective_from, effective_to);
