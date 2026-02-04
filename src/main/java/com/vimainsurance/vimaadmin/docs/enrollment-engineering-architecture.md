# Employee Self-Service Enrollment - Backend Implementation Plan

> **Implementation Approach:** Simplified backend-first development with maximum reuse of existing tables.
> **Team:** 2 mid-level developers with Cursor AI + Copilot (parallel development)
> **Timeline:** 4-5 weeks for backend + 1 week for frontend integration (~5-6 weeks total)
> **Status:** Planning phase
> **AI Acceleration:** ~50% faster with Cursor AI assistance

## Overview

This document outlines the **simplified backend implementation** for the Employee Self-Service Enrollment workflow. The system leverages existing tables (`customers`, `endorsements`, `nominees`) with minimal new tables.

**Key Principle:** 
- Use existing tables with status fields to track draft/pending/approved states
- Only 3-4 new tables for enrollment-specific functionality
- No data migration complexity - just status updates

---

## 1. Simplified Database Schema

### **New Tables (Only 3)**

#### 1.1 enrollment_windows
Core table for time-bound enrollment periods.

```sql
CREATE TABLE enrollment_windows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES cpc.organizations(organization_id),
    
    -- Basic Info
    name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Schedule
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    
    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'scheduled',
    -- Values: scheduled, active, closed, cancelled
    
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

CREATE INDEX idx_enrollment_windows_org ON enrollment_windows(organization_id);
CREATE INDEX idx_enrollment_windows_status ON enrollment_windows(status);
CREATE INDEX idx_enrollment_windows_dates ON enrollment_windows(start_date, end_date);
```

#### 1.2 enrollment_invitations
Tracks magic link tokens sent to employees.

```sql
CREATE TABLE enrollment_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_window_id UUID NOT NULL REFERENCES enrollment_windows(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES cpc.customers(individual_id),
    
    -- Token (stored hashed for security)
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    
    -- Status tracking
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
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

CREATE INDEX idx_invitations_window ON enrollment_invitations(enrollment_window_id);
CREATE INDEX idx_invitations_employee ON enrollment_invitations(employee_id);
CREATE INDEX idx_invitations_status ON enrollment_invitations(status);
CREATE INDEX idx_invitations_token ON enrollment_invitations(token_hash);
```

#### 1.3 enrollment_submissions
Submission header with all enrollment data in JSONB.

```sql
CREATE TABLE enrollment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES cpc.customers(individual_id),
    enrollment_window_id UUID NOT NULL REFERENCES enrollment_windows(id),
    invitation_id UUID REFERENCES enrollment_invitations(id),
    endorsement_id UUID REFERENCES cpc.endorsements(endorsement_id),
    
    -- Reference number (human-readable)
    reference_number VARCHAR(50) UNIQUE,
    
    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'draft',
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

CREATE INDEX idx_submissions_employee ON enrollment_submissions(employee_id);
CREATE INDEX idx_submissions_window ON enrollment_submissions(enrollment_window_id);
CREATE INDEX idx_submissions_status ON enrollment_submissions(status);
CREATE INDEX idx_submissions_reference ON enrollment_submissions(reference_number);
CREATE INDEX idx_submissions_endorsement ON enrollment_submissions(endorsement_id);
```

#### 1.4 Config Storage Pattern

All company-specific enrollment rules are stored in the `enrollment_windows.config` JSONB field. This provides:
- ✅ Per-window configuration flexibility
- ✅ No additional tables needed
- ✅ Easy to modify rules between enrollment periods

**Example Java class for type-safe config handling:**

```java
@Data
public class EnrollmentConfig {
    // Coverage options
    private Boolean parentCoverageEnabled = false;
    private Boolean topupEnabled = false;
    
    // Limits
    private Integer maxDependents = 7;
    private Integer maxChildren = 4;
    private Integer maxParents = 4;
    
    // Plan rules
    private Boolean gmcMandatory = true;
    private Boolean gpaMandatory = true;
    private Boolean gtlMandatory = true;
    
    // Automation settings
    private Boolean reminderEnabled = true;
    private Integer reminderFrequencyDays = 3;
    private Boolean autoBatchEnabled = true;
    
    // Premium calculation
    private Integer parentEmployerSharePercentage = 50;
    private Map<String, PremiumRateTable> premiumRules = new HashMap<>();
    
    @Data
    public static class PremiumRateTable {
        private String ageRange;
        private BigDecimal premium;
    }
}
```

---

### **Modified Existing Tables**

#### 2.1 cpc.customers (Add status field)

```sql
-- Add enrollment status tracking
ALTER TABLE cpc.customers 
  ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'active';
-- Values: active, pending_approval, rejected, inactive

ALTER TABLE cpc.customers
  ADD COLUMN IF NOT EXISTS enrollment_window_id UUID REFERENCES enrollment_windows(id);

ALTER TABLE cpc.customers
  ADD COLUMN IF NOT EXISTS enrollment_submission_id UUID REFERENCES enrollment_submissions(id);

CREATE INDEX IF NOT EXISTS idx_customers_status ON cpc.customers(status);
CREATE INDEX IF NOT EXISTS idx_customers_enrollment_window ON cpc.customers(enrollment_window_id);
```

**Usage:**
- Employee adds dependent during enrollment → Insert with `status='pending_approval'`
- HR approves submission → Update `status='active'`
- HR rejects submission → Delete or update `status='rejected'`

#### 2.2 cpc.endorsements (Add source field)

```sql
-- Track endorsement source
ALTER TABLE cpc.endorsements 
  ADD COLUMN IF NOT EXISTS source VARCHAR(50) DEFAULT 'csv_upload';
-- Values: csv_upload, self_enrollment, api, manual

ALTER TABLE cpc.endorsements
  ADD COLUMN IF NOT EXISTS enrollment_window_id UUID REFERENCES enrollment_windows(id);

ALTER TABLE cpc.endorsements
  ADD COLUMN IF NOT EXISTS submission_count INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_endorsements_source ON cpc.endorsements(source);
CREATE INDEX IF NOT EXISTS idx_endorsements_enrollment_window ON cpc.endorsements(enrollment_window_id);
```

#### 2.3 cpc.nominees (Add submission_id field)

```sql
-- Make policy_id nullable and add submission_id
ALTER TABLE cpc.nominees 
  ALTER COLUMN policy_id DROP NOT NULL;

ALTER TABLE cpc.nominees
  ADD COLUMN IF NOT EXISTS submission_id UUID REFERENCES enrollment_submissions(id);

-- Add constraint: Must have either policy_id OR submission_id
ALTER TABLE cpc.nominees
  ADD CONSTRAINT IF NOT EXISTS check_policy_or_submission 
  CHECK (
    (policy_id IS NOT NULL AND submission_id IS NULL) OR 
    (policy_id IS NULL AND submission_id IS NOT NULL)
  );

CREATE INDEX IF NOT EXISTS idx_nominees_submission ON cpc.nominees(submission_id);
```

**Usage:**
- During enrollment → Create nominee with `submission_id`, `policy_id=null`
- After policy assignment → Update `policy_id`, set `submission_id=null`

---

## 2. Simplified Implementation Plan

### **Phase 1: Database Foundation**
**Duration:** 1 week | **Tickets:** BE-001, BE-002

- Create 3 new tables via Flyway migrations
- Modify existing tables (add status, source, submission_id columns)
- Create JPA entities and repositories
- Add enum types

### **Phase 2: Core Services & Security**
**Duration:** 1 week | **Tickets:** BE-003

- Token generation and validation service
- Rate limiting
- JWT authentication
- Access control service

### **Phase 3: Admin APIs**
**Duration:** 1.5 weeks | **Tickets:** BE-004, BE-005

- Enrollment window management APIs
- Bulk invitation sending
- Email integration

### **Phase 4: Employee Enrollment APIs**
**Duration:** 2 weeks | **Tickets:** BE-006, BE-007, BE-008

- Token validation and profile API
- Dependent management (direct to customers table)
- Plan selection and nominee management
- Final submission API

### **Phase 5: HR Approval & Batch**
**Duration:** 1.5 weeks | **Tickets:** BE-009, BE-010

- HR approval/rejection APIs
- Batch processing service
- Status updates (no data migration!)

### **Phase 6: Automation & Config**
**Duration:** 1 week | **Tickets:** BE-012, BE-013

- Scheduled jobs (reminders, expiry, auto-batch)
- Configuration management APIs

### **Phase 7: Testing & Documentation**
**Duration:** 1 week | **Tickets:** BE-014

- Unit and integration tests
- API documentation (Swagger)
- Monitoring setup

**Total Backend Timeline: 8 weeks** (Without Cursor AI)

---

## 2.1. Accelerated Parallel Development Plan (With Cursor AI)

> **Team:** 2 mid-level developers with Cursor AI + GitHub Copilot
> **Knowledge:** Well-versed with existing DB and Java code
> **Acceleration:** ~50% faster development with AI assistance
> **Strategy:** Parallel development after foundational setup

### **📅 Week 1: Foundation Setup (Both Devs - Pair/Collaborate)**

| Dev | Tickets | Tasks | Days |
|-----|---------|-------|------|
| **Dev1 + Dev2** | BE-001 | Create 3 tables (Flyway migrations) | 0.5 |
| **Dev1 + Dev2** | BE-002 | JPA entities & repositories | 1.0 |
| **Dev1 + Dev2** | BE-003 | Token security service | 1.5 |

**Week 1 Total: 3 days** (2 days buffer for environment setup, code review)

**Deliverables:**
- ✅ Database schema deployed to dev
- ✅ All entities and repositories working
- ✅ Token generation/validation tested
- ✅ Both devs understand codebase structure

---

### **📅 Week 2: Parallel Track - Admin & Employee APIs**

| Dev | Tickets | Tasks | Days |
|-----|---------|-------|------|
| **Dev1** | BE-004 | Enrollment window CRUD APIs | 2.0 |
| **Dev1** | BE-005 | Invitation & email service APIs | 2.0 |
| **Dev1** | BE-013 | Window config management | 1.0 |
| | | |
| **Dev2** | BE-006 | Token validation & profile API | 1.5 |
| **Dev2** | BE-007 | Dependent management APIs | 2.5 |
| **Dev2** | Start BE-008 | Plan selection API (partial) | 1.0 |

**Week 2 Total: 5 days per dev**

**Deliverables:**
- ✅ **Dev1:** Admin portal backend complete (windows, invitations, config)
- ✅ **Dev2:** Employee enrollment flow 70% complete

**Dependencies Resolved:** Both work independently, no blocking

---

### **📅 Week 3: Parallel Track - HR & Employee Completion**

| Dev | Tickets | Tasks | Days |
|-----|---------|-------|------|
| **Dev1** | BE-010 | HR approval/rejection APIs | 2.0 |
| **Dev1** | BE-011 | Batch processing & endorsement | 2.5 |
| **Dev1** | | Buffer/code review | 0.5 |
| | | |
| **Dev2** | Complete BE-008 | Plan selection & nominee APIs | 1.5 |
| **Dev2** | BE-009 | Submission & declaration API | 1.5 |
| **Dev2** | Start BE-014 | Unit tests for employee APIs | 2.0 |

**Week 3 Total: 5 days per dev**

**Deliverables:**
- ✅ **Dev1:** HR portal + batch processing complete
- ✅ **Dev2:** All employee APIs complete + tested

---

### **📅 Week 4: Automation, Testing & Integration**

| Dev | Tickets | Tasks | Days |
|-----|---------|-------|------|
| **Dev1** | BE-012 | Background jobs & schedulers | 2.0 |
| **Dev1** | BE-014 | Integration tests, E2E tests | 2.0 |
| **Dev1** | | Performance testing | 1.0 |
| | | |
| **Dev2** | BE-014 | API documentation (Swagger) | 1.5 |
| **Dev2** | BE-014 | Monitoring setup (Actuator) | 1.0 |
| **Dev2** | BE-014 | Bug fixes, edge cases | 1.5 |
| **Both** | | Code review together | 1.0 |

**Week 4 Total: 5 days per dev**

**Deliverables:**
- ✅ All features complete
- ✅ Automated tests passing
- ✅ API documentation published
- ✅ Monitoring configured

---

### **📅 Week 5: Polish & Deployment (Buffer Week)**

| Dev | Tasks | Days |
|-----|-------|------|
| **Both** | Final code review & refactoring | 1.0 |
| **Both** | Load testing & optimization | 1.0 |
| **Both** | Deployment scripts & runbook | 0.5 |
| **Both** | Staging deployment & smoke tests | 1.0 |
| **Both** | Bug fixes from QA/testing | 1.5 |

**Week 5 Total: 5 days**

**Deliverables:**
- ✅ Production-ready backend
- ✅ All tests passing
- ✅ Documentation complete
- ✅ Deployed to staging

---

## 2.2. Detailed Task Assignment

### **Why This Split Works:**

**Dev1 Focus:** Admin & Management Layer
- Enrollment windows (BE-004)
- Invitations & emails (BE-005)
- HR approval (BE-010)
- Batch processing (BE-011)
- Background jobs (BE-012)
- Config management (BE-013)

**Dev2 Focus:** Employee-Facing APIs
- Token validation (BE-006)
- Dependent management (BE-007)
- Plan selection (BE-008)
- Submission (BE-009)
- Testing & documentation (BE-014)

**Benefits:**
- ✅ No dependency conflicts (clean separation)
- ✅ Balanced workload (~28 SP each)
- ✅ Each dev owns a complete feature vertical
- ✅ Minimal merge conflicts

---

## 2.3. Cursor AI Acceleration Breakdown

| Task Type | Without Cursor | With Cursor | Time Saved |
|-----------|---------------|-------------|------------|
| Database migrations | 4 hours | 1.5 hours | 62% |
| JPA entities | 6 hours | 2 hours | 67% |
| CRUD controllers | 8 hours | 3 hours | 62% |
| Service layer | 12 hours | 6 hours | 50% |
| Unit tests | 10 hours | 2 hours | 80% |
| Integration tests | 8 hours | 4 hours | 50% |
| Swagger docs | 4 hours | 0.5 hours | 87% |
| Bug fixing | 6 hours | 3 hours | 50% |

**Average Acceleration: ~60% faster** for experienced devs using Cursor effectively

---

## 2.4. Risk Mitigation

**Week 1 is Critical:**
- Both devs must understand the architecture
- Database schema must be solid (changes later are costly)
- Token service is foundational for all APIs

**Synchronization Points:**
- Daily standups (15 min)
- End of Week 2: Integration test (Dev1 APIs + Dev2 APIs)
- End of Week 3: Code review together
- Week 4: Pair on tricky bugs

**If Behind Schedule:**
- Defer BE-012 (Background Jobs) to Week 5
- Defer BE-013 (Config Management) - use hardcoded config initially
- Focus on core enrollment flow first

---

**Total Backend Timeline: 4-5 weeks** (vs 8 weeks without Cursor)

---

## 3. Architecture Review

### ✅ **Key Simplifications**

1. **Reduced Tables:** 9 → 3 (67% reduction)
2. **No Data Migration:** Just status updates instead of copying data
3. **Reused Tables:** customers, endorsements, nominees, deal_endorsements
4. **JSONB Storage:** Plan selections, nominees, premium breakdown, company config
5. **Simpler Code:** Less transformation logic, fewer repositories
6. **No Separate Config Table:** Rules stored per enrollment window in JSONB

### ⚠️ **Trade-offs**

1. **Production tables contain drafts:** But isolated by `status='pending_approval'`
2. **JSONB queries:** Less structured, but we don't need complex queries on draft data
3. **Careful query filters:** Must always filter by status to avoid draft records

### 🎯 **Benefits**

- **50% faster development** (~8 weeks vs 13 weeks)
- **Easier to maintain** (fewer tables = less complexity)
- **Consistent with existing patterns** (similar to CSV upload flow)
- **Lower database overhead** (fewer joins, simpler schema)

---

## 4. JIRA Tickets - Simplified Backend Implementation

> **Total: 13 tickets | 58 Story Points | 8 weeks**

---

### **BE-001: Database Schema & Migrations**
**Priority:** High | **Story Points:** 5 | **Type:** Backend

Create minimal new tables and modify existing ones.

**Sub-tasks:**
- Create `enrollment_windows` table with JSONB config field (stores all company rules per window)
- Create `enrollment_invitations` table with token tracking
- Create `enrollment_submissions` table with JSONB fields (plan_selections, nominee_data, premium_breakdown)
- Modify `cpc.customers`: Add `status`, `enrollment_window_id`, `enrollment_submission_id`
- Modify `cpc.endorsements`: Add `source`, `enrollment_window_id`, `submission_count`
- Modify `cpc.nominees`: Make `policy_id` nullable, add `submission_id`, add constraint
- Create all indexes for performance
- Create enums: WindowStatus, InvitationStatus, SubmissionStatus
- Write Flyway migrations in order (V1_enrollment_base.sql)
- Test migrations on dev database
- Test rollback procedure

**Acceptance Criteria:**
- All 3 new tables created successfully
- Existing tables modified without data loss
- Foreign keys and constraints working
- Indexes created
- Config JSONB field in enrollment_windows stores and retrieves rules correctly
- Migrations reversible (rollback tested)

**Database Changes:**
```sql
New tables: 3
Modified tables: 3 (customers, endorsements, nominees)
Total complexity: LOW
```

---

### **BE-002: Core Entities & Repositories**
**Priority:** High | **Story Points:** 3 | **Type:** Backend

Create JPA entities for new tables and update existing ones.

**Sub-tasks:**
- Create `EnrollmentWindow` entity with JSONB config mapping (EnrollmentConfig class)
- Create `EnrollmentInvitation` entity
- Create `EnrollmentSubmission` entity with JSONB fields (PlanSelection, NomineeData, PremiumBreakdown classes)
- Update `Deals` (customers) entity: Add status, enrollment fields
- Update `Endorsement` entity: Add source, enrollment fields
- Update `Nominee` entity: Add submissionId, make policyId nullable
- Create repositories with custom query methods:
  - EnrollmentWindowRepository
  - EnrollmentInvitationRepository
  - EnrollmentSubmissionRepository
- Add @Version for optimistic locking on submissions
- Create enum classes: WindowStatus, InvitationStatus, SubmissionStatus
- Create Java classes for JSONB structures: EnrollmentConfig, PlanSelection, NomineeData, PremiumBreakdown
- Configure JSONB type converters (use @JdbcTypeCode(SqlTypes.JSON))
- Test basic CRUD operations
- Test JSONB serialization/deserialization

**Acceptance Criteria:**
- All entities map correctly to database
- JSONB config field in EnrollmentWindow serializes/deserializes to EnrollmentConfig object
- JSONB fields in EnrollmentSubmission handle complex nested structures
- Repositories tested with sample data
- Optimistic locking works (version field)
- No N+1 query issues
- Can query enrollment windows and access config.maxDependents, config.gmcMandatory, etc.

---

### **BE-003: Token Service & Security**
**Priority:** High | **Story Points:** 5 | **Type:** Backend

Implement token generation, validation, and security.

**Sub-tasks:**
- Create `TokenSecurityService`:
  - generateToken() - 32-byte cryptographically secure
  - hashToken() - SHA-256 hashing
  - verifyToken() - constant-time comparison
- Create `EnrollmentTokenService`:
  - validateToken(token) → EnrollmentContext
  - Check expiry, usage, window status
  - Mark invitation as 'opened' on first access
- Create `EnrollmentContext` DTO
- Create custom exceptions: InvalidTokenException, TokenExpiredException, TokenAlreadyUsedException
- Implement rate limiting with Bucket4j (3 req/min per IP)
- Add @RateLimit annotation and aspect
- Generate JWT after token validation
- Add IP address extraction utility
- Write comprehensive unit tests

**Acceptance Criteria:**
- Tokens are 256-bit URL-safe strings
- Raw tokens never stored or logged
- Rate limiting returns 429 Too Many Requests
- All error scenarios tested
- Unit test coverage > 90%

---

### **BE-004: Enrollment Window Management APIs**
**Priority:** High | **Story Points:** 5 | **Type:** Backend

Admin APIs for enrollment window CRUD.

**Sub-tasks:**
- Create `EnrollmentWindowController`:
  - POST /api/admin/enrollment-windows - Create
  - GET /api/admin/enrollment-windows - List with filters
  - GET /api/admin/enrollment-windows/{id} - Details
  - PUT /api/admin/enrollment-windows/{id} - Update
  - POST /api/admin/enrollment-windows/{id}/activate - Manual activate
  - POST /api/admin/enrollment-windows/{id}/close - Close
  - DELETE /api/admin/enrollment-windows/{id} - Soft delete
  - GET /api/admin/enrollment-windows/{id}/stats - Statistics
- Create `EnrollmentWindowService` with business logic
- Validate date ranges, no overlapping windows
- Calculate stats (invited, opened, submitted counts)
- Create DTOs: WindowRequest, WindowResponse, WindowStatsResponse
- Add access control: VIMA_ADMIN all companies, HR_ADMIN own company
- Add pagination and sorting

**Acceptance Criteria:**
- All CRUD operations work
- Stats calculation accurate
- Access control enforced
- Cannot create overlapping windows
- Swagger documentation generated

**API Contract:**
```json
POST /api/admin/enrollment-windows
Request: {
  "organizationId": "uuid",
  "name": "Annual Enrollment 2026",
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "config": {
    "reminderEnabled": true,
    "maxDependents": 7,
    "gmcMandatory": true
  }
}
Response: {id, name, status, dates, stats}
```

---

### **BE-005: Invitation & Email Service APIs (+ Draft Endorsement Creation)**
**Priority:** High | **Story Points:** 8 | **Type:** Backend

Bulk invitation sending with email delivery + **create draft endorsement upfront for tracking**.

**Sub-tasks:**
- Create `EnrollmentInvitationService`:
  - sendInvitation(employeeId, windowId)
  - sendBulkInvitations(employeeIds, windowId)
  - sendReminders(windowId) - Used by scheduled job
  - extendDeadline(invitationId, newDate)
- **Create `EndorsementPreparationService`:** (NEW)
  - **createDraftEndorsement(windowId, employeeIds)**
    - Create endorsement (status='draft', source='self_enrollment', type='ADDITION')
    - Create deal_endorsement entries for each employee (enrollment_status='invited')
    - Link endorsement_id to enrollment_window
    - Return endorsement with member count
  - **getEnrollmentProgress(windowId)**
    - Query deal_endorsements for enrollment_status counts
    - Calculate completion rate
    - Return progress summary
- Create `EnrollmentInvitationController`:
  - POST /api/admin/enrollments/send-invitation - Single invite
  - POST /api/admin/enrollments/send-bulk - Bulk invites (creates endorsement)
  - POST /api/admin/enrollments/send-reminders - Manual reminder trigger
  - POST /api/admin/enrollments/{employeeId}/extend - Extend deadline
  - GET /api/admin/enrollments/invitations - List with status
  - **GET /api/admin/enrollments/windows/{windowId}/progress** - Track completion (NEW)
- Generate unique token using TokenSecurityService
- Store hashed token in invitation
- Integrate with existing email service (SES/SMTP)
- Create email template with magic link
- Update invitation status after send
- Handle email failures with retry mechanism
- Track reminder_count and last_reminder_at
- Validate: no duplicate invitations per window
- Async processing for bulk sends (CompletableFuture or @Async)

**Acceptance Criteria:**
- Single invite completes in < 2 seconds
- Bulk 1000 employees in < 5 minutes
- **Draft endorsement created automatically with all employees**
- **Progress tracking API shows "4/7 completed"**
- Failed emails logged and marked
- Email contains valid magic link
- Async bulk processing works
- Endorsement linked to enrollment window

**API Contract:**
```json
POST /api/admin/enrollments/send-bulk
Request: {
  "windowId": "uuid",
  "employeeIds": ["uuid1", "uuid2", ...]
}
Response: {
  "sent": 950,
  "failed": 50,
  "failedEmployees": [...],
  "endorsementId": "uuid",
  "endorsementStatus": "draft",
  "totalMembers": 950
}

GET /api/admin/enrollments/windows/{windowId}/progress
Response: {
  "windowId": "uuid",
  "endorsementId": "uuid",
  "totalEmployees": 7,
  "invitedCount": 7,
  "openedCount": 5,
  "submittedCount": 4,
  "approvedCount": 0,
  "completionRate": 57.14,
  "employeeDetails": [
    {
      "employeeId": "uuid",
      "name": "John Doe",
      "enrollmentStatus": "submitted",
      "submittedAt": "2026-01-15T10:30:00Z"
    },
    {
      "employeeId": "uuid",
      "name": "Jane Smith",
      "enrollmentStatus": "invited",
      "invitedAt": "2026-01-10T09:00:00Z"
    }
  ]
}
```

---

### **BE-006: Employee Enrollment - Token Validation API**
**Priority:** High | **Story Points:** 4 | **Type:** Backend

Public API for token validation and profile retrieval.

**Sub-tasks:**
- Create `EnrollmentController`:
  - GET /api/enrollment/{token} - Validate and get context
  - GET /api/enrollment/{token}/profile - Employee details
  - GET /api/enrollment/{token}/window-info - Window details
- Validate token using EnrollmentTokenService
- Retrieve employee from cpc.customers
- Create or get draft submission (status='draft')
- Update invitation status to 'opened' on first access
- Calculate days remaining
- Fetch enrollment window with config JSONB (rules, limits from window.config)
- Fetch existing dependents (status='active' from cpc.customers)
- Handle token errors with proper HTTP codes (400, 410, 403, 404)
- Create DTOs: EnrollmentTokenResponse, EmployeeProfileDto, WindowInfoDto, ConfigDto
- Add CORS for employee portal domain

**Acceptance Criteria:**
- Valid token returns complete context
- Expired token returns 410 Gone
- Already completed returns submission summary
- Response time < 500ms
- Existing dependents listed correctly

**API Contract:**
```json
GET /api/enrollment/{token}
Response: {
  "employee": {id, name, email, dob, grade},
  "window": {id, name, startDate, endDate, daysRemaining},
  "submission": {id, status, planSelections, nomineeData} | null,
  "existingDependents": [...],
  "config": {maxDependents, parentCoverageEnabled, gmcMandatory}
}
```

---

### **BE-007: Dependent Management APIs**
**Priority:** High | **Story Points:** 6 | **Type:** Backend

CRUD APIs for managing dependents (direct to cpc.customers).

**Sub-tasks:**
- Add endpoints to `EnrollmentController`:
  - GET /api/enrollment/{token}/dependents - List all
  - POST /api/enrollment/{token}/dependents - Add new
  - PUT /api/enrollment/{token}/dependents/{id} - Update
  - DELETE /api/enrollment/{token}/dependents/{id} - Remove
- Create `EnrollmentDependentService`:
  - Insert dependent directly into cpc.customers with status='pending_approval'
  - Link to primary employee via primary_individual field
  - Validate relationship rules using window.config (maxSpouse, maxChildren, maxParents)
  - Validate age constraints (spouse 18+, children <25, parents 50+)
  - Calculate premium using window.config.premiumRules (JSONB lookup by age/relationship)
  - Calculate employer vs employee share using window.config.parentEmployerSharePercentage
- Link dependent to submission via enrollment_submission_id
- Create DTOs: DependentRequest, DependentResponse
- Handle document upload (integrate with existing Document service)
- Add optimistic locking with @Version
- Validate: cannot modify after submission

**Acceptance Criteria:**
- Dependent inserted into cpc.customers correctly
- Status='pending_approval' isolates from production
- Premium calculation accurate
- Document upload links properly
- Cannot exceed company limits
- Response includes updated total premium

**API Contract:**
```json
POST /api/enrollment/{token}/dependents
Request: {
  "fullName": "Child Name",
  "relationship": "child",
  "dateOfBirth": "2010-05-15",
  "gender": "male"
}
Response: {
  "dependent": {id, fullName, relationship, age},
  "premium": {total, employerShare, employeeShare},
  "totalSubmissionPremium": 15000.00
}
```

---

### **BE-008: Plan Selection & Nominee APIs**
**Priority:** High | **Story Points:** 6 | **Type:** Backend

APIs for plan selection and nominee management.

**Sub-tasks:**
- Add endpoints to `EnrollmentController`:
  - GET /api/enrollment/{token}/plans - Available plans
  - POST /api/enrollment/{token}/plan-selections - Save selections (to JSONB)
  - GET /api/enrollment/{token}/nominees - List nominees
  - POST /api/enrollment/{token}/nominees - Add nominee (to cpc.nominees with submission_id)
  - DELETE /api/enrollment/{token}/nominees/{id} - Remove
- Create `PlanSelectionService`:
  - Fetch plans from window.config (gmcMandatory, gpaMandatory, gtlMandatory, topupEnabled)
  - Calculate coverage based on employee grade (can store grade-to-coverage mapping in window.config)
  - Calculate premiums from window.config.premiumRules
  - Validate mandatory plans cannot be opted out
  - Store selections in submission.plan_selections JSONB
  - Calculate total premium across all plans + dependents
- Create `NomineeService`:
  - Insert into cpc.nominees with submission_id, policy_id=null
  - Validate percentages sum to 100% per plan
  - Link to dependents if applicable
- Create DTOs: PlanDetailsDto, PlanSelectionRequest, NomineeRequest
- Cache plan data with @Cacheable

**Acceptance Criteria:**
- Plan selections stored in JSONB correctly
- Nominees inserted into cpc.nominees with submission_id
- Nominee percentages validated (100% per plan)
- Cannot opt-out of mandatory plans
- Premium calculation accurate
- Response time < 1 second

**API Contract:**
```json
POST /api/enrollment/{token}/plan-selections
Request: {
  "selections": [
    {"planType": "GMC", "opted": true, "coverageAmount": 500000},
    {"planType": "GTL", "opted": true, "coverageAmount": 1500000}
  ]
}
Response: {
  "saved": true,
  "premiumBreakdown": {total, employer, employee}
}

POST /api/enrollment/{token}/nominees
Request: {
  "planType": "GTL",
  "fullName": "Spouse Name",
  "relationship": "Spouse",
  "percentage": 100
}
Response: {id, fullName, percentage}
```

---

### **BE-009: Submission & Declaration API**
**Priority:** High | **Story Points:** 4 | **Type:** Backend

Final submission with validation.

**Sub-tasks:**
- Add endpoints:
  - POST /api/enrollment/{token}/submit - Submit enrollment
  - GET /api/enrollment/{token}/summary - Get summary
- Create `SubmissionService`:
  - Validate all required fields completed
  - Validate declaration acceptance
  - Generate reference number (ENR-YYYY-ORG-XXXXX)
  - Capture IP and timestamp
  - Check idempotency key header
  - Validate nominee percentages (100% per plan)
  - Calculate final premium, store in premium_breakdown JSONB
  - Update submission status: draft → submitted
  - Update invitation status: in_progress → completed
  - Update cpc.customers records: keep status='pending_approval'
  - Send confirmation email with reference number
- Create DTOs: SubmitRequest, SubmissionResponse
- Add transactional boundary
- Prevent edits after submission

**Acceptance Criteria:**
- Cannot submit without declaration
- Duplicate submission returns existing (idempotent)
- Reference number unique and formatted correctly
- Confirmation email sent in < 1 minute
- Returns 409 Conflict if already submitted

**API Contract:**
```json
POST /api/enrollment/{token}/submit
Headers: {Idempotency-Key: "uuid"}
Request: {
  "declarationAccepted": true,
  "declarationTimestamp": "2026-01-15T10:30:00Z"
}
Response: {
  "referenceNumber": "ENR-2026-ACME-00123",
  "submittedAt": "2026-01-15T10:30:00Z",
  "status": "submitted"
}
```

---

### **BE-010: HR Approval & Rejection APIs**
**Priority:** High | **Story Points:** 5 | **Type:** Backend

HR portal for reviewing and approving submissions.

**Sub-tasks:**
- Create `HRApprovalController`:
  - GET /api/hr/enrollments/pending - List pending
  - GET /api/hr/enrollments/{id} - Details
  - POST /api/hr/enrollments/{id}/approve - Approve
  - POST /api/hr/enrollments/{id}/reject - Reject
  - POST /api/hr/enrollments/bulk-approve - Bulk approve
- Create `HRApprovalService`:
  - Approval workflow:
    - Update submission.status = 'approved'
    - Update cpc.customers.status = 'active' (for dependents)
    - Set reviewed_by, reviewed_at
    - Send approval email
  - Rejection workflow:
    - Update submission.status = 'rejected'
    - Delete or mark cpc.customers.status = 'rejected' (for new dependents)
    - Store rejection_reason
    - Reopen invitation if needed
    - Send rejection email
  - Bulk approval with transaction
- Create DTOs: SubmissionDetailDto, ApprovalRequest, RejectionRequest
- Add access control: HR_ADMIN sees only their company
- Add filters and search
- Add pagination

**Acceptance Criteria:**
- HR sees only their company submissions
- Bulk approve processes 100+ in < 30 seconds
- Rejected submissions can be resubmitted
- Approval/rejection transactional
- Emails sent to employee

**API Contract:**
```json
POST /api/hr/enrollments/{id}/approve
Response: {success: true, message: "Approved"}

POST /api/hr/enrollments/{id}/reject
Request: {reason: "Missing documents"}
Response: {success: true, message: "Rejected"}
```

---

### **BE-011: Endorsement Finalization & Approval**
**Priority:** High | **Story Points:** 6 | **Type:** Backend

Finalize draft endorsement after window closes (endorsement already created in BE-005).

> **Note:** Endorsement is created upfront when invitations are sent (BE-005). This ticket handles finalization.

**Sub-tasks:**
- Create `EndorsementFinalizationService`:
  - **finalizeEndorsement(endorsementId, includeIncomplete=false)**
    - Validate: Endorsement exists and status='draft'
    - Validate: All members reviewed (approved/rejected) OR includeIncomplete=true
    - Query all approved submissions for window
    - Update deal_endorsement.enrollment_status = 'finalized' for approved members
    - Remove rejected members from endorsement (delete deal_endorsement entries)
    - Migrate nominees to policies:
      - Find nominees where submission_id = submission.id
      - After policy assignment: Update nominee.policy_id, clear submission_id
    - Update submission status: 'approved' → 'endorsed'
    - Link submission.endorsement_id (if not already linked)
    - Update dependent.status: 'pending_approval' → 'active'
    - Change endorsement.status: 'draft' → 'approved'
    - Update endorsement.updated_at
    - Calculate final premium totals
    - Send notification to VIMA admin + insurer API
  - **getEndorsementSummary(endorsementId)**
    - Return member counts by status
    - Show completion rate
    - List incomplete members
- Add endpoints:
  - POST /api/admin/endorsements/{id}/finalize - Finalize endorsement
  - GET /api/admin/endorsements/{id}/summary - Get finalization summary
  - POST /api/admin/endorsements/{id}/approve - HR final approval
- Add transactional boundary (rollback on any failure)
- Handle edge cases:
  - What if no one completed? (Allow finalization with 0 members)
  - What if some incomplete? (includeIncomplete flag)
- Send batch completion notification

**Acceptance Criteria:**
- Can only finalize endorsements with status='draft'
- All approved members remain in endorsement
- Rejected/incomplete members removed (if includeIncomplete=false)
- Nominees migrated to policies correctly
- Endorsement status changes to 'approved'
- Transactional (all or nothing)
- Completes in < 2 minutes for 500 members
- Notification sent to admins

**API Contract:**
```json
GET /api/admin/endorsements/{endorsementId}/summary
Response: {
  "endorsementId": "uuid",
  "status": "draft",
  "totalMembers": 7,
  "completedMembers": 4,
  "approvedMembers": 3,
  "rejectedMembers": 1,
  "incompleteMembers": 3,
  "readyToFinalize": false,
  "members": [
    {"name": "John", "status": "approved"},
    {"name": "Jane", "status": "submitted"},
    {"name": "Bob", "status": "invited"}
  ]
}

POST /api/admin/endorsements/{endorsementId}/finalize
Request: {
  "includeIncomplete": false,
  "notes": "Finalized after window close"
}
Response: {
  "endorsementId": "uuid",
  "status": "approved",
  "finalMemberCount": 4,
  "removedMembers": 3,
  "totalPremium": 150000,
  "message": "Endorsement finalized successfully"
}
```

---

### **BE-012: Background Jobs & Schedulers**
**Priority:** Medium | **Story Points:** 4 | **Type:** Backend

Scheduled tasks for automation (reminders, expiry, window status updates).

**Sub-tasks:**
- Create `EnrollmentScheduledJobs` with Spring @Scheduled:
  1. **WindowStatusUpdater** (hourly - cron: 0 0 * * * *):
     - Activate windows where status='scheduled' AND start_date <= today
     - Close windows where status='active' AND end_date < today
     - Trigger batch creation if window.config.autoBatchEnabled = true
  2. **ReminderEmailSender** (daily 9 AM - cron: 0 0 9 * * *):
     - Find invitations with status IN (sent, opened, in_progress) AND expires_at > now
     - Check: last_reminder_at + reminderFrequencyDays <= today
     - Send reminder emails to employees
     - Update reminder_count, last_reminder_at
     - Send final reminder 1 day before expiry
  3. **EnrollmentExpirer** (daily midnight - cron: 0 0 0 * * *):
     - Mark invitations as expired where expires_at < now
     - Update employee enrollment_status to 'expired'
     - Notify admins about expired count per window
- Add ShedLock for distributed job locking (prevent concurrent runs)
- Add job execution logging (start time, end time, records processed, errors)
- Add manual trigger endpoints for testing:
  - POST /api/admin/jobs/run/WindowStatusUpdater
  - POST /api/admin/jobs/run/ReminderEmailSender
  - POST /api/admin/jobs/run/EnrollmentExpirer
- Add Micrometer metrics for job monitoring
- Add retry mechanism for failed email sends (exponential backoff)

**Acceptance Criteria:**
- Windows activate/close automatically based on dates
- Reminder emails sent per configured frequency (window.config.reminderFrequencyDays)
- Expired invitations marked correctly each day
- Jobs don't overlap (ShedLock prevents concurrent execution)
- Manual triggers work for testing/recovery
- Job execution logged with details (processed count, failures)
- Failed operations logged and retried

---

### **BE-013: Window Config Management APIs**
**Priority:** Medium | **Story Points:** 3 | **Type:** Backend

APIs to manage enrollment window configuration (JSONB).

**Sub-tasks:**
- Enhance `EnrollmentWindowController` with config-specific endpoints:
  - GET /api/admin/enrollment-windows/{id}/config - Get current config
  - PUT /api/admin/enrollment-windows/{id}/config - Update config
  - GET /api/admin/enrollment-windows/{id}/config/preview - Preview config changes impact
- Create `WindowConfigService`:
  - Update window.config JSONB field
  - Validate config values:
    - maxChildren <= maxDependents
    - parentEmployerSharePercentage between 0-100
    - Premium rules have valid age ranges
  - Create config templates for common scenarios
  - Audit log for config changes (store in window.updated_at, updated_by)
  - Cache window configs in Redis (1-hour TTL, key: window_config:{windowId})
  - Invalidate cache on config updates
- Create DTOs: WindowConfigRequest, WindowConfigResponse, ConfigValidationResponse
- Add validation helper: ConfigValidator with business rules
- Add access control: VIMA_ADMIN can modify all, HR_ADMIN can view only

**Acceptance Criteria:**
- Config updates reflected in window.config JSONB
- Validation prevents invalid configurations
- Cache improves read performance (< 50ms for config lookups)
- Only VIMA_ADMIN can modify configs
- Audit trail captured (updated_at, updated_by fields)
- Config changes don't affect already-submitted enrollments (immutable after window closes)

**API Contract:**
```json
GET /api/admin/enrollment-windows/{id}/config
Response: {
  "maxDependents": 7,
  "parentCoverageEnabled": true,
  "gmcMandatory": true,
  "premiumRules": {
    "employee": {"age": "18-30", "premium": 5000},
    "spouse": {"age": "18-30", "premium": 4000}
  }
}

PUT /api/admin/enrollment-windows/{id}/config
Request: {config object}
Response: {success: true, updatedConfig: {...}}
```

---

### **BE-014: Testing, Monitoring & Documentation**
**Priority:** Medium | **Story Points:** 5 | **Type:** Backend

Comprehensive testing and observability.

**Sub-tasks:**
- **Unit Tests (JUnit 5 + Mockito):**
  - Token generation, hashing, validation
  - Premium calculation from JSONB rules
  - Status transitions
  - Access control
- **Integration Tests:**
  - Full enrollment flow: token → dependent → submission → approval
  - Batch creation → endorsement
  - Concurrent submissions (optimistic locking)
- **Performance Tests (JMeter):**
  - Bulk invitation: 1000 employees < 5 min
  - Concurrent enrollments: 100 users
  - Batch creation: 500 submissions < 2 min
- **API Documentation (Swagger):**
  - Configure Springdoc OpenAPI
  - Add annotations to all controllers
  - Document request/response models
  - Export OpenAPI spec
- **Monitoring (Actuator + Micrometer):**
  - Custom metrics (completion rate, approval time)
  - Correlation ID logging
  - Health checks
  - Prometheus export
- **Error Handling:**
  - Global exception handler
  - Standard error format
  - Sensitive data masking

**Acceptance Criteria:**
- Unit test coverage > 85%
- Integration tests cover all flows
- Performance tests pass SLAs
- Swagger UI accessible
- Monitoring dashboards show metrics

---

## 5. Backend Deployment Checklist

**Pre-Production (Staging):**
- [ ] Run Flyway migrations on staging
- [ ] Verify 3 new tables + 3 modified tables
- [ ] Load test APIs (1000 req/min)
- [ ] Test bulk invitation (1000 employees)
- [ ] Verify email delivery
- [ ] Test JWT token flow
- [ ] Smoke test all endpoints
- [ ] Verify multi-tenant access control
- [ ] Test scheduled jobs manually
- [ ] Check database performance

**Production Rollout:**
- [ ] **Phase 1 - Database Migration:**
  - Schedule maintenance window
  - Backup database
  - Run migrations: `flyway migrate`
  - Verify: `flyway info`
- [ ] **Phase 2 - Backend Deployment:**
  - Deploy with feature disabled (if using feature flags)
  - Verify `/actuator/health` returns UP
  - Check application logs
- [ ] **Phase 3 - Smoke Testing:**
  - Test admin API (GET windows)
  - Verify no errors in logs
- [ ] **Phase 4 - Gradual Rollout:**
  - Enable for one pilot company
  - Send test invitations to 5-10 employees
  - Monitor error rates and logs
  - Verify end-to-end flow
  - Enable for remaining companies (10% per day)

**Monitoring:**
- [ ] Set up dashboards (response times, error rates)
- [ ] Configure alerts (error rate > 5%, response time > 3s)
- [ ] Monitor enrollment metrics

**Post-Deployment:**
- [ ] Monitor completion rates
- [ ] Check for slow queries
- [ ] Optimize indexes based on usage
- [ ] Collect feedback
- [ ] Document issues and fixes

**Frontend Integration (After Backend Stable):**
- [ ] Share OpenAPI spec with frontend
- [ ] Set up CORS for frontend domain
- [ ] Test integration on staging
- [ ] Coordinate deployment

---

## Summary: Updated Story Points & Timeline

### **Ticket Story Points (After Workflow Change)**

| Ticket | Description | Original SP | Updated SP | Change | Reason |
|--------|-------------|-------------|------------|--------|--------|
| BE-001 | Database Schema & Migrations | 5 | 5 | - | Minor schema additions |
| BE-002 | Core Entities & Repositories | 3 | 3 | - | Entity mapping updates |
| BE-003 | Token Security | 5 | 5 | - | No change |
| BE-004 | Enrollment Window APIs | 5 | 5 | - | No change |
| **BE-005** | **Invitation + Endorsement Creation** | **6** | **8** | **+2** | **Now creates draft endorsement** |
| BE-006 | Token Validation API | 4 | 4 | - | No change |
| BE-007 | Dependent Management | 6 | 6 | - | Status tracking (minor) |
| BE-008 | Plan & Nominee APIs | 6 | 6 | - | No change |
| BE-009 | Submission API | 4 | 4 | - | Status tracking (minor) |
| BE-010 | HR Approval APIs | 5 | 5 | - | Status tracking (minor) |
| **BE-011** | **Endorsement Finalization** | **6** | **6** | **0** | **Different work, same effort** |
| BE-012 | Background Jobs | 4 | 4 | - | No change |
| BE-013 | Config Management | 3 | 3 | - | No change |
| BE-014 | Testing & Documentation | 5 | 5 | - | No change |
| **Total** | | **57 SP** | **59 SP** | **+2 SP** | **~0.25 weeks added** |

### **Updated Timeline with Cursor AI**

| Week | Dev1 Tasks | Dev2 Tasks | SP per Dev |
|------|-----------|-----------|------------|
| **Week 1** | Foundation (both) | Foundation (both) | 13 SP (shared) |
| **Week 2** | BE-004 (5), BE-005 (8), BE-013 (3) | BE-006 (4), BE-007 (6), BE-008 (start, 3) | 16 SP / 13 SP |
| **Week 3** | BE-010 (5), BE-011 (6) | BE-008 (3), BE-009 (4), BE-014 (2) | 11 SP / 9 SP |
| **Week 4** | BE-012 (4), BE-014 (3) | BE-014 (testing, 3) | 7 SP / 3 SP |
| **Week 5** | Polish, deployment | Polish, deployment | Buffer |
| **Total** | ~30 SP | ~29 SP | **59 SP** |

### **Timeline Comparison**

| Approach | Team | Duration | Total SP |
|----------|------|----------|----------|
| Original Complex Plan | 1 dev | 13 weeks | 94 SP |
| Simplified (No Cursor) | 1 dev | 8 weeks | 57 SP |
| **With Workflow Change** | **1 dev** | **~8.5 weeks** | **59 SP** |
| **With Cursor AI (2 devs)** | **2 devs** | **~5 weeks** | **59 SP** ✅ |

**Impact of Workflow Change:** +2 SP (~0.25 weeks) - Minimal impact

### **Key Workflow Changes**

**Before:**
```
Window Created → Send Invites → Employees Enroll → HR Approves 
→ Window Closes → CREATE Endorsement
```

**After (New):**
```
Window Created → Send Invites + CREATE DRAFT ENDORSEMENT 
→ Track Progress (4/7 completed) → Employees Enroll 
→ HR Approves → Window Closes → FINALIZE Endorsement
```

**Benefits:**
- ✅ Real-time progress tracking ("4 out of 7 completed")
- ✅ Single endorsement from start to finish
- ✅ Better visibility for HR (see who's incomplete)
- ✅ Cleaner data model
- ✅ Follows existing CSV upload pattern

**Cost:** +2 story points (~0.25 weeks)

---

**Team Allocation:**
- 2 backend developers (can work in parallel after BE-002)
- 1 QA engineer (from week 4)
- 1 DevOps for deployment

**Critical Path:**
1. Database foundation (blocking all)
2. Token security (blocking enrollment APIs)
3. **BE-005 (Endorsement creation)** - blocking progress tracking
4. Employee APIs (blocking HR approval)
5. **BE-011 (Finalization)** - blocking production use

**Additional Documentation:**
- See `WORKFLOW_CHANGE_SUMMARY.md` for detailed workflow change analysis

---

*Last Updated: February 4 2026*
*Document Owner: Engineering Team*
