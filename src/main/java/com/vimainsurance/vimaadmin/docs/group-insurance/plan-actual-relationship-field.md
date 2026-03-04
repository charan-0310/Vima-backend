# Plan: Store Actual Relationship (e.g. Son / Daughter) for Group Insurance

> **Goal:** Persist the user-provided relationship label (e.g. "Son", "Daughter", "Child") in addition to the normalized slot (`CHILD1`, `CHILD2`, …) so we can display and report the real relationship without changing how we identify records.

---

## 1. Design choice: separate column vs other options

### Recommended: add `actual_relationship` column

- **Meaning:** Store the original relationship string from the upload (e.g. "Son", "Daughter", "Child") when we normalize children to CHILD1–CHILD4. For non-child relationships, this can equal `relationship` or be null.
- **Pros:**
  - Clear semantics; no change to existing `relationship`-based lookups or uniqueness.
  - Easy to add to DTOs, APIs, and UI.
  - Optional: null for existing rows and for flows that don’t send it.
- **Cons:** One extra column and migration.

### Alternatives (not recommended)

- **Encode in `relationship`:** e.g. store "CHILD1|Son". Con: breaks existing queries and enums; parsing everywhere.
- **Use JSONB / metadata:** Con: not queryable or reportable in a simple way; more complex.
- **No new column, only frontend display:** Con: we don’t have the data after upload; can’t show “Son”/“Daughter” in backend-driven views or reports.

**Conclusion:** Add a nullable `actual_relationship` column and populate it from the bulk-upload (and any other) flow that has the original value.

---

## 2. Scope of changes

### 2.1 Database

- **Table:** `cpc.customers`
- **Change:** Add column `actual_relationship VARCHAR(50) NULL`.
- **Migration:** New Flyway script, e.g. `V{next}__add_actual_relationship_to_customers.sql`:
  - `ALTER TABLE cpc.customers ADD COLUMN actual_relationship VARCHAR(50) NULL;`
  - Optional: short comment on column for future reference.

No unique constraint; this is display/metadata only. Identity and lookups remain on `(employee_number, relationship)` (and org, etc.) as today.

### 2.2 Backend (Java)

| Layer | File / component | Change |
|-------|------------------|--------|
| Entity | `Deals.java` | Add `private String actualRelationship;` and `@Column(name = "actual_relationship", length = 50)`. |
| DTO (upload) | `EmployeeUploadDto.java` | Optional: add `actualRelationship` if we want to pass it explicitly from frontend. Alternatively backend can derive from request before normalization (see Frontend). |
| Mapper | `EmployeeToDeals.java` | In `mapToDeals` and `updateDealFromDto`: set `deal.setActualRelationship(dto.getActualRelationship())` (or derived value) when present. For new dependents created in `EmployeeService`, set `actualRelationship` from the DTO or from a new field. |
| Batch insert/update | `EmployeeBatchService.java` | Add `actual_relationship` to INSERT and UPDATE SQL and to `BatchPreparedStatementSetter` (get/set from entity). |
| Response DTOs | Any DTO that exposes relationship (e.g. `OrganizationEmployeeDto`, `EmployeeInsuranceResponseDto`, report DTOs) | Add `actualRelationship` (optional) so UI can show “Son”/“Daughter” when present. |
| Lookups | No change | All finders continue to use `relationship` (CHILD1, CHILD2, …). |

**When to set `actual_relationship`:**

- **Bulk upload (portal):** Frontend should send the original relationship (e.g. "Son", "Daughter") in each row **before** it is overwritten by CHILD1–CHILD4. Backend then receives both: normalized `relationship` (CHILD1, …) and a separate field we can map to `actual_relationship` (see Frontend below).
- **Backend-only CSV/import:** If the parser has the original string, set it when building the entity; otherwise leave null.
- **Self-enrollment / other flows:** Set when the source provides a label (e.g. “Son”); otherwise null. Existing data remains null until backfilled or updated.

### 2.3 Frontend (vima-web-portal)

- **Capture before overwrite:** In `fileParser.ts`, before calling `normalizeChildRelationships()`:
  - For each row, if `relationship` is a child type (Son/Daughter/Child), copy the current value to a new field, e.g. `actualRelationship` (or `originalRelationship`), on the row object.
  - Then run `normalizeChildRelationships()` as today (so `relationship` becomes CHILD1, CHILD2, …).
- **Payload:** Include `actualRelationship` (or the chosen name) in the row payload sent to the backend. Backend maps it to `actual_relationship` only when it’s a child-type value (or always when present).
- **Validation:** Optional: allow only a small set (e.g. Son, Daughter, Child) for `actualRelationship` when relationship is CHILD1–CHILD4.
- **Display:** Where we show relationship (e.g. member cards, tables), show `actualRelationship` if present, else fall back to `relationship` (e.g. “Child 1”).

---

## 3. Implementation order

1. **Flyway migration** — add `actual_relationship` to `cpc.customers`.
2. **Entity + batch service** — add field to `Deals`, and to batch INSERT/UPDATE in `EmployeeBatchService`.
3. **DTOs + mapper** — add to upload DTO and to `EmployeeToDeals` (and any response DTOs that need it).
4. **EmployeeService** — when creating/updating dependents from bulk upload, set `actualRelationship` from the DTO (or from the new frontend field).
5. **Frontend** — capture original relationship before normalization, add field to payload, then optionally use it in UI.
6. **APIs / responses** — expose `actual_relationship` where relationship is shown (e.g. org employees, insurance response, reports).
7. **Optional:** Backfill script or one-time update for existing CHILD1–CHILD4 rows if we ever get a source for historical “Son”/“Daughter” (else leave null).

---

## 4. Twins and multiple children

- **Identity and lookups:** Unchanged. We still identify by primary’s employee number + normalized `relationship` (CHILD1, CHILD2, …).
- **Storage:** Row 1 can have `relationship = CHILD1`, `actual_relationship = Son`; row 2 `relationship = CHILD2`, `actual_relationship = Son` (twins) or `Daughter`. So we support both “two sons” and “son + daughter” and retain the labels.

---

## 5. Naming

- **Column / entity:** `actual_relationship` is clear. Alternatives: `relationship_display`, `original_relationship` (implies we might not use it for display only). Recommend `actual_relationship`.
- **Frontend field:** Same name or `originalRelationship` for the value before CHILD1–CHILD4 normalization; keep one naming convention across FE and BE.

---

## 6. Summary

- Add nullable **`actual_relationship`** to `cpc.customers` and to the Deals entity.
- Keep **`relationship`** as the only field used for uniqueness and all existing lookups (emp + relationship).
- Frontend: capture original Son/Daughter/Child in a separate field before normalizing to CHILD1–CHILD4 and send it in the upload payload.
- Backend: persist it in batch and normal save/update; expose it in response DTOs where relationship is shown.
- No change to twins behaviour: we still create two rows (CHILD1, CHILD2); we just also store the actual label(s) (e.g. Son, Son or Son, Daughter) for display and reporting.
