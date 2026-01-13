
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('1a2b3c4d-5e6f-7081-92ab-cdef12345678', '5f2d3a1b-9c4e-4f15-8d2b-1a2b3c4d5e6f', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e', '0f1e2d3c-4b5a-6c7d-8e9f-0a1b2c3d4e5f', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f90', 'db12ab34-c56d-78ef-90ab-12cd34ef56ab', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9012', 'db12ab34-c56d-78ef-90ab-12cd34ef56ab', 'ROLE_SALES_AGENT', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('7a8b9c0d-1e2f-3a4b-5c6d-7e8f90123456', '9999aaaa-bbbb-cccc-dddd-eeeeffff0000', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('9c0d1e2f-3a4b-5c6d-7e8f-90123456789a', '11112222-3333-4444-5555-666677778888', 'ROLE_HR_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('8b9c0d1e-2f3a-4b5c-6d7e-8f9012345678', '9999aaaa-bbbb-cccc-dddd-eeeeffff0000', 'ROLE_HR_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('d3f6a8e2-9b47-4c1a-8f2e-6c9a3f12b4cd', 'f0e1d2c3-b4a5-9607-1827-374657485960', 'ROLE_HR_ADMIN', TRUE, ARRAY['READ','WRITE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('a1c2b3d4-e5f6-47a8-9c0d-1e2f3a4b5c6d', '5f2d3a1b-9c4e-4f15-8d2b-1a2b3c4d5e6f', 'ROLE_HR_ADMIN', TRUE, ARRAY['READ','WRITE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f', '0f1e2d3c-4b5a-6c7d-8e9f-0a1b2c3d4e5f', 'ROLE_SALES_MANAGER', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('0d1e2f3a-4b5c-6d7e-8f90-123456789abc', 'a3b2c1d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d', 'ROLE_SALES_MANAGER', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('1e2f3a4b-5c6d-7e8f-9012-3456789abcde', '123e4567-e89b-12d3-a456-426614174000', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('2f3a4b5c-6d7e-8f90-1234-56789abcdef0', 'abcdef12-3456-7890-abcd-ef1234567890', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
VALUES ('3a4b5c6d-7e8f-9012-3456-789abcdef012', '0a1b2c3d-4e5f-6789-0a1b-2c3d4e5f6789', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (flag_id, role_name, is_active, actions)
VALUES ('955a1543-1098-40e4-a0be-53b767a03815', 'ROLE_VIMA_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (flag_id, role_name, is_active, actions)
VALUES ( '955a1543-1098-40e4-a0be-53b767a03815', 'ROLE_HR_ADMIN', TRUE, ARRAY['READ','WRITE','APPROVE']::permission_action[]);
