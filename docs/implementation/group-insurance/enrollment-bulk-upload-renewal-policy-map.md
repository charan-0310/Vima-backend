# Enrollment Bulk Upload: Renewal and Policy Map Rule

> **Purpose:** Document the change that allows re-uploading existing employees for **renewal** (new policy). Previously, upload rejected any employee who already existed in `cpc.customers`. Now we allow existing customers and validate via `cpc.employee_policy_map`: no new policy → "Employee already exists."; new policy start must be after current policy end, otherwise "cannot be in two policies." This is the base for later bulk-upload flows where we will ask "which policy are you uploading this for."

---

## 1. Context

- **Table:** `cpc.customers` (entity: `Deals`) holds employees and dependents. If an employee exists, they also have row(s) in `cpc.employee_policy_map` (entity: `EmployeePolicyMap`) with `effective_from` and `effective_to` (end date).
- **Flow:** Self-enrollment bulk upload is `POST /api/v1/admin/enrollment-windows/{id}/upload` (multipart: file + employee list). Implemented in `EnrollmentWindowServiceImpl.uploadEmployees()`.
- **Previous behaviour:** Validation rejected the upload if employee ID or email already existed in `cpc.customers` for the org ("Employee X already exists in the organization" / "Email X already exists"). So renewal uploads (same employees, new policy) were blocked.
- **New behaviour:** We no longer reject purely because the employee exists in customers. We allow existing customers and apply a **policy-map date rule** so that renewal (new policy start after current policy end) is allowed; otherwise we reject with a clear message.

---

## 2. Rules (current implementation)

| Scenario | Behaviour |
|----------|-----------|
| Employee **not** in `cpc.customers` | Create new row (unchanged). |
| Employee **in** customers | Do not reject for "already exists" in DB. Run policy-map check. |
| No **new policy** for the company | Reject with **"Employee already exists."** (Same wording as before for this case.) |
| New policy exists, **new policy start date > existing policy end date** | Allow (renewal). |
| New policy exists, **new policy start date ≤ existing policy end** (or existing has no end) | Reject: **"Employee X cannot be in two policies; new policy start must be after current policy end."** |

**Customer upsert:** When processing the upload, for each employee row we look up `Deals` by `employee_number` + `organization_id` + `relationship = SELF`. If found → **update** (name, email, DOB, enrollment window); if not found → **insert** new `Deals`. No duplicate customers for the same employee + org.

---

## 3. Where it is implemented

### 3.1 Backend

| Component | File | Change |
|-----------|------|--------|
| Validation (no "already exists" from Deals) | `EnrollmentWindowServiceImpl` | `validateSelfEmployeeEnrollmentRequest()` no longer queries Deals for existing employee IDs/emails or adds "already exists in the organization" / "email already exists". Only DOB-in-future and other basic checks remain. |
| Renewal validation (policy map + new policy date) | `EnrollmentWindowServiceImpl` | New private method `validateExistingEmployeesForRenewal(window, organizationId, requestDtos)`: for each **distinct** employee ID in the request, if that employee exists in Deals, then (1) resolve **new policy start date** for the org; (2) if no new policy → add "Employee already exists."; (3) if new policy exists, load ACTIVE `employee_policy_map` rows for that individual + org, take latest `effective_to`; (4) if `new_policy_start` is not after that end (or `effective_to` is null) → add "Employee X cannot be in two policies; new policy start must be after current policy end." Called from `uploadEmployees()` after `validateSelfEmployeeEnrollmentRequest()` and before `createSelfEmployee()`. |
| New policy start date | `EnrollmentWindowServiceImpl` | Private method `getNewPolicyStartDateForOrganization(organizationId)`: loads org's policies with `PolicyStatus.ACTIVE` and `appliesToEmployees == true`, returns `Optional.of(earliest policy start date)` or `Optional.empty()` if none. |
| Customer upsert | `EnrollmentWindowServiceImpl` | `createSelfEmployee()`: for each request row, `findByEmployeeNumberAndOrganizationIdAndRelationship(employeeId, orgId, "SELF")`; if present → update that Deals (name, email, DOB, enrollmentWindow, updatedAt); else → create new Deals and save. Batched save unchanged. |
| Policy map by individual + org | `IEmployeePolicyMapRepository` | New method: `findByIndividualIdAndOrganizationIdAndStatus(UUID individualId, UUID organizationId, String status)` to fetch ACTIVE mappings for an employee in an org (used to compute latest `effective_to`). |

### 3.2 Dependencies

- `EnrollmentWindowServiceImpl` now injects `IEmployeePolicyMapRepository` and `IPolicyRepository` (and uses `Policy`, `PolicyStatus`, `EmployeePolicyMap`).

### 3.3 Frontend

- No change required: the frontend already shows backend validation errors as returned. There is no client-side check that blocks upload solely because "employee already exists in org."

---

## 4. "New policy" for the company (current definition)

Today the enrollment window does **not** store a selected policy ID. "New policy" is derived as:

- All policies for the organization with status **ACTIVE** and **appliesToEmployees = true**.
- **New policy start date** = earliest `start_date` among those policies.

So if the company has one or more such policies, we use that earliest start date for the rule "new policy start must be after current policy end." If the company has no such policy, we treat it as "no new policy" and reject existing employees with "Employee already exists."

**Planned evolution:** Later, bulk upload (and possibly enrollment window creation) will ask **which policy** the upload is for. When that is in place, "new policy start date" can come from the selected policy instead of the earliest applicable policy.

---

## 5. Example (renewal)

- Current GMC for the org ends **31 Mar 2027** (`effective_to` on `employee_policy_map`).
- New GMC (new insurer) starts **1 Apr 2027**.
- HR uploads the same employees to the new enrollment window.
- **Before:** Upload failed with "Employee X already exists in the organization."
- **After:** Org has an applicable (ACTIVE, appliesToEmployees) policy with start date e.g. 1 Apr 2027; 1 Apr 2027 is after 31 Mar 2027 → validation passes; customers are updated and linked to the window (no duplicate rows).

---

## 6. Related

- [Bulk Upload & Relationship Identity](bulk-upload-and-relationship-identity.md) — group insurance bulk upload (endorsement) and identity by employee number + relationship.
- Plan: enrollment upload allow renewal (customer + policy-map date rule).

## 7. Enrollment bulk upload: 3 mandatory fields, dependents, prefill (VIMA-428, VIMA-429)

- **Mandatory fields:** Employee (SELF) rows require only **employeeId, name, email** (3 fields). `dateOfBirth` is optional in `SelfEmployeeEnrollmentRequestDto` and in validation/upsert.
- **Option A (file as source of truth):** Backend parses the uploaded CSV/Excel in `uploadEmployees()`. If the file has a `relationship` column, rows are split into SELF vs dependents. Son/Daughter/Child are normalized to CHILD1–CHILD4 by order per employee. Duplicate email is enforced only among SELF rows.
- **Dependents:** Dependent rows are validated without failing the whole upload. Valid dependents are stored in draft `EnrollmentSubmission.dependents` (JSONB) for employees that have dependent rows, so the self-enrollment form can pre-populate.
- **Extra columns:** Optional CSV columns (phone, gender, dateOfJoining, designation, department) are mapped to `Deals` and included in the self-enrollment context for prefill.
- **Frontend:** Duplicate-employeeId validation removed in `enrollmentValidators.ts` (multiple rows per employeeId allowed for SELF + dependents). Duplicate-email check applies only to employee (SELF) rows. Self-enrollment Dependents step shows "Pre-filled by HR" for dependents loaded from submission.
