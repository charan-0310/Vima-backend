-- Create endorsement_status_enum with common values for enrollment window, invitation, and submission statuses
-- Values: active, pending_approval, rejected, inactive, draft, submitted, approved, endorsed, scheduled, closed, cancelled
CREATE TYPE cpc.endorsement_status_enum AS ENUM (
    -- Window: scheduled, active, closed, cancelled
    'ACTIVE',
    'SCHEDULED',
    'CLOSED',
    'CANCELLED',
    -- Invitation: pending, sent, opened, in_progress, completed, expired
    'PENDING',
    'SENT',
    'OPENED',
    'IN_PROGRESS',
    'COMPLETED',
    'EXPIRED',
    -- Submission: draft, submitted, approved, rejected, endorsed
    'DRAFT',
    'SUBMITTED',
    'APPROVED',
    'REJECTED',
    'ENDORSED',
    'PENDING_APPROVAL',
    'INACTIVE'
);

CREATE TABLE cpc.enrollment_windows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES cpc.organizations(organization_id),
    
    -- Basic Info
    name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Schedule
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    
    -- Status (scheduled, active, closed, cancelled)
    status cpc.endorsement_status_enum NOT NULL DEFAULT 'SCHEDULED',
    
    -- Configuration (stored as JSONB)
    config JSONB DEFAULT '{}',
    /* Example config:
    {
      "reminderEnabled": true,
      "reminderFrequencyDays": 3,
      "autoBatchEnabled": true,
      "parentCoverageEnabled": false,
      "maxDependents": 7,
      "gmcMandatory": true,
      "gpaMandatory": true,
      "gtlMandatory": true
    }
    */
    
    -- Audit
    created_by UUID NOT NULL REFERENCES admin.admin_users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    closed_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT valid_date_range CHECK (end_date > start_date)
);

CREATE INDEX idx_enrollment_windows_org ON cpc.enrollment_windows(organization_id);
CREATE INDEX idx_enrollment_windows_status ON cpc.enrollment_windows(status);
CREATE INDEX idx_enrollment_windows_dates ON cpc.enrollment_windows(start_date, end_date);


CREATE TABLE cpc.enrollment_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_window_id UUID NOT NULL REFERENCES cpc.enrollment_windows(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES cpc.customers(individual_id),
    
    -- Token (stored hashed for security)
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    
    -- Status tracking
    status cpc.endorsement_status_enum NOT NULL DEFAULT 'PENDING',
    -- Values: pending, sent, opened, in_progress, completed, expired
    
    -- Timestamps
    sent_at TIMESTAMP WITH TIME ZONE,
    opened_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Reminders
    reminder_count INTEGER DEFAULT 0,
    last_reminder_at TIMESTAMP WITH TIME ZONE,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT unique_employee_window UNIQUE (employee_id, enrollment_window_id)
);

CREATE INDEX idx_invitations_window ON cpc.enrollment_invitations(enrollment_window_id);
CREATE INDEX idx_invitations_employee ON cpc.enrollment_invitations(employee_id);
CREATE INDEX idx_invitations_status ON cpc.enrollment_invitations(status);
CREATE INDEX idx_invitations_token ON cpc.enrollment_invitations(token_hash);


CREATE TABLE cpc.enrollment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES cpc.customers(individual_id),
    enrollment_window_id UUID NOT NULL REFERENCES enrollment_windows(id),
    invitation_id UUID REFERENCES enrollment_invitations(id),
    endorsement_id UUID REFERENCES cpc.endorsements(endorsement_id),
    
    -- Reference number (human-readable)
    reference_number VARCHAR(50) UNIQUE,
    
    -- Status
    status cpc.endorsement_status_enum NOT NULL DEFAULT 'DRAFT',
    -- Values: draft, submitted, approved, rejected, endorsed
    
    -- All enrollment data in JSONB (no separate tables!)
    plan_selections JSONB DEFAULT '[]',
    /* Example:
    [
      {"planType": "GMC", "opted": true, "coverageAmount": 500000, "premium": 5000},
      {"planType": "GTL", "opted": true, "coverageAmount": 1500000, "premium": 3000}
    ]
    */
    
    nominee_data JSONB DEFAULT '{}',
    /* Example:
    {
      "GTL": [
        {"fullName": "Spouse Name", "relationship": "Spouse", "percentage": 60, "contact": "9876543210"},
        {"fullName": "Child Name", "relationship": "Child", "percentage": 40}
      ],
      "GPA": [
        {"fullName": "Spouse Name", "relationship": "Spouse", "percentage": 100}
      ]
    }
    */
    
    premium_breakdown JSONB DEFAULT '{}',
    /* Example:
    {
      "employee": {"gmc": 5000, "gtl": 3000, "gpa": 2000},
      "dependents": [
        {"name": "Spouse", "premium": 4000},
        {"name": "Child", "premium": 3000}
      ],
      "total": 17000,
      "employerShare": 12000,
      "employeeShare": 5000
    }
    */
    
    -- Submission timestamps
    submitted_at TIMESTAMP WITH TIME ZONE,
    
    -- Review
    reviewed_by UUID REFERENCES admin.admin_users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    
    -- Declaration
    declaration_accepted BOOLEAN DEFAULT FALSE,
    declaration_timestamp TIMESTAMP WITH TIME ZONE,
    declaration_ip_address INET,
    
    -- Concurrency control
    version INTEGER DEFAULT 1,
    idempotency_key VARCHAR(64) UNIQUE,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT unique_employee_submission UNIQUE (employee_id, enrollment_window_id)
);

CREATE INDEX idx_submissions_employee ON cpc.enrollment_submissions(employee_id);
CREATE INDEX idx_submissions_window ON cpc.enrollment_submissions(enrollment_window_id);
CREATE INDEX idx_submissions_status ON cpc.enrollment_submissions(status);
CREATE INDEX idx_submissions_reference ON cpc.enrollment_submissions(reference_number);
CREATE INDEX idx_submissions_endorsement ON cpc.enrollment_submissions(endorsement_id);