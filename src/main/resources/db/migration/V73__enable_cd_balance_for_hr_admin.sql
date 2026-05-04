-- Enable CD balance feature visibility for HR Admin.
-- This allows HR users to view CD balance on policy cards and open ledgers (read-only).

-- Ensure role-level permission is enabled for HR admin.
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT
    gen_random_uuid(),
    'c101d201-e302-f403-a504-b605c706d807',
    'ROLE_HR_ADMIN',
    TRUE,
    ARRAY['READ']::permission_action[]
WHERE NOT EXISTS (
    SELECT 1
    FROM admin.feature_flag_roles
    WHERE flag_id = 'c101d201-e302-f403-a504-b605c706d807'
      AND role_name = 'ROLE_HR_ADMIN'
);

UPDATE admin.feature_flag_roles
SET is_active = TRUE,
    actions = ARRAY['READ']::permission_action[]
WHERE flag_id = 'c101d201-e302-f403-a504-b605c706d807'
  AND role_name = 'ROLE_HR_ADMIN';
