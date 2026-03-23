-- CD Balance tracking: ledger, policy denormalized balance, proof documents, feature flag
-- Ref: PRD / cd-balance-workflow data model

-- ---------------------------------------------------------------------------
-- 1. PostgreSQL enum types (cpc)
-- ---------------------------------------------------------------------------
DO $$ BEGIN CREATE TYPE cpc.cd_transaction_type_enum AS ENUM (
    'INITIAL_DEPOSIT',
    'ENDORSEMENT_DEBIT',
    'ENDORSEMENT_CREDIT',
    'ADJUSTMENT',
    'TOP_UP',
    'SETTLEMENT'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN CREATE TYPE cpc.cd_transaction_source_enum AS ENUM (
    'MANUAL',
    'ENDORSEMENT_APPROVAL',
    'API_SYNC'
); EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ---------------------------------------------------------------------------
-- 2. Denormalized balance on policy
-- ---------------------------------------------------------------------------
ALTER TABLE cpc.policies
    ADD COLUMN IF NOT EXISTS cd_balance NUMERIC(15, 2) NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------------
-- 3. CD balance transaction ledger
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cpc.cd_balance_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id BIGINT NOT NULL REFERENCES cpc.policies (policy_id) ON DELETE RESTRICT,
    organization_id UUID NOT NULL REFERENCES cpc.organizations (organization_id) ON DELETE RESTRICT,
    endorsement_id UUID REFERENCES cpc.endorsements (endorsement_id) ON DELETE SET NULL,
    transaction_type cpc.cd_transaction_type_enum NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    running_balance NUMERIC(15, 2) NOT NULL,
    description VARCHAR(500),
    notes TEXT,
    reference_number VARCHAR(100),
    source cpc.cd_transaction_source_enum NOT NULL,
    performed_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_cd_balance_transactions_policy_id
    ON cpc.cd_balance_transactions (policy_id);
CREATE INDEX IF NOT EXISTS idx_cd_balance_transactions_organization_id
    ON cpc.cd_balance_transactions (organization_id);
CREATE INDEX IF NOT EXISTS idx_cd_balance_transactions_endorsement_id
    ON cpc.cd_balance_transactions (endorsement_id);
CREATE INDEX IF NOT EXISTS idx_cd_balance_transactions_created_at
    ON cpc.cd_balance_transactions (created_at);
CREATE INDEX IF NOT EXISTS idx_cd_balance_transactions_transaction_type
    ON cpc.cd_balance_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_cd_balance_transactions_source
    ON cpc.cd_balance_transactions (source);

COMMENT ON TABLE cpc.cd_balance_transactions IS 'CD (credit/debit) ledger per policy; running_balance and policy.cd_balance updated in application transactions';

-- ---------------------------------------------------------------------------
-- 4. Proof documents (up to 3 per transaction)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cpc.cd_transaction_documents (
    transaction_id UUID NOT NULL REFERENCES cpc.cd_balance_transactions (transaction_id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES document.documents (document_id) ON DELETE RESTRICT,
    PRIMARY KEY (transaction_id, document_id)
);

CREATE INDEX IF NOT EXISTS idx_cd_transaction_documents_document_id
    ON cpc.cd_transaction_documents (document_id);

CREATE OR REPLACE FUNCTION cpc.enforce_cd_transaction_document_limit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF (
        SELECT COUNT(*)::INT
        FROM cpc.cd_transaction_documents
        WHERE transaction_id = NEW.transaction_id
    ) >= 3 THEN
        RAISE EXCEPTION 'cd_transaction_documents: at most 3 proof documents per transaction';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_cd_transaction_documents_limit ON cpc.cd_transaction_documents;
CREATE TRIGGER trg_cd_transaction_documents_limit
    BEFORE INSERT ON cpc.cd_transaction_documents
    FOR EACH ROW
    EXECUTE FUNCTION cpc.enforce_cd_transaction_document_limit();

-- ---------------------------------------------------------------------------
-- 5. Feature flag: group-insurance.cd-balance (child of group-insurance)
-- ---------------------------------------------------------------------------
INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
VALUES (
    'c101d201-e302-f403-a504-b605c706d807',
    'group-insurance.cd-balance',
    'CD (credit/debit) balance tracking per policy — ledger, endorsement approval, proof documents',
    '11112222-3333-4444-5555-666677778888',
    FALSE
)
ON CONFLICT (flag_id) DO NOTHING;

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'c101d201-e302-f403-a504-b605c706d807', 'ROLE_VIMA_ADMIN', TRUE,
       ARRAY['READ', 'WRITE', 'APPROVE']::permission_action[]
WHERE NOT EXISTS (
    SELECT 1 FROM admin.feature_flag_roles
    WHERE flag_id = 'c101d201-e302-f403-a504-b605c706d807' AND role_name = 'ROLE_VIMA_ADMIN'
);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'c101d201-e302-f403-a504-b605c706d807', 'ROLE_SUPER_ADMIN', TRUE,
       ARRAY['READ', 'WRITE', 'APPROVE']::permission_action[]
WHERE NOT EXISTS (
    SELECT 1 FROM admin.feature_flag_roles
    WHERE flag_id = 'c101d201-e302-f403-a504-b605c706d807' AND role_name = 'ROLE_SUPER_ADMIN'
);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'c101d201-e302-f403-a504-b605c706d807', 'ROLE_HR_ADMIN', FALSE,
       ARRAY['READ']::permission_action[]
WHERE NOT EXISTS (
    SELECT 1 FROM admin.feature_flag_roles
    WHERE flag_id = 'c101d201-e302-f403-a504-b605c706d807' AND role_name = 'ROLE_HR_ADMIN'
);
