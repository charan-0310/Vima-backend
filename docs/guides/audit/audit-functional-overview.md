# Vima Audit Trail — What Are We Tracking?

> Client-facing overview of the audit system being implemented on the Vima platform.

---

## What Is This?

Every time a critical record is created, updated, or deleted in Vima, the system will automatically record:

- **Who** made the change (user name, email, role)
- **What** changed (which record, which fields, old value vs new value)
- **When** it happened (exact timestamp)
- **How** it happened (manual action, bulk upload, system process, or employee self-enrollment)
- **Request trace** (correlation ID for debugging, IP address)

These audit records are **immutable** — they cannot be edited or deleted by anyone, including admins.

---

## What's Being Audited

### 1. Employee / Member Data

**Table:** `customers` (individuals, employees, dependents)

| What we track | Example |
|--------------|---------|
| Employee added to organization | "John Doe added to Acme Corp" |
| Personal details updated | Name, email, phone, city, gender changed — old and new values captured |
| Employee status changes | Active → Inactive, enrollment status changes |
| Dependent linked/unlinked | Dependent linked to primary member |
| Employee exit recorded | Date of exit, reason for exit |
| Department / designation changes | Transferred from Sales to Marketing |

**Excluded from audit (security):** Password hashes, Aadhaar numbers

---

### 2. Organization / Company Data

**Table:** `organizations`

| What we track | Example |
|--------------|---------|
| Organization created | "Acme Corp created by admin@vima.com" |
| Company details updated | Name, address, contact person, phone, email — old and new values |
| Status changes | Active → Inactive |
| GSTIN / PAN updates | Business registration changes tracked |

---

### 3. Policies

**Table:** `policies`, `motor_policy_details`

| What we track | Example |
|--------------|---------|
| Policy created | New policy issued for an organization |
| Premium changes | Total premium changed from 50,000 to 55,000 |
| Sum insured changes | Coverage amount increased/decreased |
| Coverage updates | Covered individuals added or removed |
| Status changes | Draft → Active → Expired → Renewed |
| Policy dates modified | Start date, end date, renewal date changes |
| Motor vehicle details | Vehicle registration, make, model, IDV value changes |

---

### 4. Endorsements

**Tables:** `endorsements`, `deal_endorsements`

| What we track | Example |
|--------------|---------|
| Endorsement created | "Addition endorsement created for Acme Corp by admin@vima.com" |
| Status changes | Pending → Approved → Submitted to Insurer → Confirmed |
| Approval actions | Who approved, when, confirmation method |
| Premium impact | Premium change type and amount recorded |
| Insurer reference | Reference number from insurer captured |
| Employee-endorsement links | Which employees were part of which endorsement |
| Bulk uploads | All employees in a bulk upload tracked under one batch |

---

### 5. Enrollment Windows

**Table:** `enrollment_windows`

| What we track | Example |
|--------------|---------|
| Window created | "Open Enrollment 2026 created for Acme Corp" |
| Schedule changes | Start date, end date modified |
| Status changes | Scheduled → Active → Closed |
| Configuration changes | Window settings/rules modified |
| Window closed | Who closed it and when |

---

### 6. Enrollment Invitations

**Table:** `enrollment_invitations`

| What we track | Example |
|--------------|---------|
| Invitation sent | Invitation sent to employee for enrollment window |
| Invitation opened | Employee opened the enrollment link |
| Invitation completed | Employee completed enrollment |
| Invitation expired | Link expired without action |
| Reminders sent | Reminder count and last reminder timestamp |

**Excluded from audit (security):** Token hashes

---

### 7. Enrollment Submissions

**Table:** `enrollment_submissions`

| What we track | Example |
|--------------|---------|
| Submission created | Employee started enrollment form |
| Stage progression | Stage changes through the enrollment flow |
| Plan selections | Which insurance plans the employee selected |
| Premium breakdown | Calculated premium details |
| Submission completed | Employee submitted final enrollment |
| Review actions | Reviewed by whom, when, approval or rejection |
| Rejection reasons | Why a submission was rejected |
| Declaration | Whether employee accepted declaration, timestamp, IP address |

**Excluded from audit (PII):** Personal details JSONB, dependents JSONB, nominee data JSONB

---

### 8. Nominees

**Table:** `nominees`

| What we track | Example |
|--------------|---------|
| Nominee added | "Spouse Jane Doe added as 50% nominee" |
| Nominee updated | Name, relationship, percentage changed |
| Nominee deactivated | Nominee marked inactive |

---

## What's NOT Being Audited (Phase 1)

These are lower-risk or configuration tables that don't need audit tracking yet:

| Area | Reason |
|------|--------|
| Admin user accounts | Phase 2 — login/logout and role change tracking |
| Feature flags | Configuration data, low compliance risk |
| Quotes | Read-heavy, informational, not binding |
| Vendor integrations | Internal API configuration |
| Sales incentives & targets | Internal sales operations |
| Insurance provider master data | Rarely changes |
| Documents table | Phase 2 — document upload/deletion tracking |

---

## How It Works (Non-Technical)

```
User performs an action (e.g., updates a policy premium)
        │
        ▼
System saves the change to the main database
        │
        ▼
Audit system automatically captures:
  ✓ Full snapshot of the record BEFORE the change
  ✓ Full snapshot of the record AFTER the change
  ✓ Who made the change
  ✓ Timestamp
  ✓ Source (manual / bulk upload / enrollment / system)
        │
        ▼
Audit record stored in separate audit tables
(immutable — cannot be modified or deleted)
```

- **No manual logging required** — the system captures everything automatically
- **No performance impact** — audit writes happen within the same database transaction
- **Bulk operations** — a bulk upload of 1000 employees is tracked as one batch event

---

## Audit Data Retention

- All audit data is retained **indefinitely**
- No records are hard-deleted from the Vima platform — all deletions are soft deletes
- Audit entries persist even if the parent record is deactivated

---

## What Questions Can the Audit Trail Answer?

| Question | How |
|----------|-----|
| "Who changed this employee's status?" | Look up audit history for that employee ID |
| "What did this policy look like 3 months ago?" | Retrieve the policy snapshot at that point in time |
| "Who approved this endorsement?" | Check the endorsement audit trail for the approval revision |
| "How many times was this premium changed?" | Count revisions for that policy with premium_amount changes |
| "Was this enrollment submission tampered with after submission?" | Compare all revision snapshots for that submission |
| "Which admin made changes on a specific date?" | Query all revisions by user and date range |
| "What was the original data before this bulk upload?" | Retrieve the pre-revision snapshot for all affected records |

---

## Summary — Audit Coverage

| Area | Tables | Tracked Actions |
|------|--------|----------------|
| **Employees & Dependents** | 1 | Create, update, status change, exit, department transfer |
| **Organizations** | 1 | Create, update, status change |
| **Policies** | 2 | Create, update, premium/coverage changes, status lifecycle |
| **Endorsements** | 2 | Create, approve, reject, submit to insurer, link employees |
| **Enrollment** | 4 | Window lifecycle, invitations, submissions, nominees |
| **Total** | **10 tables** | All create/update/delete operations |
