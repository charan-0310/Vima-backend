-- BE-01 Phase 2: enrollment_submissions Phase 2 columns and enrollment_plan_selections table

-- enrollment_submissions: add deduction_frequency, total_employee/employer_annual_premium, cost_sharing_snapshot, deduction_amount_per_period, consent_timestamp, consent_text_snapshot
ALTER TABLE cpc.enrollment_submissions ADD COLUMN IF NOT EXISTS deduction_frequency VARCHAR(20);
ALTER TABLE cpc.enrollment_submissions ADD COLUMN IF NOT EXISTS total_employee_annual_premium DECIMAL(15, 2);
ALTER TABLE cpc.enrollment_submissions ADD COLUMN IF NOT EXISTS total_employer_annual_premium DECIMAL(15, 2);
ALTER TABLE cpc.enrollment_submissions ADD COLUMN IF NOT EXISTS cost_sharing_snapshot JSONB DEFAULT '{}';
ALTER TABLE cpc.enrollment_submissions ADD COLUMN IF NOT EXISTS deduction_amount_per_period DECIMAL(15, 2);
ALTER TABLE cpc.enrollment_submissions ADD COLUMN IF NOT EXISTS consent_timestamp TIMESTAMP WITH TIME ZONE;
ALTER TABLE cpc.enrollment_submissions ADD COLUMN IF NOT EXISTS consent_text_snapshot TEXT;

-- enrollment_plan_selections: normalized table with sum_insured, deductible_amount, topup_plan_option_id, is_voluntary
CREATE TABLE IF NOT EXISTS cpc.enrollment_plan_selections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_submission_id UUID NOT NULL REFERENCES cpc.enrollment_submissions(id) ON DELETE CASCADE,
    plan_type VARCHAR(50) NOT NULL,
    opted BOOLEAN NOT NULL DEFAULT TRUE,
    coverage_amount DECIMAL(15, 2),
    premium DECIMAL(15, 2),
    sum_insured DECIMAL(15, 2),
    deductible_amount DECIMAL(15, 2),
    topup_plan_option_id UUID REFERENCES cpc.topup_plan_options(id),
    is_voluntary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_enrollment_plan_selections_submission ON cpc.enrollment_plan_selections(enrollment_submission_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_plan_selections_plan_type ON cpc.enrollment_plan_selections(plan_type);
