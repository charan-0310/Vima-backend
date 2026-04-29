ALTER TABLE admin.insurance_providers
ADD COLUMN IF NOT EXISTS network_hospitals_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS blacklisted_hospitals_url VARCHAR(500);

COMMENT ON COLUMN admin.insurance_providers.network_hospitals_url IS
'URL to insurer network/cashless hospital list';

COMMENT ON COLUMN admin.insurance_providers.blacklisted_hospitals_url IS
'URL to insurer blacklisted/exclusion hospital list';
