-- V79: re-introduce text fields with new, semantically explicit names.
-- These drive the employee wording / claim-checklist screens; the FK columns
-- added in V78 (policy_wording_document_id, claim_checklist_document_id)
-- remain as admin-only PDF storage and are no longer surfaced to employees.
ALTER TABLE cpc.policies
    ADD COLUMN IF NOT EXISTS policy_wording_summary TEXT,
    ADD COLUMN IF NOT EXISTS claim_checklist_additional_docs TEXT;

COMMENT ON COLUMN cpc.policies.policy_wording_summary
    IS 'Admin-authored condensed policy wording (HTML from rich text editor). Shown to employees in place of the wording PDF.';
COMMENT ON COLUMN cpc.policies.claim_checklist_additional_docs
    IS 'Admin-authored extra-documents notes (HTML). Appended to the default 5-section claim checklist on the employee portal.';
