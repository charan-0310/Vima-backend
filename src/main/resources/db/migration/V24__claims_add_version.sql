-- Add version column for optimistic locking on claims.claims
ALTER TABLE claims.claims
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;
