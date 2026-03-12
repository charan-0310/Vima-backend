-- Enable enrollment.payroll-reports feature flag for admin roles so payroll report download works by default
UPDATE admin.feature_flag_roles
SET is_active = true
WHERE flag_id = 'b404c504-d605-e706-f807-a908b009c010'
  AND role_name IN ('ROLE_VIMA_ADMIN', 'ROLE_HR_ADMIN');
