-- Phase 3 / VIMA-344: Add grade and CTC to cpc.customers for flex budget allocation (GRADE_BASED, CTC_PERCENTAGE)
-- PRD §4.1.2, §4.1.6

ALTER TABLE cpc.customers ADD COLUMN IF NOT EXISTS grade VARCHAR(50) NULL;
ALTER TABLE cpc.customers ADD COLUMN IF NOT EXISTS ctc DECIMAL(15, 2) NULL;

COMMENT ON COLUMN cpc.customers.grade IS 'Employee grade/band (e.g. L1, L2, L3, L4) for flex budget GRADE_BASED allocation.';
COMMENT ON COLUMN cpc.customers.ctc IS 'Annual Cost to Company (CTC) in rupees; used for flex budget CTC_PERCENTAGE allocation. Null/zero blocks enrollment with message to contact HR.';
