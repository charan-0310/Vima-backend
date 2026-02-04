-- Add enrollment status tracking
ALTER TABLE cpc.customers 
  ADD COLUMN IF NOT EXISTS enrollment_status cpc.endorsement_status_enum DEFAULT 'ACTIVE';
-- Values: active, pending_approval, rejected, inactive

ALTER TABLE cpc.customers
  ADD COLUMN IF NOT EXISTS enrollment_window_id UUID REFERENCES enrollment_windows(id);

ALTER TABLE cpc.customers
  ADD COLUMN IF NOT EXISTS enrollment_submission_id UUID REFERENCES enrollment_submissions(id);

CREATE INDEX IF NOT EXISTS idx_customers_status ON cpc.customers(status);
CREATE INDEX IF NOT EXISTS idx_customers_enrollment_window ON cpc.customers(enrollment_window_id);

CREATE TYPE cpc.endorsement_source_enum AS ENUM (
  'CSV_UPLOAD',
  'SELF_ENROLLMENT',
  'API',
  'MANUAL'
);

-- Track endorsement source
ALTER TABLE cpc.endorsements 
  ADD COLUMN IF NOT EXISTS source cpc.endorsement_source_enum DEFAULT 'CSV_UPLOAD';
-- Values: csv_upload, self_enrollment, api, manual

ALTER TABLE cpc.endorsements
  ADD COLUMN IF NOT EXISTS enrollment_window_id UUID REFERENCES enrollment_windows(id);

ALTER TABLE cpc.endorsements
  ADD COLUMN IF NOT EXISTS submission_count INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_endorsements_source ON cpc.endorsements(source);
CREATE INDEX IF NOT EXISTS idx_endorsements_enrollment_window ON cpc.endorsements(enrollment_window_id);


-- Make policy_id nullable and add submission_id
ALTER TABLE cpc.nominees 
  ALTER COLUMN policy_id DROP NOT NULL;

ALTER TABLE cpc.nominees
  ADD COLUMN IF NOT EXISTS submission_id UUID REFERENCES enrollment_submissions(id);

ALTER TABLE cpc.nominees
  ADD COLUMN IF NOT EXISTS customer_id UUID REFERENCES cpc.customers(individual_id);

-- Add constraint: Must have either policy_id OR submission_id
ALTER TABLE cpc.nominees
  ADD CONSTRAINT IF NOT EXISTS check_policy_or_submission 
  CHECK (
    (policy_id IS NOT NULL AND submission_id IS NULL) OR 
    (policy_id IS NULL AND submission_id IS NOT NULL)
  );

CREATE INDEX IF NOT EXISTS idx_nominees_submission ON cpc.nominees(submission_id);