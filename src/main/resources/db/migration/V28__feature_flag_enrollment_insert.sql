-- Feature flag for enrollment under group insurance
INSERT INTO admin.feature_flags (flag_id, flag_key, description, parent_flag_id, is_active)
VALUES ('a1b2c3d4-e5f6-4789-a012-3456789abcde', 'group-insurance.enrollment', 'Enrollment functionality under group insurance', '11112222-3333-4444-5555-666677778888', TRUE);

INSERT INTO admin.feature_flags (flag_id, flag_key, description, is_active)
VALUES ('a1b2c3d4-e5f6-4789-a012-3456789abcdf', 'claims', 'Claims Dashboard', TRUE);
