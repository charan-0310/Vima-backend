-- Stable per-employee identifier for wellness partner JWTs (e.g. MantraCare user_identifier claim)

ALTER TABLE cpc.customers
    ADD COLUMN IF NOT EXISTS wellness_user_id VARCHAR(36);

CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_wellness_user_id
    ON cpc.customers (wellness_user_id)
    WHERE wellness_user_id IS NOT NULL;

COMMENT ON COLUMN cpc.customers.wellness_user_id IS
    'Stable random UUID (varchar) used as user_identifier claim for wellness partners (e.g. MantraCare). Lazy-populated on first partner click.';
