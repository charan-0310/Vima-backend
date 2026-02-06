-- Add SELF_ENROLLMENT to document type enum for enrollment window document uploads
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'SELF_ENROLLMENT';
