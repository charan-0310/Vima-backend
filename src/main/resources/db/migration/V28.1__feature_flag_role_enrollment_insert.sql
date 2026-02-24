-- VIMA_ADMIN: read and write actions, enabled by default for enrollment
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('c3d4e5f6-a7b8-4901-c234-56789abcdef0', 'a1b2c3d4-e5f6-4789-a012-3456789abcde', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE']::permission_action[]);

-- HR_ADMIN: read and write actions, enabled by default for enrollment
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('d4e5f6a7-b8c9-4012-d345-6789abcdef01', 'a1b2c3d4-e5f6-4789-a012-3456789abcde', 'ROLE_HR_ADMIN', TRUE, ARRAY['READ','WRITE']::permission_action[]);
