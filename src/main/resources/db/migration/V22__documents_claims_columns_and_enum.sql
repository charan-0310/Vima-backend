-- Add insurer sync columns to document.documents (no data loss)
ALTER TABLE document.documents
    ADD COLUMN IF NOT EXISTS synced_to_insurer BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE document.documents
    ADD COLUMN IF NOT EXISTS insurer_doc_ref VARCHAR(255);

ALTER TABLE document.documents
    ADD COLUMN IF NOT EXISTS synced_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN document.documents.synced_to_insurer IS 'Whether document has been synced to insurer (Phase 2)';
COMMENT ON COLUMN document.documents.insurer_doc_ref IS 'Insurer reference for synced document';
COMMENT ON COLUMN document.documents.synced_at IS 'When document was synced to insurer';

-- Add new DocumentType enum values for claims
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'CLAIM_LETTER_APPROVAL';
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'CLAIM_LETTER_REJECTION';
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'CLAIM_LETTER_QUERY';
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'CLAIM_LETTER_PAID';
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'QUERY_RESPONSE_DOC';
