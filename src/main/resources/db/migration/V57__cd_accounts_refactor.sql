-- CD balance v2 refactor:
-- Move from policy-centric CD balance to account-centric (organization + insurer).
-- This migration is additive and does not modify previous migration files.

-- ---------------------------------------------------------------------------
-- 1) CD accounts master table
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cpc.cd_accounts (
    cd_account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES cpc.organizations (organization_id) ON DELETE RESTRICT,
    insurer_name VARCHAR(255) NOT NULL,
    label VARCHAR(100),
    cd_balance NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_cd_accounts_organization_id
    ON cpc.cd_accounts (organization_id);
CREATE INDEX IF NOT EXISTS idx_cd_accounts_insurer_name
    ON cpc.cd_accounts (insurer_name);

-- One default (label IS NULL) account per org + insurer.
CREATE UNIQUE INDEX IF NOT EXISTS uq_cd_account_default
    ON cpc.cd_accounts (organization_id, insurer_name)
    WHERE label IS NULL;

-- Additional labeled accounts per org + insurer + label (future per-policy overrides).
CREATE UNIQUE INDEX IF NOT EXISTS uq_cd_account_labeled
    ON cpc.cd_accounts (organization_id, insurer_name, label)
    WHERE label IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 2) Add policy -> cd_account link
-- ---------------------------------------------------------------------------
ALTER TABLE cpc.policies
    ADD COLUMN IF NOT EXISTS cd_account_id UUID;

ALTER TABLE cpc.policies
    DROP CONSTRAINT IF EXISTS fk_policies_cd_account_id;

ALTER TABLE cpc.policies
    ADD CONSTRAINT fk_policies_cd_account_id
    FOREIGN KEY (cd_account_id) REFERENCES cpc.cd_accounts (cd_account_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_policies_cd_account_id
    ON cpc.policies (cd_account_id);

-- ---------------------------------------------------------------------------
-- 3) Backfill default CD accounts from existing policy data
-- ---------------------------------------------------------------------------
-- Aggregate old per-policy balances into one default account per org + insurer.
INSERT INTO cpc.cd_accounts (organization_id, insurer_name, label, cd_balance, status)
SELECT
    p.organization_id,
    p.insurer_name,
    NULL AS label,
    COALESCE(SUM(COALESCE(p.cd_balance, 0)), 0) AS cd_balance,
    'ACTIVE' AS status
FROM cpc.policies p
WHERE p.organization_id IS NOT NULL
  AND p.insurer_name IS NOT NULL
  AND btrim(p.insurer_name) <> ''
GROUP BY p.organization_id, p.insurer_name
ON CONFLICT (organization_id, insurer_name) WHERE label IS NULL
DO NOTHING;

-- Link each policy to its default account.
UPDATE cpc.policies p
SET cd_account_id = ca.cd_account_id
FROM cpc.cd_accounts ca
WHERE p.cd_account_id IS NULL
  AND p.organization_id = ca.organization_id
  AND p.insurer_name = ca.insurer_name
  AND ca.label IS NULL;

-- ---------------------------------------------------------------------------
-- 4) Refactor ledger table to account-centric FK
-- ---------------------------------------------------------------------------
ALTER TABLE cpc.cd_balance_transactions
    ADD COLUMN IF NOT EXISTS cd_account_id UUID;

-- Backfill transaction.cd_account_id via policy linkage.
UPDATE cpc.cd_balance_transactions tx
SET cd_account_id = p.cd_account_id
FROM cpc.policies p
WHERE tx.cd_account_id IS NULL
  AND tx.policy_id = p.policy_id
  AND p.cd_account_id IS NOT NULL;

-- Fallback for legacy/unmappable rows:
-- create one synthetic default account per org for transactions that could not be linked via policy.
INSERT INTO cpc.cd_accounts (organization_id, insurer_name, label, cd_balance, status)
SELECT DISTINCT
    tx.organization_id,
    '__MIGRATION_UNMAPPED__' AS insurer_name,
    NULL AS label,
    0,
    'ACTIVE'
FROM cpc.cd_balance_transactions tx
WHERE tx.cd_account_id IS NULL
  AND tx.organization_id IS NOT NULL
ON CONFLICT (organization_id, insurer_name) WHERE label IS NULL
DO NOTHING;

UPDATE cpc.cd_balance_transactions tx
SET cd_account_id = ca.cd_account_id
FROM cpc.cd_accounts ca
WHERE tx.cd_account_id IS NULL
  AND tx.organization_id = ca.organization_id
  AND ca.insurer_name = '__MIGRATION_UNMAPPED__'
  AND ca.label IS NULL;

ALTER TABLE cpc.cd_balance_transactions
    DROP CONSTRAINT IF EXISTS fk_cd_balance_transactions_cd_account_id;

ALTER TABLE cpc.cd_balance_transactions
    ADD CONSTRAINT fk_cd_balance_transactions_cd_account_id
    FOREIGN KEY (cd_account_id) REFERENCES cpc.cd_accounts (cd_account_id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_cd_balance_transactions_cd_account_id
    ON cpc.cd_balance_transactions (cd_account_id);

-- policy_id should remain for traceability but no longer mandatory.
ALTER TABLE cpc.cd_balance_transactions
    ALTER COLUMN policy_id DROP NOT NULL;

-- cd_account_id is now the required FK for all ledger rows.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM cpc.cd_balance_transactions
        WHERE cd_account_id IS NULL
    ) THEN
        RAISE EXCEPTION
            'V57 migration failed: cd_balance_transactions has rows with NULL cd_account_id after backfill';
    END IF;
END $$;

ALTER TABLE cpc.cd_balance_transactions
    ALTER COLUMN cd_account_id SET NOT NULL;

COMMENT ON TABLE cpc.cd_accounts IS 'CD account per organization + insurer (default), with optional labeled accounts for future per-policy overrides';
COMMENT ON COLUMN cpc.policies.cd_account_id IS 'Linked CD account for this policy';
COMMENT ON COLUMN cpc.policies.cd_balance IS 'Deprecated for writes in v2; denormalized CD balance is maintained in cpc.cd_accounts.cd_balance';
COMMENT ON COLUMN cpc.cd_balance_transactions.cd_account_id IS 'Primary account-level FK for CD ledger entries';
