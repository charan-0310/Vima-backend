-- Migration to support GMC, GPA, and GTL policy types
-- V11__add_policy_type_support.sql
-- Note: Enum values are added in V11.1 migration to ensure they're committed before use

-- Note: Using existing product_type column for policy types (GMC, GPA, GTL)
-- Note: Using existing coverage_type column for GMC coverage types (E, ES, ESC, ESCP)

-- Step 1: Add new columns to policies table
ALTER TABLE cpc.policies
ADD COLUMN IF NOT EXISTS policy_category VARCHAR(50) DEFAULT 'EMPLOYEE',
ADD COLUMN IF NOT EXISTS sum_insured_multiplier INTEGER CHECK (sum_insured_multiplier >= 1 AND sum_insured_multiplier <= 5),
ADD COLUMN IF NOT EXISTS applies_to_employees BOOLEAN DEFAULT true;

-- Step 2: Make coverage_type nullable (only applies to GMC, not required for GPA/GTL)
ALTER TABLE cpc.policies ALTER COLUMN coverage_type DROP NOT NULL;

-- Step 3: Remove NOT NULL constraint from tpa_organization_name (only required for GMC)
ALTER TABLE cpc.policies ALTER COLUMN tpa_organization_name DROP NOT NULL;

-- Step 4: Migrate existing data: Update product_type to GMC for existing health policies
-- Note: Only runs if you have existing HEALTH/GHI policies
UPDATE cpc.policies
SET product_type = 'GMC'
WHERE product_type IN ('HEALTH', 'GHI');

-- Step 5: Migrate existing coverage_type values to GMC coverage types
-- Note: Only runs if you have existing policies with old coverage values
UPDATE cpc.policies
SET coverage_type = CASE
    WHEN coverage_type = 'INDIVIDUAL' THEN 'E'
    WHEN coverage_type = 'FAMILY_FLOATER' THEN 'ES'
    WHEN coverage_type = 'GROUP' THEN 'ES'
    ELSE coverage_type
END
WHERE product_type = 'GMC' AND coverage_type IN ('INDIVIDUAL', 'FAMILY_FLOATER', 'GROUP');

-- Step 6: Add column comments for documentation
COMMENT ON COLUMN cpc.policies.product_type IS 'Product/Policy type: GMC (Group Medical Coverage), GPA (Group Personal Accident), GTL (Group Term Life), or traditional types (HEALTH, MOTOR, etc.)';
COMMENT ON COLUMN cpc.policies.policy_category IS 'Category of policy: EMPLOYEE, MOTOR, PROPERTY, LIABILITY';
COMMENT ON COLUMN cpc.policies.coverage_type IS 'Coverage type: E (Employee), ES (Employee+Spouse), ESC (Employee+Spouse+Children), ESCP (Employee+Spouse+Children+Parents) for GMC, or INDIVIDUAL/FAMILY_FLOATER/GROUP for traditional policies';
COMMENT ON COLUMN cpc.policies.sum_insured_multiplier IS 'CTC multiplier for GPA/GTL policies (1x to 5x). Only applicable for GPA and GTL policy types.';
COMMENT ON COLUMN cpc.policies.applies_to_employees IS 'Flag indicating if policy applies to employees';
COMMENT ON COLUMN cpc.policies.tpa_organization_name IS 'TPA name (required for GMC only)';
COMMENT ON COLUMN cpc.policies.tpa_contact_info IS 'TPA contact information (required for GMC only)';

-- Step 7: Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_policies_product_type ON cpc.policies(product_type);
CREATE INDEX IF NOT EXISTS idx_policies_policy_category ON cpc.policies(policy_category);
CREATE INDEX IF NOT EXISTS idx_policies_applies_to_employees ON cpc.policies(applies_to_employees);

