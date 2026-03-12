-- Seed premium rate table rows for organizations that have enrollment windows,
-- so that calculate-premium API returns non-zero values during employee enrollment.
-- Uses FLAT pricing: one rate per member; effective from 2020 so it's always in range.
-- Inserts only when no rate row exists for that org + product_type (idempotent).

-- GMC (Group Mediclaim / Health) - Rs 5000 per member per year
INSERT INTO cpc.premium_rate_tables (
    id, organization_id, product_type, rate, effective_from, pricing_model, gst_inclusive, is_deleted
)
SELECT gen_random_uuid(), ew.organization_id, 'GMC', 5000, '2020-01-01'::date, 'FLAT', false, false
FROM (SELECT DISTINCT organization_id FROM cpc.enrollment_windows) ew
WHERE NOT EXISTS (
    SELECT 1 FROM cpc.premium_rate_tables p
    WHERE p.organization_id = ew.organization_id AND p.product_type = 'GMC' AND p.is_deleted = false
);

-- GPA (Group Personal Accident) - Rs 500 per member per year
INSERT INTO cpc.premium_rate_tables (
    id, organization_id, product_type, rate, effective_from, pricing_model, gst_inclusive, is_deleted
)
SELECT gen_random_uuid(), ew.organization_id, 'GPA', 500, '2020-01-01'::date, 'FLAT', false, false
FROM (SELECT DISTINCT organization_id FROM cpc.enrollment_windows) ew
WHERE NOT EXISTS (
    SELECT 1 FROM cpc.premium_rate_tables p
    WHERE p.organization_id = ew.organization_id AND p.product_type = 'GPA' AND p.is_deleted = false
);

-- GTL (Group Term Life) - Rs 1000 per member per year
INSERT INTO cpc.premium_rate_tables (
    id, organization_id, product_type, rate, effective_from, pricing_model, gst_inclusive, is_deleted
)
SELECT gen_random_uuid(), ew.organization_id, 'GTL', 1000, '2020-01-01'::date, 'FLAT', false, false
FROM (SELECT DISTINCT organization_id FROM cpc.enrollment_windows) ew
WHERE NOT EXISTS (
    SELECT 1 FROM cpc.premium_rate_tables p
    WHERE p.organization_id = ew.organization_id AND p.product_type = 'GTL' AND p.is_deleted = false
);

-- Cost-sharing rules: 70% employer, 30% employee for GMC, GPA, GTL (so premium breakdown shows split)
INSERT INTO cpc.cost_sharing_rules (
    id, company_id, plan_type, coverage_category, employer_share_type, employer_share_value, effective_from, is_deleted
)
SELECT gen_random_uuid(), ew.organization_id, 'GMC', 'ALL_DEPENDENTS', 'PERCENTAGE', 70, '2020-01-01'::date, false
FROM (SELECT DISTINCT organization_id FROM cpc.enrollment_windows) ew
WHERE NOT EXISTS (
    SELECT 1 FROM cpc.cost_sharing_rules c
    WHERE c.company_id = ew.organization_id AND c.plan_type = 'GMC' AND c.coverage_category = 'ALL_DEPENDENTS' AND c.is_deleted = false
);

INSERT INTO cpc.cost_sharing_rules (
    id, company_id, plan_type, coverage_category, employer_share_type, employer_share_value, effective_from, is_deleted
)
SELECT gen_random_uuid(), ew.organization_id, 'GPA', 'ALL_DEPENDENTS', 'PERCENTAGE', 70, '2020-01-01'::date, false
FROM (SELECT DISTINCT organization_id FROM cpc.enrollment_windows) ew
WHERE NOT EXISTS (
    SELECT 1 FROM cpc.cost_sharing_rules c
    WHERE c.company_id = ew.organization_id AND c.plan_type = 'GPA' AND c.coverage_category = 'ALL_DEPENDENTS' AND c.is_deleted = false
);

INSERT INTO cpc.cost_sharing_rules (
    id, company_id, plan_type, coverage_category, employer_share_type, employer_share_value, effective_from, is_deleted
)
SELECT gen_random_uuid(), ew.organization_id, 'GTL', 'ALL_DEPENDENTS', 'PERCENTAGE', 70, '2020-01-01'::date, false
FROM (SELECT DISTINCT organization_id FROM cpc.enrollment_windows) ew
WHERE NOT EXISTS (
    SELECT 1 FROM cpc.cost_sharing_rules c
    WHERE c.company_id = ew.organization_id AND c.plan_type = 'GTL' AND c.coverage_category = 'ALL_DEPENDENTS' AND c.is_deleted = false
);
