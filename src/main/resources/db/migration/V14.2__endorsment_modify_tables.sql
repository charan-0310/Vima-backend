 
ALTER TABLE cpc.endorsements
  ADD COLUMN IF NOT EXISTS source_metadata JSONB DEFAULT '{}';
/* Example for self_enrollment:
{
  "windowId": "uuid",
  "windowName": "FY 2025-26 Annual",
  "enrollmentStartDate": "2026-03-01",
  "enrollmentEndDate": "2026-03-31"
}
*/
 
ALTER TABLE cpc.endorsements
  ADD COLUMN IF NOT EXISTS enrollment_window_id UUID REFERENCES cpc.enrollment_windows(id);

ALTER TABLE cpc.customers
  ALTER COLUMN phone DROP NOT NULL;

ALTER TYPE cpc.endorsement_status_enum ADD VALUE IF NOT EXISTS 'HRMS_SYNC';