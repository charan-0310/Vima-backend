# Group Insurance Bulk Upload & Relationship Identity

> **Purpose:** Document how bulk upload works, where **employee number + relationship** is used as a unique way to identify records, and what happens in the **twins** case (e.g. two "Son" or two "Daughter").

---

## 1. Data model (customers / Deals)

- **Table:** `cpc.customers` (JPA entity: `Deals`).
- **Key columns:**
  - `individual_id` (PK)
  - `employee_number` — for **primary (Self)** this is the employee’s ID; for **dependents** (in portal bulk-upload flow) it is set to the **primary’s** employee number so we can look up by “primary emp + relationship”.
  - `relationship` — stored as normalized enum-style values: `SELF`, `SPOUSE`, `FATHER`, `MOTHER`, `FATHER_IN_LAW`, `MOTHER_IN_LAW`, `CHILD1`, `CHILD2`, `CHILD3`, `CHILD4`.
  - `primary_individual_id` — FK to primary member (null for Self).

There is **no** DB unique constraint on `(employee_number, relationship)`; uniqueness is enforced in application logic (one SELF per employee number, one CHILD1 per primary, etc.).

---

## 2. Frontend bulk upload flow

### 2.1 Parse & normalize (fileParser)

- **File:** `vima-web-portal/src/services/fileParser.ts`
- **Flow:**
  1. Parse Excel/CSV into rows with `employeeId`, `relationship`, `name`, `dateOfBirth`, etc.
  2. **Relationship normalization:** `normalizeRelationship()` maps known values (e.g. "Employee" → "SELF"). It does **not** map "Son"/"Daughter"/"Child" here.
  3. **Child normalization:** `normalizeChildRelationships()` runs **after** parse:
     - Groups rows by `employeeId`.
     - For each group, in **row order**, any row whose `relationship` is one of `son`, `daughter`, `child` (case-insensitive) is **replaced** with `CHILD1`, `CHILD2`, `CHILD3`, or `CHILD4` in sequence (max 4).
- **Effect:** The original value ("Son", "Daughter", "Child") is **discarded**; only the ordinal slot (CHILD1–CHILD4) is sent to the backend.

### 2.2 Payload sent to backend

- **Service:** `vima-web-portal/src/services/bulkUploadService.ts`
- **Endpoint:** `POST organization/:organizationId/upload` (multipart: file + `employees` JSON).
- **Payload:** Array of employee rows; each has `relationship` already normalized to `SELF`, `SPOUSE`, `CHILD1`, `CHILD2`, etc. No separate field for “actual” relationship (Son/Daughter).

---

## 3. Backend: where employee number + relationship identify a record

These are the places that effectively use **employee number + relationship** (and sometimes org/name) to find or treat a row as unique.

### 3.1 Portal bulk upload (EmployeeService)

- **File:** `EmployeeService.java` → `uploadEmployees()`.

**Primaries (Self):**

- **Lookup:** `dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(employeeId, organizationId, "SELF")`.
- **Uniqueness:** One primary per `(employee_number, organization_id, relationship = SELF)`.

**Dependents:**

- Dependents are grouped by the **primary’s** `employeeId` (from CSV). For **new** dependents the code sets `newDependent.setEmployeeNumber(employeeId)` (primary’s employee number) so they can be found later by the same pattern.
- **Lookup:** Existing dependents are loaded by `findByPrimaryIndividualIdIn(primaryIndividualIds)`, then grouped by `relationship` (`existingDependentsByRelationship`). So for a given primary we resolve “the one with this relationship” (e.g. CHILD1, CHILD2).
- **Uniqueness:** One row per `(primary’s employee_number, organization_id, relationship)` where `relationship` is the normalized value (e.g. CHILD1, CHILD2). No Son/Daughter stored.

**Repository:**

- `IDealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(employeeNumber, organizationId, relationship)` — exact match on `employee_number`, `organization_id`, `relationship`. Used for primary (SELF) and for **bulk deletion** (see below) to find the single deal (primary or dependent) for that emp number + relationship.

### 3.2 Bulk deletion (EmployeeService)

- **File:** `EmployeeService.java` → `deleteEmployee()`.
- **Lookup:** For each deletion row, `findByEmployeeNumberAndOrganizationIdAndRelationship(employeeId, organizationId, relationship)`.
- **Meaning:** `employeeId` is the **primary’s** employee number (same as in upload); `relationship` is the normalized value (SELF, CHILD1, CHILD2, …). So again, **employee number + relationship** uniquely identify the record to delete.

### 3.3 Health ID upload (EndorsementServiceImpl)

- **File:** `EndorsementServiceImpl.java` (health ID upload).
- **Lookup:** `findByNameAndEmployeeNumberAndRelationshipAndOrganizationIdForEndorsement(name, employeeId, relationship, organizationId, endorsementId)` (and fallback without endorsement for SELF).
- **Meaning:** Record is identified by **name + employee number (or primary’s) + relationship** within org/endorsement. So relationship is again part of the identity for matching.

### 3.4 OrganizationServiceImpl (CSV deletion / validation)

- **File:** `OrganizationServiceImpl.java`.
- **Lookup:** For dependent deletion, dependents are fetched by primary; then matching is by **relationship + first name** (`existingDependent.getRelationship().equalsIgnoreCase(deal.getRelationship())` and `getFirstName()` match).
- **Meaning:** Here “identity” is primary + relationship + name; relationship is still the stored normalized value (CHILD1, etc.).

---

## 4. Twins / multiple children of same type

### 4.1 Current behaviour

- **Example:** CSV has two rows for same employee: Relationship "Son", "Son" (twins).
- **Frontend:** Both become CHILD1 and CHILD2 (in row order). Backend receives `CHILD1` and `CHILD2`.
- **Backend:** Creates/updates two dependents: one with `relationship = CHILD1`, one with `relationship = CHILD2`. So we **do** support two (or more) children; they are distinguished by **ordinal** (CHILD1, CHILD2, …), not by “Son” vs “Daughter”.

### 4.2 What is lost

- We **do not** persist the original relationship label (Son / Daughter / Child).
- So for twins both “Son”, we correctly get two rows (CHILD1, CHILD2), but we cannot tell they were both “Son”. Similarly, if the user had one “Son” and one “Daughter”, we only store CHILD1 and CHILD2; we lose which was Son and which was Daughter.

### 4.3 Summary table

| Scenario              | CSV relationship | After frontend | Stored in DB | Note                          |
|----------------------|------------------|----------------|--------------|-------------------------------|
| One son              | Son              | CHILD1         | CHILD1       | Son/Daughter not stored       |
| One daughter         | Daughter         | CHILD1         | CHILD1       | Son/Daughter not stored       |
| Twins (two sons)     | Son, Son         | CHILD1, CHILD2 | CHILD1, CHILD2 | Two rows; “Son” not stored  |
| Son + daughter       | Son, Daughter    | CHILD1, CHILD2 | CHILD1, CHILD2 | Two rows; which is which lost |

---

## 5. References (code locations)

| Area              | File / component              | What uses emp + relationship                         |
|-------------------|------------------------------|------------------------------------------------------|
| Repository        | `IDealsRepository`           | `findByEmployeeNumberAndOrganizationIdAndRelationship`; name+emp+relationship queries (JPQL and native) |
| Upload             | `EmployeeService.uploadEmployees` | Primary/dependent match by emp + relationship   |
| Deletion (portal)  | `EmployeeService.deleteEmployee`  | Find deal by emp + relationship for SELF/dependent |
| Health ID          | `EndorsementServiceImpl`     | Match by name + employee number + relationship      |
| CSV deletion       | `OrganizationServiceImpl`    | Match dependent by relationship + first name       |
| Frontend parse     | `fileParser.normalizeChildRelationships` | Son/Daughter/Child → CHILD1–CHILD4          |
| Frontend validation| `employeeValidation.ts`      | Valid relationships (SELF, SPOUSE, CHILD1–4, …)    |

---

## 6. Conclusion

- **Unique identification** of a customer row (for a given org) is effectively by **primary’s employee number + relationship** (and for dependents, `employee_number` on the row is set to the primary’s employee number in the portal upload flow).
- **Twins** (e.g. two “Son” or two “Daughter”) are supported as two distinct rows (CHILD1, CHILD2); the only gap is that we do **not** store the original relationship text (Son/Daughter/Child), so we cannot display or report “actual” relationship without adding a new field and populating it (e.g. `actual_relationship`).
