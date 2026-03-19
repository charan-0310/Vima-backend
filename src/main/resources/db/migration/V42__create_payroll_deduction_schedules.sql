-- BE-01 Phase 2: Payroll deduction schedules for per-employee deduction breakdown and reporting
CREATE TABLE cpc.payroll_deduction_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES cpc.customers(individual_id) ON DELETE CASCADE,
    enrollment_submission_id UUID REFERENCES cpc.enrollment_submissions(id) ON DELETE SET NULL,
    product_type VARCHAR(50) NOT NULL,
    plan_type VARCHAR(50),
    coverage_amount DECIMAL(15, 2),
    total_premium_annual DECIMAL(15, 2) NOT NULL,
    employer_share_annual DECIMAL(15, 2) NOT NULL,
    employee_share_annual DECIMAL(15, 2) NOT NULL,
    deduction_frequency VARCHAR(20) NOT NULL,
    deduction_amount_per_period DECIMAL(15, 2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_payroll_deduction_org ON cpc.payroll_deduction_schedules(organization_id);
CREATE INDEX idx_payroll_deduction_employee ON cpc.payroll_deduction_schedules(employee_id);
CREATE INDEX idx_payroll_deduction_submission ON cpc.payroll_deduction_schedules(enrollment_submission_id);
CREATE INDEX idx_payroll_deduction_effective ON cpc.payroll_deduction_schedules(effective_from, effective_to);
