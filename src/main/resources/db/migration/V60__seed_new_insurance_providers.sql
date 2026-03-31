-- Seed additional insurance providers needed for product-specific policy flows.
-- - ACKO and GENERALI for GENERAL
-- - MANIPAL for HEALTH
INSERT INTO admin.insurance_providers (
    provider_name,
    provider_code,
    product_type,
    is_active
)
VALUES
    ('Acko', 'ACKO', 'GENERAL', true),
    ('Generali Central', 'GENERALI', 'GENERAL', true),
    ('Manipal Cigna', 'MANIPAL', 'HEALTH', true)
ON CONFLICT (provider_code) DO UPDATE
SET
    provider_name = EXCLUDED.provider_name,
    product_type = EXCLUDED.product_type,
    is_active = true,
    updated_at = now();
