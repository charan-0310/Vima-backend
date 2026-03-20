-- Feature flag for enrollment config under group-insurance.enrollment
INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
VALUES (
    'b505c605-d706-e807-f908-a009b101c111',
    'group-insurance.enrollment-config',
    'Enrollment configuration management (company enrollment config)',
    '11112222-3333-4444-5555-666677778888',
    TRUE
);

-- VIMA_ADMIN: read and write actions, enabled by default
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES (
    'e605f706-a807-4908-b109-c210d311e412',
    'b505c605-d706-e807-f908-a009b101c111',
    'ROLE_VIMA_ADMIN',
    TRUE,
    ARRAY['READ','WRITE']::permission_action[]
);

-- HR_ADMIN: read and write actions, enabled by default
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES (
    'f706a807-b908-4a10-c211-d312e413f514',
    'b505c605-d706-e807-f908-a009b101c111',
    'ROLE_HR_ADMIN',
    TRUE,
    ARRAY['READ','WRITE']::permission_action[]
);
