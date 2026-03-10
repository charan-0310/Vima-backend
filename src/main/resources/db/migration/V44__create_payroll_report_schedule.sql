-- BE-05: Payroll report schedule for recurring monthly report generation
CREATE TABLE IF NOT EXISTS cpc.payroll_report_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,
    enrollment_window_id UUID REFERENCES cpc.enrollment_windows(id) ON DELETE SET NULL,
    next_run_date DATE NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    recipient_emails TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payroll_report_schedule_org ON cpc.payroll_report_schedule(organization_id);
CREATE INDEX IF NOT EXISTS idx_payroll_report_schedule_active ON cpc.payroll_report_schedule(is_active) WHERE is_active = TRUE;

COMMENT ON TABLE cpc.payroll_report_schedule IS 'Scheduled payroll deduction report generation; job runs monthly and emails report to recipient_emails';
