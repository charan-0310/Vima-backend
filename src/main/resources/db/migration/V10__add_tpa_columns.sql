-- Add TPA (Third Party Administrator) columns to policies table
ALTER TABLE cpc.policies
ADD COLUMN IF NOT EXISTS tpa_organization_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS tpa_contact_info VARCHAR(255);

-- Add comments to document the columns
COMMENT ON COLUMN cpc.policies.tpa_organization_name IS 'Third Party Administrator organization name (required)';
COMMENT ON COLUMN cpc.policies.tpa_contact_info IS 'TPA contact information - phone, email, or any contact details (optional)';
