-- ============================================================
-- Wellness feature flag + role mappings
-- Ticket: VIMA-432
-- NOTE: Originally numbered as V64, but shifted to V68 because
-- V65 was already introduced/applied in this branch history.
-- ============================================================

INSERT INTO admin.feature_flags (flag_id, flag_key, description, is_active)
VALUES (
    'f6c2f585-7406-41ca-8e44-1df529f59071',
    'wellness',
    'Wellness Partners - employee portal wellness tab and partner cards',
    TRUE
)
ON CONFLICT (flag_key) DO UPDATE SET
    description = EXCLUDED.description;

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT
    gen_random_uuid(),
    ff.flag_id,
    'ROLE_EMPLOYEE',
    TRUE,
    ARRAY['READ']::permission_action[]
FROM admin.feature_flags ff
WHERE ff.flag_key = 'wellness'
ON CONFLICT (role_name, flag_id) DO UPDATE SET
    is_active = EXCLUDED.is_active,
    actions = EXCLUDED.actions;

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT
    gen_random_uuid(),
    ff.flag_id,
    'ROLE_VIMA_ADMIN',
    TRUE,
    ARRAY['READ', 'WRITE']::permission_action[]
FROM admin.feature_flags ff
WHERE ff.flag_key = 'wellness'
ON CONFLICT (role_name, flag_id) DO UPDATE SET
    is_active = EXCLUDED.is_active,
    actions = EXCLUDED.actions;

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT
    gen_random_uuid(),
    ff.flag_id,
    'ROLE_HR_ADMIN',
    TRUE,
    ARRAY['READ']::permission_action[]
FROM admin.feature_flags ff
WHERE ff.flag_key = 'wellness'
ON CONFLICT (role_name, flag_id) DO UPDATE SET
    is_active = EXCLUDED.is_active,
    actions = EXCLUDED.actions;
