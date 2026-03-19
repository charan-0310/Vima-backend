-- Phase 2: Add product type enum values and policy columns for PARENT_GMC and TOP_UP/SUPER_TOP_UP
-- Enum values must be added before columns that use them

-- Add TOP_UP, SUPER_TOP_UP, PARENT_GMC to product_type enum (if not present)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'product_type_enum' AND e.enumlabel = 'PARENT_GMC') THEN
        ALTER TYPE cpc.product_type_enum ADD VALUE 'PARENT_GMC';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'product_type_enum' AND e.enumlabel = 'TOP_UP') THEN
        ALTER TYPE cpc.product_type_enum ADD VALUE 'TOP_UP';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'product_type_enum' AND e.enumlabel = 'SUPER_TOP_UP') THEN
        ALTER TYPE cpc.product_type_enum ADD VALUE 'SUPER_TOP_UP';
    END IF;
END $$;

-- Add PARENT to coverage_type enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_enum e JOIN pg_type t ON e.enumtypid = t.oid WHERE t.typname = 'coverage_type_enum' AND e.enumlabel = 'PARENT') THEN
        ALTER TYPE cpc.coverage_type_enum ADD VALUE 'PARENT';
    END IF;
END $$;

-- Parent/In-Law fields (PARENT_GMC)
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS parent_coverage_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS in_law_coverage_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS max_parents INTEGER DEFAULT 0;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS max_in_laws INTEGER DEFAULT 0;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS parent_age_limit INTEGER;

-- Top-up / Super top-up fields
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS insurer_name VARCHAR(255);
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS deductible_amount NUMERIC(15, 2);
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS sum_insured_options JSONB;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS covers_dependents BOOLEAN DEFAULT FALSE;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS covers_parents BOOLEAN DEFAULT FALSE;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS effective_from DATE;
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS effective_to DATE;

COMMENT ON COLUMN cpc.policies.parent_coverage_enabled IS 'PARENT_GMC: Enable parent coverage';
COMMENT ON COLUMN cpc.policies.in_law_coverage_enabled IS 'PARENT_GMC: Enable in-law coverage';
COMMENT ON COLUMN cpc.policies.max_parents IS 'PARENT_GMC: Max number of parents';
COMMENT ON COLUMN cpc.policies.max_in_laws IS 'PARENT_GMC: Max number of in-laws';
COMMENT ON COLUMN cpc.policies.parent_age_limit IS 'PARENT_GMC: Max age for parent/in-law (optional)';
COMMENT ON COLUMN cpc.policies.deductible_amount IS 'TOP_UP/SUPER_TOP_UP: Deductible amount';
COMMENT ON COLUMN cpc.policies.sum_insured_options IS 'TOP_UP/SUPER_TOP_UP: Sum insured options (JSON array)';
COMMENT ON COLUMN cpc.policies.effective_from IS 'TOP_UP/SUPER_TOP_UP: Effective from date';
COMMENT ON COLUMN cpc.policies.effective_to IS 'TOP_UP/SUPER_TOP_UP: Effective to date';
