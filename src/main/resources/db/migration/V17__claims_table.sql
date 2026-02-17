-- Claims Phase 1: claims.claims table with API-ready nullable insurer fields
CREATE TABLE claims.claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_number VARCHAR(100) NOT NULL UNIQUE,

    -- Core references
    organization_id UUID NOT NULL REFERENCES cpc.organizations(organization_id),
    policy_id BIGINT NOT NULL REFERENCES cpc.policies(policy_id),
    employee_id UUID NOT NULL REFERENCES cpc.customers(individual_id),

    -- Member fields
    member_id VARCHAR(100),
    member_type VARCHAR(50),
    member_name VARCHAR(255),
    member_dob DATE,
    member_uhid VARCHAR(100),
    relationship VARCHAR(50),

    -- Claim details
    claim_type VARCHAR(100),
    claim_category VARCHAR(100),
    product_type VARCHAR(100),
    reason_for_admission TEXT,
    diagnosis TEXT,
    claim_amount DECIMAL(18, 2),

    -- Hospital fields
    hospital_name VARCHAR(255),
    hospital_city VARCHAR(100),
    hospital_state VARCHAR(100),
    hospital_pincode VARCHAR(20),
    hospital_provider_code VARCHAR(100),
    is_network_hospital BOOLEAN,

    -- Date fields
    date_of_admission DATE,
    date_of_discharge DATE,
    date_of_submission DATE,

    -- Bank fields
    bank_account_number VARCHAR(50),
    account_holder_name VARCHAR(255),
    ifsc_code VARCHAR(20),
    bank_branch_name VARCHAR(255),

    -- Insurer reference (nullable Phase 1, auto-populated Phase 2)
    insurer_id UUID,
    insurer_claim_ref VARCHAR(100),
    insurer_claim_number VARCHAR(100),
    insurer_inward_number VARCHAR(100),
    insurer_status VARCHAR(100),
    insurer_current_status VARCHAR(100),
    insurer_remarks TEXT,
    rejection_reason TEXT,

    -- Internal status
    internal_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    submission_source VARCHAR(100),

    -- Parent claim
    parent_claim_id UUID REFERENCES claims.claims(id),
    parent_insurer_claim_ref VARCHAR(100),

    -- ABHA
    abha_id VARCHAR(100),

    -- Actor tracking
    submitted_by UUID REFERENCES admin.admin_users(id),
    reviewed_by UUID REFERENCES admin.admin_users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    approved_by UUID REFERENCES admin.admin_users(id),
    approved_at TIMESTAMP WITH TIME ZONE,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE claims.claims IS 'Phase 1 claims table; insurer_* fields nullable until Phase 2';
