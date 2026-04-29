-- Seed additional insurer network/blacklisted hospital links for existing providers.
-- Updates only URL columns (and updated_at), leaves all other insurer fields unchanged.

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.godigit.com/health-insurance/digit-cashless-network-hospitals-list',
    blacklisted_hospitals_url = 'https://www.godigit.com/health-insurance/excluded-hospitals',
    updated_at = now()
WHERE provider_code = 'GODIGIT';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.nivabupa.com/healthinsurance/locate-care/hospital-listing',
    blacklisted_hospitals_url = 'https://rules.nivabupa.com/static_doc/unlisted/Excluded_List_Revised_10092025.pdf',
    updated_at = now()
WHERE provider_code = 'NIVABUPA';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.starhealth.in/network-hospitals',
    blacklisted_hospitals_url = 'https://www.starhealth.in/lookup/hospital/?Search=',
    updated_at = now()
WHERE provider_code = 'STAR';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.tataaig.com/locator/cashless-network-hospitals',
    blacklisted_hospitals_url = 'https://www.tataaig.com/excluded-hospital-list',
    updated_at = now()
WHERE provider_code = 'TATAAIG';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.royalsundaram.in/cashless-hospital',
    blacklisted_hospitals_url = 'https://www.royalsundaram.in/assets/List-of-Excluded-Hospitals.pdf',
    updated_at = now()
WHERE provider_code = 'ROYAL';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.adityabirlahealth.com/healthinsurance/locate-care/hospital-listing',
    blacklisted_hospitals_url = 'http://www.adityabirlacapital.com/healthinsurance/locate-care/hospital-listing',
    updated_at = now()
WHERE provider_code = 'ADITYABIRLA';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://csc.orientalinsurance.org.in/network-hospitals',
    blacklisted_hospitals_url = NULL,
    updated_at = now()
WHERE provider_code = 'ORIENTAL';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://kwa.kerala.gov.in/wp-content/uploads/2024/07/Employees-LIST-OF-NETWORKS-HOSPITALS_PAN-INDIA-_Ms-United-India-Insurance-Company.pdf',
    blacklisted_hospitals_url = NULL,
    updated_at = now()
WHERE provider_code = 'UNITEDIND';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://newindia.co.in/hospitals-list',
    blacklisted_hospitals_url = NULL,
    updated_at = now()
WHERE provider_code = 'NEWIND';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://nationalinsurance.nic.co.in/en/health-insurance',
    blacklisted_hospitals_url = NULL,
    updated_at = now()
WHERE provider_code = 'NATIONAL';

UPDATE admin.insurance_providers
SET
    network_hospitals_url = 'https://www.cholainsurance.com/cashless-hospitals',
    blacklisted_hospitals_url = 'https://safewaytpa.in/Documents/CholaMandalam_Excluded_List.pdf',
    updated_at = now()
WHERE provider_code = 'CHOLAMS';
