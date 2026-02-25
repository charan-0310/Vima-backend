-- Rename standalone claims flag to claims-management and add feature_flag_roles
-- so it appears on Feature Flag page and in auth/me for VIMA_ADMIN and HR_ADMIN.

-- 1. Rename claims -> claims-management
UPDATE admin.feature_flags
SET flag_key = 'claims-management',
    description = 'Claims Management (admin/hr claims)'
WHERE flag_key = 'claims';

-- 2. Add roles for claims-management (flag_id from V28: a1b2c3d4-e5f6-4789-a012-3456789abcdf)
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES
  (gen_random_uuid(), 'a1b2c3d4-e5f6-4789-a012-3456789abcdf', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]),
  (gen_random_uuid(), 'a1b2c3d4-e5f6-4789-a012-3456789abcdf', 'ROLE_HR_ADMIN', TRUE, ARRAY['READ']::permission_action[]);
