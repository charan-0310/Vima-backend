# JIRA Ticket Changes Summary - Self-Enrollment Workflow Update

> **Date:** February 5, 2026  
> **Reason:** Workflow changes to improve HR/Admin separation and endorsement timing  
> **Impact:** +3 Story Points (~0.5 weeks), Minimal timeline impact

---

## Overview of Workflow Changes

### **Key Changes:**
1. **Endorsement created AFTER HR reviews** (not upfront during invitation)
2. **Window has 5 statuses** (not 3): scheduled → active → pending_review → approved → closed
3. **Window activation** triggers invitation sending (2-step process)
4. **Resend activation link** feature for individual employees
5. **Endorsement source tracking** with windowName in source_metadata JSONB field
6. **HR can edit on behalf** of employee or remove employee
7. **Clear role separation**: HR manages enrollments, Admin manages endorsements

---

## Affected JIRA Tickets

### ✅ **No Changes Needed**
- **BE-001**: Database Schema - Only minor additions (source_metadata JSONB)
- **BE-002**: Entities & Repositories - Small entity updates
- **BE-003**: Token Security - No change
- **BE-006**: Token Validation API - No change
- **BE-007**: Dependent Management - No change
- **BE-008**: Plan & Nominee APIs - No change
- **BE-009**: Submission API - No change
- **BE-012**: Background Jobs - No change
- **BE-013**: Config Management - No change
- **BE-014**: Testing & Documentation - No change

---

## 📝 Tickets Requiring Updates

### **BE-001: Database Schema & Migrations**
**Story Points:** 5 → 5 (No change)

#### Changes to Ticket:
**Add to Sub-tasks:**
```sql
-- Add to endorsements table modification:
ALTER TABLE cpc.endorsements
  ADD COLUMN IF NOT EXISTS source_metadata JSONB DEFAULT '{}';
  
-- Example structure:
{
  "windowId": "uuid",
  "windowName": "FY 2025-26 Annual",
  "enrollmentStartDate": "2026-03-01",
  "enrollmentEndDate": "2026-03-31"
}

-- Update enrollment_windows status enum:
-- Values: scheduled, active, pending_review, approved, closed, cancelled
```

**Acceptance Criteria Updates:**
- Add: "source_metadata JSONB field stores window name and details for self_enrollment source"
- Add: "Window status supports 5 states: scheduled, active, pending_review, approved, closed"

---

### **BE-004: Enrollment Window Management APIs**
**Story Points:** 5 → 6 (+1)

#### Changes to Ticket:
**Add to Sub-tasks:**
- Create window activation endpoint:
  - POST /api/admin/enrollment-windows/{id}/activate - Activate window (status: scheduled → active)
  - Validate window hasn't started yet
  - Trigger invitation sending process
  - Return activation status and email send results

**Update API Contracts:**
```json
POST /api/admin/enrollment-windows/{id}/activate
Response: {
  "windowId": "uuid",
  "status": "active",
  "invitationsSent": 498,
  "invitationsFailed": 2,
  "failedEmployees": [...]
}
```

**Acceptance Criteria Updates:**
- Add: "Window can be manually activated before start_date"
- Add: "Activation triggers bulk invitation sending"
- Add: "Window status transitions: scheduled → active"

---

### **BE-005: Invitation & Email Service APIs**
**Story Points:** 8 → 6 (-2, complexity reduced)

#### Changes to Ticket:
**Title Change:** 
- FROM: "Invitation & Email Service APIs (+ Draft Endorsement Creation)"
- TO: "Invitation & Email Service APIs + Window Activation"

**REMOVE from Sub-tasks:**
- ❌ Create `EndorsementPreparationService`
- ❌ createDraftEndorsement(windowId, employeeIds)
- ❌ Create deal_endorsement entries
- ❌ Link endorsement_id to enrollment_window
- ❌ Endorsement-related code

**ADD to Sub-tasks:**
- ✅ resendActivationLink(invitationId) - Resend individual magic link
- ✅ Window activation triggers bulk send
- ✅ Update window status after activation
- ✅ getEnrollmentProgress() - Track without endorsement reference

**Update API Contracts:**
```json
// REMOVE this endpoint (no longer creating endorsement):
POST /api/admin/enrollments/send-bulk

// ADD these endpoints:
POST /api/admin/enrollments/windows/{windowId}/activate
Response: {
  "windowStatus": "active",
  "sent": 950,
  "failed": 50,
  "failedEmployees": [...]
}

POST /api/admin/enrollments/{invitationId}/resend
Response: {
  "sent": true,
  "email": "employee@company.com"
}

// UPDATE this endpoint (remove endorsement references):
GET /api/admin/enrollments/windows/{windowId}/progress
Response: {
  "windowId": "uuid",
  "windowStatus": "active",
  "totalEmployees": 7,
  "invitedCount": 7,
  "submittedCount": 4,
  "approvedCount": 2,
  "reviewedCount": 6,
  // NO endorsementId field
}
```

**Acceptance Criteria Updates:**
- REMOVE: "Draft endorsement created automatically"
- REMOVE: "Endorsement linked to enrollment window"
- ADD: "Window activation triggers invitation sending"
- ADD: "Individual activation links can be resent"
- ADD: "Progress tracking works without endorsement"

---

### **BE-010: HR Approval & Rejection APIs**
**Story Points:** 5 → 6 (+1)

#### Changes to Ticket:
**Add to Sub-tasks:**
- POST /api/hr/enrollments/{submissionId}/edit-on-behalf - HR edits submission for employee
  - Update dependent details
  - Update plan selections
  - Update nominee data
  - Maintain audit trail (edited_by, edited_at)
- DELETE /api/hr/enrollments/{submissionId}/remove - Remove employee from window
  - Mark submission as 'removed'
  - Exclude from endorsement creation
  - Send notification to employee
- Validate all submissions reviewed before allowing endorsement creation
- Track review progress (approved, rejected, removed counts)

**Update API Contracts:**
```json
POST /api/hr/enrollments/{submissionId}/edit-on-behalf
Request: {
  "dependents": [...],
  "planSelections": [...],
  "nominees": {...}
}
Response: {
  "submissionId": "uuid",
  "status": "approved",
  "editedBy": "HR Admin Name",
  "editedAt": "2026-01-20T14:30:00Z"
}

DELETE /api/hr/enrollments/{submissionId}/remove
Request: {
  "reason": "Employee not eligible"
}
Response: {
  "submissionId": "uuid",
  "status": "removed",
  "removedBy": "HR Admin Name"
}
```

**Acceptance Criteria Updates:**
- ADD: "HR can edit submissions on behalf of employees"
- ADD: "HR can remove employees from enrollment window"
- ADD: "Audit trail captured for HR edits"
- ADD: "Removed employees excluded from endorsement creation"

---

### **BE-011: Endorsement Creation from Completed Reviews**
**Story Points:** 6 → 7 (+1)

#### Changes to Ticket:
**Title Change:**
- FROM: "Endorsement Finalization & Approval"
- TO: "Endorsement Creation from Completed Reviews"

**Description Change:**
- FROM: "Finalize draft endorsement after window closes (endorsement already created in BE-005)"
- TO: "Create endorsement AFTER all HR reviews are complete (not upfront)"

**REPLACE Sub-tasks:**

**REMOVE:**
- ❌ finalizeEndorsement() - No longer applicable
- ❌ Update existing draft endorsement
- ❌ Update deal_endorsement.enrollment_status

**ADD:**
- ✅ createEndorsementFromWindow(windowId, windowName)
  - Validate all submissions reviewed (approved/rejected/removed)
  - Create NEW endorsement (not update existing)
  - Set source='self_enrollment'
  - Set source_metadata JSONB:
    ```json
    {
      "windowId": "uuid",
      "windowName": "FY 2025-26 Annual",
      "enrollmentStartDate": "2026-03-01",
      "enrollmentEndDate": "2026-03-31",
      "totalInvited": 500,
      "totalApproved": 450
    }
    ```
  - Create deal_endorsement entries for approved members
  - Link submission.endorsement_id
  - Update window status: pending_review → approved
  - Send notification to Admin portal (not insurer)
- ✅ getWindowReviewSummary(windowId)
  - Show counts: approved, rejected, removed, pending
  - Check if ready to create endorsement
- ✅ POST /api/hr/enrollments/windows/{windowId}/create-endorsement
- ✅ GET /api/hr/enrollments/windows/{windowId}/review-summary

**Update API Contracts:**
```json
GET /api/hr/enrollments/windows/{windowId}/review-summary
Response: {
  "windowId": "uuid",
  "windowName": "FY 2025-26 Annual",
  "totalEmployees": 500,
  "approvedCount": 450,
  "rejectedCount": 20,
  "removedCount": 20,
  "editedByHrCount": 30,
  "pendingReviewCount": 10,
  "readyToCreateEndorsement": false,
  "message": "10 submissions still need review"
}

POST /api/hr/enrollments/windows/{windowId}/create-endorsement
Request: {
  "windowName": "FY 2025-26 Annual",
  "notes": "Annual enrollment completed"
}
Response: {
  "endorsementId": "uuid",
  "status": "draft",
  "source": "self_enrollment",
  "sourceMetadata": {
    "windowId": "uuid",
    "windowName": "FY 2025-26 Annual",
    "enrollmentStartDate": "2026-03-01",
    "enrollmentEndDate": "2026-03-31"
  },
  "totalMembers": 450,
  "totalDependents": 1200,
  "totalPremium": 7200000,
  "message": "Endorsement created successfully. View in Admin > Endorsements."
}
```

**Acceptance Criteria Updates:**
- REMOVE: "Can only finalize endorsements with status='draft'"
- REMOVE: "Endorsement status changes from draft to approved"
- ADD: "Can only create endorsement after all reviews complete"
- ADD: "Endorsement created with source='self_enrollment'"
- ADD: "source_metadata stores windowName and window details"
- ADD: "Endorsement appears in Admin > Endorsements page"
- ADD: "Source displayed as 'Self-Enrollment - [Window Name]'"
- ADD: "Window status transitions: pending_review → approved"
- ADD: "Notification sent to Admin portal"

---

## Summary of Story Point Changes

| Ticket | Old SP | New SP | Change | Impact |
|--------|--------|--------|--------|--------|
| BE-001 | 5 | 5 | 0 | Minor JSONB field addition |
| BE-004 | 5 | 6 | +1 | Window activation endpoint |
| BE-005 | 8 | 6 | -2 | Removed endorsement creation |
| BE-010 | 5 | 6 | +1 | Edit-on-behalf, remove APIs |
| BE-011 | 6 | 7 | +1 | Create endorsement (not finalize) |
| **Total** | **57** | **60** | **+3** | **~0.5 weeks** |

---

## Workflow Comparison

### **OLD Workflow:**
```
1. HR creates window (scheduled)
2. HR uploads CSV
3. System creates DRAFT endorsement immediately
4. System sends magic links
5. Employees enroll
6. HR approves submissions → Updates endorsement
7. Window closes
8. HR finalizes endorsement → Changes status to approved
```

### **NEW Workflow:**
```
1. HR creates window (scheduled)
2. HR uploads CSV
3. HR activates window → System sends magic links (active)
4. Employees enroll
5. HR reviews ALL submissions:
   - Approve as-is
   - Edit on behalf of employee
   - Remove employee
6. HR creates endorsement → System creates NEW endorsement (pending_review → approved)
7. Endorsement appears in Admin > Endorsements page
8. Window auto-closes on end_date (closed)
```

---

## Benefits of New Workflow

1. **Clear Role Separation**
   - HR deals with enrollment management (HR page)
   - Admin deals with endorsement processing (Endorsements page)

2. **Better Control**
   - HR completes ALL reviews before endorsement creation
   - No partial/draft endorsements cluttering the system
   - HR can edit or remove employees easily

3. **Source Tracking**
   - Endorsements show source: "Self-Enrollment - FY 2025-26 Annual"
   - Easy to identify which endorsements came from which enrollment windows
   - Supports 4 source types: csv_upload, self_enrollment, hrms_sync, manual

4. **Window Lifecycle**
   - 5 clear statuses: scheduled → active → pending_review → approved → closed
   - Each status has a clear purpose and transition trigger

---

## Testing Impact

### New Test Cases to Add:

**BE-004:**
- Test window activation before start_date
- Test window activation after already activated (should fail)
- Test activation triggers invitation sending

**BE-005:**
- Test resend activation link for single employee
- Test progress tracking without endorsement reference

**BE-010:**
- Test HR edit-on-behalf flow
- Test HR remove employee flow
- Test audit trail for HR edits

**BE-011:**
- Test endorsement creation only after all reviews
- Test source_metadata JSONB structure
- Test window name stored correctly
- Test endorsement appears in Admin page with correct source

---

## Documentation Updates Completed

✅ **Updated:**
- `self-enrollment-functional-guide.md` - All workflow diagrams updated
- `enrollment-engineering-architecture.md` - All ticket definitions updated

---

## Action Items for Team

### For Product Manager:
- [ ] Review and approve workflow changes
- [ ] Update JIRA tickets with new descriptions
- [ ] Update acceptance criteria in JIRA
- [ ] Communicate changes to stakeholders

### For Developers:
- [ ] Review updated ticket details (BE-004, BE-005, BE-010, BE-011)
- [ ] Update technical implementation approach
- [ ] Plan for source_metadata JSONB structure
- [ ] Update API documentation

### For QA:
- [ ] Review new test cases
- [ ] Update test plans for affected tickets
- [ ] Plan for window lifecycle testing
- [ ] Plan for source tracking verification

---

## Questions & Clarifications

### Q: What happens to employees who didn't complete enrollment?
**A:** HR has 3 options:
1. Edit on behalf of employee (complete their submission)
2. Remove employee from window (they won't be in endorsement)
3. Wait and let them be excluded (same as option 2)

### Q: Can HR create endorsement with incomplete reviews?
**A:** No. System blocks endorsement creation until ALL employees are either approved, rejected, or removed.

### Q: Where does the endorsement appear after creation?
**A:** Admin > Endorsements page, with source showing "Self-Enrollment - [Window Name]"

### Q: What happens to the enrollment window after endorsement is created?
**A:** 
- Window status changes to 'approved'
- Window auto-closes on end_date
- Window data remains for audit purposes

### Q: Can we have multiple endorsements from one window?
**A:** No. One enrollment window creates one endorsement. If needed, create a new window for additional enrollments.

---

**Document Version:** 1.0  
**Last Updated:** February 5, 2026  
**Status:** Ready for Implementation
