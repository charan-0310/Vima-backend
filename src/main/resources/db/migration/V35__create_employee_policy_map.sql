-- BE-00: Employee–Policy Mapping — create table and indexes
-- Many-to-many junction between individuals (employees + dependents) and policies

CREATE TABLE cpc.employee_policy_map (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- WHO is covered
    individual_id UUID NOT NULL,
    primary_employee_id UUID,
    relationship VARCHAR(50) NOT NULL,

    -- WHICH policy
    policy_id BIGINT NOT NULL,
    organization_id UUID NOT NULL,

    -- COVERAGE details
    sum_insured DECIMAL(15, 2),
    coverage_tier VARCHAR(50),

    -- OPT-IN tracking
    is_voluntary BOOLEAN DEFAULT FALSE,

    -- STATUS
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    effective_from DATE NOT NULL,
    effective_to DATE,
    cancellation_reason VARCHAR(255),
    cancelled_at TIMESTAMP WITH TIME ZONE,

    -- SOURCE
    source VARCHAR(30) NOT NULL,
    enrollment_window_id UUID,
    endorsement_id UUID,
    enrollment_submission_id UUID,

    -- TIMESTAMPS
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT fk_epm_individual FOREIGN KEY (individual_id) REFERENCES cpc.customers(individual_id),
    CONSTRAINT fk_epm_primary_employee FOREIGN KEY (primary_employee_id) REFERENCES cpc.customers(individual_id),
    CONSTRAINT fk_epm_policy FOREIGN KEY (policy_id) REFERENCES cpc.policies(policy_id),
    CONSTRAINT fk_epm_organization FOREIGN KEY (organization_id) REFERENCES cpc.organizations(organization_id),
    CONSTRAINT fk_epm_enrollment_window FOREIGN KEY (enrollment_window_id) REFERENCES cpc.enrollment_windows(id),
    CONSTRAINT fk_epm_endorsement FOREIGN KEY (endorsement_id) REFERENCES cpc.endorsements(endorsement_id),
    CONSTRAINT fk_epm_submission FOREIGN KEY (enrollment_submission_id) REFERENCES cpc.enrollment_submissions(id)
);

-- Only one active mapping per individual per policy
CREATE UNIQUE INDEX idx_epm_unique_active
    ON cpc.employee_policy_map(individual_id, policy_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_epm_individual ON cpc.employee_policy_map(individual_id);
CREATE INDEX idx_epm_policy ON cpc.employee_policy_map(policy_id);
CREATE INDEX idx_epm_organization ON cpc.employee_policy_map(organization_id);
CREATE INDEX idx_epm_primary_employee ON cpc.employee_policy_map(primary_employee_id) WHERE primary_employee_id IS NOT NULL;
CREATE INDEX idx_epm_status ON cpc.employee_policy_map(status);
CREATE INDEX idx_epm_enrollment_window ON cpc.employee_policy_map(enrollment_window_id) WHERE enrollment_window_id IS NOT NULL;
CREATE INDEX idx_epm_endorsement ON cpc.employee_policy_map(endorsement_id) WHERE endorsement_id IS NOT NULL;
