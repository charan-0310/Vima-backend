-- Add insurance provider / policy display fields to claims (submit payload)
ALTER TABLE claims.claims
    ADD COLUMN IF NOT EXISTS insurance_provider_logo VARCHAR(200),
    ADD COLUMN IF NOT EXISTS policy_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS valid_until DATE;

COMMENT ON COLUMN claims.claims.insurance_provider_logo IS 'Display name for insurer (e.g. Star Health)';
COMMENT ON COLUMN claims.claims.policy_number IS 'Policy number (e.g. GMC-2026-CHN-00123)';
COMMENT ON COLUMN claims.claims.valid_until IS 'Policy validity end date';
