-- BE-00: Backfill employee_policy_map from existing customers and policies
-- Uses WHERE NOT EXISTS because partial unique index does not support ON CONFLICT

-- Backfill: Map all active employees (SELF) to all active org policies (applies_to_employees = true)
INSERT INTO cpc.employee_policy_map (
    individual_id, primary_employee_id, relationship, policy_id,
    organization_id, sum_insured, is_voluntary, status,
    effective_from, source, created_at, updated_at
)
SELECT
    c.individual_id,
    NULL,
    'SELF',
    p.policy_id,
    c.organization_id,
    p.sum_insured,
    FALSE,
    'ACTIVE',
    GREATEST(p.start_date, COALESCE(c.date_of_joining, c.created_at::date)),
    'BACKFILL',
    NOW(),
    NOW()
FROM cpc.customers c
JOIN cpc.policies p ON p.organization_id = c.organization_id
WHERE c.relationship = 'SELF'
  AND c.status != 'INACTIVE'
  AND p.applies_to_employees = TRUE
  AND p.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM cpc.employee_policy_map epm2
    WHERE epm2.individual_id = c.individual_id
      AND epm2.policy_id = p.policy_id
      AND epm2.status = 'ACTIVE'
  );

-- Backfill: Map all active dependents to the same policies as their primary employee
INSERT INTO cpc.employee_policy_map (
    individual_id, primary_employee_id, relationship, policy_id,
    organization_id, sum_insured, is_voluntary, status,
    effective_from, source, created_at, updated_at
)
SELECT
    d.individual_id,
    d.primary_individual_id,
    d.relationship,
    epm.policy_id,
    d.organization_id,
    epm.sum_insured,
    FALSE,
    'ACTIVE',
    epm.effective_from,
    'BACKFILL',
    NOW(),
    NOW()
FROM cpc.customers d
JOIN cpc.employee_policy_map epm ON epm.individual_id = d.primary_individual_id AND epm.status = 'ACTIVE'
WHERE d.primary_individual_id IS NOT NULL
  AND d.status != 'INACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM cpc.employee_policy_map epm2
    WHERE epm2.individual_id = d.individual_id
      AND epm2.policy_id = epm.policy_id
      AND epm2.status = 'ACTIVE'
  );
