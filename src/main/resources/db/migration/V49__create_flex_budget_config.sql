-- Phase 3: Flex budget configuration and grade amounts (PRD §4.1.2)
-- Schema cpc; company_id = organization_id (cpc.organizations)

CREATE TABLE cpc.flex_budget_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES cpc.organizations(organization_id) ON DELETE CASCADE,

    allocation_method VARCHAR(30) NOT NULL,
    -- Values: GRADE_BASED, CTC_PERCENTAGE, FLAT_AMOUNT

    flat_amount DECIMAL(12, 2),
    ctc_percentage DECIMAL(5, 2),

    excess_policy VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE_PAYS',
    -- Values: EMPLOYEE_PAYS, CAP_AT_BUDGET, NOT_ALLOWED

    unused_budget_policy VARCHAR(30) NOT NULL DEFAULT 'FORFEIT',
    -- Values: FORFEIT (v1 only)

    effective_from DATE NOT NULL,
    effective_to DATE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT unique_flex_config_company_effective UNIQUE (company_id, effective_from),
    CONSTRAINT valid_allocation_method CHECK (allocation_method IN ('GRADE_BASED', 'CTC_PERCENTAGE', 'FLAT_AMOUNT')),
    CONSTRAINT valid_excess_policy CHECK (excess_policy IN ('EMPLOYEE_PAYS', 'CAP_AT_BUDGET', 'NOT_ALLOWED')),
    CONSTRAINT valid_unused_policy CHECK (unused_budget_policy IN ('FORFEIT'))
);

CREATE TABLE cpc.flex_budget_grade_amounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flex_budget_config_id UUID NOT NULL REFERENCES cpc.flex_budget_config(id) ON DELETE CASCADE,
    grade VARCHAR(50) NOT NULL,
    budget_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT unique_grade_per_config UNIQUE (flex_budget_config_id, grade)
);

CREATE INDEX idx_flex_budget_config_company ON cpc.flex_budget_config(company_id);
CREATE INDEX idx_flex_budget_config_effective ON cpc.flex_budget_config(effective_from, effective_to);
CREATE INDEX idx_flex_budget_grade_config ON cpc.flex_budget_grade_amounts(flex_budget_config_id);
