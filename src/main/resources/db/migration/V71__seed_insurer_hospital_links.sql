-- Seed insurer network/blacklisted hospital links for existing providers.
-- Updates only the newly added URL columns.
UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.bajajgeneralinsurance.com/branch-locator.html',
    blacklisted_hospitals_url = 'https://www.bajajgeneralinsurance.com/branch-locator.html',
    updated_at = now()
WHERE provider_code = 'BAJAJALL';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.icicilombard.com/cashless-hospitals',
    blacklisted_hospitals_url = 'https://ilhc.icicilombard.com/Customer/GetDelistedHospitalList',
    updated_at = now()
WHERE provider_code = 'ICICI';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.hdfcergo.com/locators/cashless-hospitals-networks',
    blacklisted_hospitals_url = 'https://www.hdfcergo.com/docs/default-source/default-document-library/excluded-hospital.pdf',
    updated_at = now()
WHERE provider_code = 'HDFC';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.acko.com/gi/p/health/networkhospitals',
    blacklisted_hospitals_url = 'https://www.acko.com/gi/p/health/network-hospitals?type=excluded',
    updated_at = now()
WHERE provider_code = 'ACKO';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.careinsurance.com/health-plan-network-hospitals.html?bank=',
    blacklisted_hospitals_url = 'https://cms.careinsurance.com/cms/public/uploads/download_center/Excluded_List.pdf',
    updated_at = now()
WHERE provider_code = 'CARE';
