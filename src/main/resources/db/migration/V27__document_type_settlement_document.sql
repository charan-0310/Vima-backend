-- Settlement document type for claim settlement uploads (optional doc when recording settlement)
ALTER TYPE document.document_type_enum ADD VALUE IF NOT EXISTS 'SETTLEMENT_DOCUMENT';
