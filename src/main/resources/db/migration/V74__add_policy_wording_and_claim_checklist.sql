-- Add optional policy wording/checklist text fields per policy.
ALTER TABLE cpc.policies
    ADD COLUMN IF NOT EXISTS policy_wording TEXT,
    ADD COLUMN IF NOT EXISTS claim_checklist TEXT;
