-- Index-aligned annual premiums per sum-insured tier for TOP_UP / SUPER_TOP_UP (same array length/order as sum_insured_options)
ALTER TABLE cpc.policies ADD COLUMN IF NOT EXISTS topup_premium_options jsonb;

COMMENT ON COLUMN cpc.policies.topup_premium_options IS 'JSON array of numbers: annual premium per sum_insured_options[i]';
