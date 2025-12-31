INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('8a7b6c5d-4e3f-21a0-b9c8-d7e6f5a4b3c2', '5f2d3a1b-9c4e-4f15-8d2b-1a2b3c4d5e6f', 'ROLE_ADMIN', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('1a2b3c4d-5e6f-7081-92ab-cdef12345678', '5f2d3a1b-9c4e-4f15-8d2b-1a2b3c4d5e6f', 'ROLE_VIMA_ADMIN', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e', '0f1e2d3c-4b5a-6c7d-8e9f-0a1b2c3d4e5f', 'ROLE_VIMA_ADMIN', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f90', 'db12ab34-c56d-78ef-90ab-12cd34ef56ab', 'ROLE_VIMA_ADMIN', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9012', 'db12ab34-c56d-78ef-90ab-12cd34ef56ab', 'ROLE_SALES_AGENT', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f901234', '9999aaaa-bbbb-cccc-dddd-eeeeffff0000', 'ROLE_ADMIN', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('7a8b9c0d-1e2f-3a4b-5c6d-7e8f90123456', '9999aaaa-bbbb-cccc-dddd-eeeeffff0000', 'ROLE_VIMA_ADMIN', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('9c0d1e2f-3a4b-5c6d-7e8f-90123456789a', '11112222-3333-4444-5555-666677778888', 'ROLE_HR_ADMIN', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('8b9c0d1e-2f3a-4b5c-6d7e-8f9012345678', '9999aaaa-bbbb-cccc-dddd-eeeeffff0000', 'ROLE_HR_ADMIN', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f', '0f1e2d3c-4b5a-6c7d-8e9f-0a1b2c3d4e5f', 'ROLE_SALES_MANAGER', ARRAY['READ','WRITE','APPROVE']::permission_action[]);

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, actions)
VALUES ('0d1e2f3a-4b5c-6d7e-8f90-123456789abc', 'a3b2c1d4-5e6f-7a8b-9c0d-1e2f3a4b5c6d', 'ROLE_SALES_MANAGER', ARRAY['READ','WRITE','APPROVE']::permission_action[]);z