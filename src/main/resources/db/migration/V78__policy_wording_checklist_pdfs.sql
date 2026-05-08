-- V78: Replace rich-text policy_wording / claim_checklist columns with FK references
-- to document.documents rows holding the uploaded PDFs.
--
-- Existing TEXT content is intentionally dropped — admins will re-upload the new PDFs.
-- The TEXT fields are reintroduced (with new, semantically explicit names) in V79
-- to drive the employee-facing wording / claim-checklist screens, while the FK
-- columns added here remain admin-only PDF storage.

-- 1. Extend the document_type enum with the two new specific document kinds.
--    These cannot be referenced inside the same transaction in older PG versions, but
--    the rest of this migration only references columns/types, not values.
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'POLICY_WORDING';
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'CLAIM_CHECKLIST';

-- 2. Add nullable FK columns on cpc.policies pointing to the uploaded PDF documents.
ALTER TABLE cpc.policies
    ADD COLUMN IF NOT EXISTS policy_wording_document_id uuid,
    ADD COLUMN IF NOT EXISTS claim_checklist_document_id uuid;

DO $$ BEGIN
    ALTER TABLE cpc.policies
        ADD CONSTRAINT policies_policy_wording_document_id_fkey
        FOREIGN KEY (policy_wording_document_id)
        REFERENCES document.documents(document_id)
        ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE cpc.policies
        ADD CONSTRAINT policies_claim_checklist_document_id_fkey
        FOREIGN KEY (claim_checklist_document_id)
        REFERENCES document.documents(document_id)
        ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 3. Drop the legacy rich-text columns. Admins must re-upload PDFs for existing
--    policies that previously had wording/checklist HTML content.
ALTER TABLE cpc.policies
    DROP COLUMN IF EXISTS policy_wording,
    DROP COLUMN IF EXISTS claim_checklist;
