# Data cleanup — fresh group insurance start

To start fresh with **new companies, policies, enrollment, and endorsements**, delete data in **child‑before‑parent** order so foreign keys are not violated. Use the order below (most dependent first).

**TRUNCATE vs DELETE vs DROP:**  
- **TRUNCATE** = remove all rows from a table; the **table and its structure stay**. It does not delete the table.  
- **DELETE** = remove rows (we use this below; same end result as TRUNCATE for “empty table”, but row-by-row).  
- **DROP TABLE** = remove the table itself (structure and data); we do **not** use this here.

The scripts below use **DELETE** only. Run in a transaction (`BEGIN;` … `COMMIT;`) if you want all-or-nothing.

---

## Cleanup order (table)

| Phase | Schema | Table | Why this order |
|-------|--------|-------|----------------|
| **1** | claims | claim_audit_log | References claims.claims |
| **1** | claims | claim_deductions | References claims.claims |
| **1** | claims | claim_queries | References claims.claims |
| **1** | claims | claim_settlement | References claims.claims |
| **1** | claims | claims | References organizations, policies, customers |
| **1** | claims | claim_number_sequence | No FKs; optional: delete rows or reset next_sequence |
| **2** | cpc | nominees | References policies, enrollment_submissions, customers |
| **2** | cpc | quote_companies | References quotes |
| **2** | cpc | quotes | References customers |
| **2** | cpc | motor_policy_details | References policies |
| **2** | cpc | deal_endorsements | References customers, endorsements |
| **2** | cpc | enrollment_submissions | References customers, enrollment_windows, invitations, endorsements |
| **2** | cpc | enrollment_invitations | References enrollment_windows, customers |
| **2** | cpc | endorsements | References organizations, enrollment_windows |
| **2** | cpc | enrollment_windows | References organizations |
| **3** | cpc | policies | References organizations, customers, insurance_providers |
| **4** | cpc | customers | References organizations; delete dependents before primaries if no CASCADE |
| **5** | admin | feature_flag_companies | References organizations, feature_flags |
| **5** | admin | admin_users | Set organization_id = NULL for HR Admins, or delete org-scoped users |
| **6** | cpc | organizations | Root tenant table — last |

---

## Notes

- **Documents:** If `document.documents` (or similar) stores rows keyed by entity (e.g. entity_type + entity_id), delete those document rows for claims, endorsements, enrollment, policies, and organizations **before** deleting those entities, or handle in app logic.
- **Keep (master/reference):** insurance_providers, feature_flags, feature_flag_roles, vendors (and related), incentive_packages (and related), zoho_token — unless you explicitly want to reset these too.
- **Customers:** Delete customers before organizations; the script order handles dependencies.
- **Claims:** If you are only resetting group insurance and want to **keep** claims data, skip Phase 1 in the script below.

---

## DBeaver-ready queries (DELETE only)

Copy and paste into DBeaver. Run the whole script, or run each block in order. Wrap in `BEGIN;` / `COMMIT;` if you want a single transaction.

```sql
-- =============================================================================
-- Phase 1: Claims (skip this block if you want to keep claims data)
-- =============================================================================

DELETE FROM claims.claim_audit_log;
DELETE FROM claims.claim_deductions;
DELETE FROM claims.claim_queries;
DELETE FROM claims.claim_settlement;
DELETE FROM claims.claims;

-- Optional: reset claim number sequence for current year
UPDATE claims.claim_number_sequence
SET next_sequence = 1, updated_at = NOW()
WHERE claim_year = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER;

-- =============================================================================
-- Phase 2: Enrollment & endorsements
-- =============================================================================

DELETE FROM cpc.nominees;
DELETE FROM cpc.quote_companies;
DELETE FROM cpc.quotes;
DELETE FROM cpc.motor_policy_details;
DELETE FROM cpc.deal_endorsements;
DELETE FROM cpc.enrollment_submissions;
DELETE FROM cpc.enrollment_invitations;
DELETE FROM cpc.endorsements;
DELETE FROM cpc.enrollment_windows;

-- =============================================================================
-- Phase 3: Policies
-- =============================================================================

DELETE FROM cpc.policies;

-- =============================================================================
-- Phase 4: Customers
-- =============================================================================

DELETE FROM cpc.customers;

-- =============================================================================
-- Phase 5: Org-scoped admin (before deleting organizations)
-- =============================================================================

DELETE FROM admin.feature_flag_companies
WHERE organization_id IN (SELECT organization_id FROM cpc.organizations);

UPDATE admin.admin_users
SET organization_id = NULL
WHERE organization_id IS NOT NULL;

-- =============================================================================
-- Phase 6: Organizations
-- =============================================================================

DELETE FROM cpc.organizations;
```

Adjust for your schema (e.g. document tables or extra dependents). See [database-tables-overview.md](./database-tables-overview.md) for full table relationships.
