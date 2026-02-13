ALTER TABLE cpc.enrollment_submissions
   ADD COLUMN IF NOT EXISTS personal_details JSONB DEFAULT '{}';

ALTER TABLE cpc.enrollment_submissions
   ADD COLUMN IF NOT EXISTS dependents JSONB DEFAULT '{}';

ALTER TABLE cpc.enrollment_submissions
   ADD COLUMN IF NOT EXISTS stage varchar(50) DEFAULT '';      