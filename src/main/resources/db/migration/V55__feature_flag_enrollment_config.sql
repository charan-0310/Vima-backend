-- Add dedicated feature flag for enrollment configuration management.
INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
SELECT
    'b505c605-d706-e807-f908-a009b101c111',
    'enrollment.config',
    'Enrollment configuration management (company enrollment config)',
    'a1b2c3d4-e5f6-4789-a012-3456789abcde',
    FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM admin.feature_flags
    WHERE flag_key = 'enrollment.config'
);

-- Add role mappings so the feature appears in Feature Management.
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT
    gen_random_uuid(),
    'b505c605-d706-e807-f908-a009b101c111',
    'ROLE_VIMA_ADMIN',
    TRUE,
    ARRAY['READ','WRITE','APPROVE']::permission_action[]
WHERE EXISTS (
    SELECT 1
    FROM admin.feature_flags
    WHERE flag_id = 'b505c605-d706-e807-f908-a009b101c111'
)
AND NOT EXISTS (
    SELECT 1
    FROM admin.feature_flag_roles
    WHERE flag_id = 'b505c605-d706-e807-f908-a009b101c111'
      AND role_name = 'ROLE_VIMA_ADMIN'
);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT
    gen_random_uuid(),
    'b505c605-d706-e807-f908-a009b101c111',
    'ROLE_HR_ADMIN',
    FALSE,
    ARRAY['READ','WRITE']::permission_action[]
WHERE EXISTS (
    SELECT 1
    FROM admin.feature_flags
    WHERE flag_id = 'b505c605-d706-e807-f908-a009b101c111'
)
AND NOT EXISTS (
    SELECT 1
    FROM admin.feature_flag_roles
    WHERE flag_id = 'b505c605-d706-e807-f908-a009b101c111'
      AND role_name = 'ROLE_HR_ADMIN'
);
