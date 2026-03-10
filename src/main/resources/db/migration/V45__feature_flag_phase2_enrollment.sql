-- BE-05: Phase 2 enrollment feature flags (cost-sharing, top-up, parent coverage, payroll reports)
INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
VALUES ('b101c201-d302-e403-f504-a605b706c807', 'enrollment.cost-sharing', 'Cost-sharing rules and employer/employee split', 'a1b2c3d4-e5f6-4789-a012-3456789abcde', FALSE)
ON CONFLICT (flag_id) DO NOTHING;

INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
VALUES ('b202c302-d403-e504-f605-a706b807c908', 'enrollment.topup-plans', 'Top-up and super top-up plan options', 'a1b2c3d4-e5f6-4789-a012-3456789abcde', FALSE)
ON CONFLICT (flag_id) DO NOTHING;

INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
VALUES ('b303c403-d504-e605-f706-a807b908c009', 'enrollment.parent-coverage', 'Parent and parent-in-law coverage', 'a1b2c3d4-e5f6-4789-a012-3456789abcde', FALSE)
ON CONFLICT (flag_id) DO NOTHING;

INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
VALUES ('b404c504-d605-e706-f807-a908b009c010', 'enrollment.payroll-reports', 'Payroll deduction reports and scheduling', 'a1b2c3d4-e5f6-4789-a012-3456789abcde', FALSE)
ON CONFLICT (flag_id) DO NOTHING;

-- Feature flag roles so they appear on management page (disabled by default)
INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'b101c201-d302-e403-f504-a605b706c807', 'ROLE_VIMA_ADMIN', FALSE, ARRAY['READ','WRITE','APPROVE']::permission_action[]
WHERE NOT EXISTS (SELECT 1 FROM admin.feature_flag_roles WHERE flag_id = 'b101c201-d302-e403-f504-a605b706c807' AND role_name = 'ROLE_VIMA_ADMIN');

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'b101c201-d302-e403-f504-a605b706c807', 'ROLE_HR_ADMIN', FALSE, ARRAY['READ','WRITE']::permission_action[]
WHERE NOT EXISTS (SELECT 1 FROM admin.feature_flag_roles WHERE flag_id = 'b101c201-d302-e403-f504-a605b706c807' AND role_name = 'ROLE_HR_ADMIN');

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'b202c302-d403-e504-f605-a706b807c908', 'ROLE_VIMA_ADMIN', FALSE, ARRAY['READ','WRITE','APPROVE']::permission_action[]
WHERE NOT EXISTS (SELECT 1 FROM admin.feature_flag_roles WHERE flag_id = 'b202c302-d403-e504-f605-a706b807c908' AND role_name = 'ROLE_VIMA_ADMIN');

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'b202c302-d403-e504-f605-a706b807c908', 'ROLE_HR_ADMIN', FALSE, ARRAY['READ','WRITE']::permission_action[]
WHERE NOT EXISTS (SELECT 1 FROM admin.feature_flag_roles WHERE flag_id = 'b202c302-d403-e504-f605-a706b807c908' AND role_name = 'ROLE_HR_ADMIN');

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'b303c403-d504-e605-f706-a807b908c009', 'ROLE_VIMA_ADMIN', FALSE, ARRAY['READ','WRITE','APPROVE']::permission_action[]
WHERE NOT EXISTS (SELECT 1 FROM admin.feature_flag_roles WHERE flag_id = 'b303c403-d504-e605-f706-a807b908c009' AND role_name = 'ROLE_VIMA_ADMIN');

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'b303c403-d504-e605-f706-a807b908c009', 'ROLE_HR_ADMIN', FALSE, ARRAY['READ','WRITE']::permission_action[]
WHERE NOT EXISTS (SELECT 1 FROM admin.feature_flag_roles WHERE flag_id = 'b303c403-d504-e605-f706-a807b908c009' AND role_name = 'ROLE_HR_ADMIN');

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'b404c504-d605-e706-f807-a908b009c010', 'ROLE_VIMA_ADMIN', FALSE, ARRAY['READ','WRITE','APPROVE']::permission_action[]
WHERE NOT EXISTS (SELECT 1 FROM admin.feature_flag_roles WHERE flag_id = 'b404c504-d605-e706-f807-a908b009c010' AND role_name = 'ROLE_VIMA_ADMIN');

INSERT INTO admin.feature_flag_roles (id, flag_id, role_name, is_active, actions)
SELECT gen_random_uuid(), 'b404c504-d605-e706-f807-a908b009c010', 'ROLE_HR_ADMIN', FALSE, ARRAY['READ','WRITE']::permission_action[]
WHERE NOT EXISTS (SELECT 1 FROM admin.feature_flag_roles WHERE flag_id = 'b404c504-d605-e706-f807-a908b009c010' AND role_name = 'ROLE_HR_ADMIN');
