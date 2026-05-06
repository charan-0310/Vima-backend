-- Safety migration: some environments missed earlier policy text-column migrations.
-- Idempotent by design so it is safe across all profiles/DBs.
ALTER TABLE cpc.policies
    ADD COLUMN IF NOT EXISTS policy_wording TEXT,
    ADD COLUMN IF NOT EXISTS claim_checklist TEXT;

COMMENT ON COLUMN cpc.policies.policy_wording IS 'Policy wording/details text';
COMMENT ON COLUMN cpc.policies.claim_checklist IS 'Claim checklist text';
