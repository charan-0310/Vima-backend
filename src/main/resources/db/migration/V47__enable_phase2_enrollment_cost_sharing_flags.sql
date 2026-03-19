-- Enable Phase 2 enrollment feature flags (cost-sharing, top-up, parent coverage) for admin roles
-- so cost-sharing-rules, top-up options, and parent coverage APIs work without manual flag toggle.
UPDATE admin.feature_flag_roles
SET is_active = true
WHERE flag_id IN (
    'b101c201-d302-e403-f504-a605b706c807',  -- enrollment.cost-sharing
    'b202c302-d403-e504-f605-a706b807c908',  -- enrollment.topup-plans
    'b303c403-d504-e605-f706-a807b908c009'   -- enrollment.parent-coverage
  )
  AND role_name IN ('ROLE_VIMA_ADMIN', 'ROLE_HR_ADMIN');
