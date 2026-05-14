# Production runbook — delete demo organizations (Postgres / pgAdmin)

This runbook deletes the following demo organizations from **Production**, along with related records across schemas.

- `3cd63934-59cb-4972-87ff-e047642e4640`
- `60bbee7b-eff9-4ca1-ab9f-3fc73580901d`
- `43807613-dea0-46d4-b8ce-b3a6aec368c5`

> **Rollback strategy**: restore from a pre-delete logical backup (pgAdmin “Backup” → Custom format) into a new DB and cut over.

---

## 0) Preflight checklist

- Confirm you are connected to the **correct PROD database**.
- Ensure you have enough disk space for a full backup.
- Prefer running during a maintenance window if these orgs have high row counts.

Run this in pgAdmin Query Tool to record where you are:

```sql
SELECT now() AS ts,
       current_database() AS db,
       current_user AS db_user,
       inet_server_addr() AS server_ip,
       inet_server_port() AS server_port;
```

---

## 1) Backup plan (pgAdmin UI)

You can’t run `pg_dump` as SQL inside Query Tool; use pgAdmin’s Backup UI.

### 1A) Take a full logical backup

- Right-click **Database → Backup…**
- **Format**: `Custom` (recommended)
- **Sections**: Pre-data, Data, Post-data
- **Options**:
  - Enable **Blobs** if you use large objects
  - Leave **Only data** unchecked (you want schema + data)
- Save as: `prod-pre-demo-org-delete-YYYYMMDD.backup`

### 1B) Validate the backup (recommended)

Safest:

- Create a new empty database (e.g., `prod_restore_check_YYYYMMDD`)
- Right-click the new DB → **Restore…** → choose the backup file
- Confirm restore completes without errors

---

## 2) TEMP table note (your question)

This runbook uses a `TEMP` table named `target_orgs`.

- **You do not need to delete it manually.**
- A `TEMP` table exists only for your current DB session and is automatically dropped when the session ends (closing the Query Tool tab / disconnecting).
- If you want it explicitly removed inside the same session anyway, you can run:

```sql
DROP TABLE IF EXISTS target_orgs;
```

---

## 3) Preflight SQL — define targets + sanity check

```sql
BEGIN;

CREATE TEMP TABLE target_orgs (organization_id uuid PRIMARY KEY);

INSERT INTO target_orgs (organization_id)
SELECT organization_id
FROM cpc.organizations
WHERE organization_id IN (
  '3cd63934-59cb-4972-87ff-e047642e4640',
  '60bbee7b-eff9-4ca1-ab9f-3fc73580901d',
  '43807613-dea0-46d4-b8ce-b3a6aec368c5'
);

-- Sanity check: confirm these are the right orgs
SELECT organization_id, organization_name, status, created_at
FROM cpc.organizations
WHERE organization_id IN (SELECT organization_id FROM target_orgs);

COMMIT;
```

---

## 4) Impact report — find all FKs referencing `cpc.organizations`

This is the authoritative way to see what Postgres will block (`RESTRICT`/`NO ACTION`) vs cascade.

```sql
SELECT
  nsp_child.nspname  AS child_schema,
  child.relname      AS child_table,
  a_child.attname    AS child_column,
  con.conname        AS fk_name,
  con.confdeltype    AS on_delete_action
FROM pg_constraint con
JOIN pg_class child              ON child.oid = con.conrelid
JOIN pg_namespace nsp_child      ON nsp_child.oid = child.relnamespace
JOIN pg_class parent             ON parent.oid = con.confrelid
JOIN pg_namespace nsp_parent     ON nsp_parent.oid = parent.relnamespace
JOIN unnest(con.conkey)  WITH ORDINALITY AS ck(attnum, ord) ON true
JOIN unnest(con.confkey) WITH ORDINALITY AS pk(attnum, ord) ON pk.ord = ck.ord
JOIN pg_attribute a_child  ON a_child.attrelid = child.oid  AND a_child.attnum = ck.attnum
JOIN pg_attribute a_parent ON a_parent.attrelid = parent.oid AND a_parent.attnum = pk.attnum
WHERE con.contype = 'f'
  AND nsp_parent.nspname = 'cpc'
  AND parent.relname = 'organizations'
ORDER BY child_schema, child_table, child_column;
```

Decode `on_delete_action`:
- `c` = CASCADE
- `r` = RESTRICT
- `a` = NO ACTION
- `n` = SET NULL
- `d` = SET DEFAULT

Optional: for any `(child_schema.child_table, child_column)` from above, estimate affected rows:

```sql
-- Template (fill in table/column from the FK query)
SELECT COUNT(*)
FROM child_schema.child_table ct
JOIN target_orgs t ON ct.child_column = t.organization_id;
```

---

## 5) Delete script (transactional, ordered)

This order is based on what exists in your Flyway migrations; key blockers are:

- `cpc.cd_balance_transactions.organization_id` is `ON DELETE RESTRICT` → must be deleted before deleting orgs.
- `cpc.cd_accounts.organization_id` is `ON DELETE RESTRICT` (added in `V57`) → must be deleted before deleting orgs.
- `cpc.wellness_access_logs.organization_id` → `cpc.organizations` (`V66`, default `NO ACTION`) → must be deleted before deleting orgs.
- `admin.feature_flag_companies.organization_id` → `cpc.organizations` (FK enforced in DB; `V1` table) → must be deleted before deleting orgs.
- `admin.notifications.company_id` → `cpc.organizations` (`V75`, `ON DELETE SET NULL`) → not an FK blocker; delete explicitly so inbox/delivery rows for the org are removed (`notification_deliveries` cascades from `notifications`).
- `cpc.endorsements.policy_id` → `cpc.policies` (`fk_endorsements_policy`, `V59`, default `NO ACTION`) → **endorsements for the org must be removed before policies** (otherwise Postgres raises `23503` on `DELETE FROM cpc.policies`).

Run this in pgAdmin Query Tool. Targets are loaded with `INSERT … SELECT … WHERE organization_id IN (…)` so `target_orgs` only contains IDs that exist in `cpc.organizations` at run time.

```sql
BEGIN;

SET LOCAL statement_timeout = '20min';
SET LOCAL lock_timeout = '30s';

CREATE TEMP TABLE target_orgs (organization_id uuid PRIMARY KEY);

INSERT INTO target_orgs (organization_id)
SELECT organization_id
FROM cpc.organizations
WHERE organization_id IN (
  '3cd63934-59cb-4972-87ff-e047642e4640',
  '60bbee7b-eff9-4ca1-ab9f-3fc73580901d',
  '43807613-dea0-46d4-b8ce-b3a6aec368c5'
);

-- Last chance sanity check
SELECT organization_id, organization_name
FROM cpc.organizations
WHERE organization_id IN (SELECT organization_id FROM target_orgs);

-- ============================================================
-- A) CD Balance tables (RESTRICT FKs)
-- ============================================================

DELETE FROM cpc.cd_transaction_documents d
USING cpc.cd_balance_transactions t, target_orgs o
WHERE d.transaction_id = t.transaction_id
  AND t.organization_id = o.organization_id;

DELETE FROM cpc.cd_balance_transactions t
USING target_orgs o
WHERE t.organization_id = o.organization_id;

DELETE FROM cpc.cd_accounts a
USING target_orgs o
WHERE a.organization_id = o.organization_id;

-- ============================================================
-- B) Enrollment / payroll / pricing / catalog (direct org FKs)
-- ============================================================

DELETE FROM cpc.payroll_deduction_schedules p
USING target_orgs o
WHERE p.organization_id = o.organization_id;

DELETE FROM cpc.payroll_report_schedule p
USING target_orgs o
WHERE p.organization_id = o.organization_id;

DELETE FROM cpc.premium_rate_tables pr
USING target_orgs o
WHERE pr.organization_id = o.organization_id;

DELETE FROM cpc.product_catalog pc
USING target_orgs o
WHERE pc.organization_id = o.organization_id;

DELETE FROM cpc.cost_sharing_rules csr
USING target_orgs o
WHERE csr.company_id = o.organization_id;

DELETE FROM cpc.topup_plan_options tpo
USING target_orgs o
WHERE tpo.company_id = o.organization_id;

DELETE FROM cpc.flex_budget_config fbc
USING target_orgs o
WHERE fbc.company_id = o.organization_id;

DELETE FROM cpc.company_enrollment_config cec
USING target_orgs o
WHERE cec.organization_id = o.organization_id;

-- employee_policy_map has an org column (no cascade specified in migration)
DELETE FROM cpc.employee_policy_map epm
USING target_orgs o
WHERE epm.organization_id = o.organization_id;

-- Pre-clear customer FKs that point to enrollment records in this org
UPDATE cpc.customers c
SET enrollment_submission_id = NULL
FROM target_orgs o
WHERE c.organization_id = o.organization_id;

UPDATE cpc.customers c
SET enrollment_window_id = NULL
FROM target_orgs o
WHERE c.organization_id = o.organization_id;

-- nominees has check_policy_or_submission; delete nominees tied to target submissions
-- (setting submission_id = NULL can violate the check when policy_id is NULL)
DELETE FROM cpc.nominees n
USING cpc.enrollment_submissions s, cpc.enrollment_windows w, target_orgs o
WHERE n.submission_id = s.id
  AND s.enrollment_window_id = w.id
  AND w.organization_id = o.organization_id;

-- enrollment_submissions/invitations hang off enrollment_windows
DELETE FROM cpc.enrollment_submissions s
USING cpc.enrollment_windows w, target_orgs o
WHERE s.enrollment_window_id = w.id
  AND w.organization_id = o.organization_id;

DELETE FROM cpc.enrollment_invitations i
USING cpc.enrollment_windows w, target_orgs o
WHERE i.enrollment_window_id = w.id
  AND w.organization_id = o.organization_id;

-- endorsements can reference enrollment_window_id (FK blocker for window delete)
UPDATE cpc.endorsements e
SET enrollment_window_id = NULL
FROM cpc.enrollment_windows w, target_orgs o
WHERE e.enrollment_window_id = w.id
  AND w.organization_id = o.organization_id;

DELETE FROM cpc.enrollment_windows w
USING target_orgs o
WHERE w.organization_id = o.organization_id;

-- ============================================================
-- C) Claims (direct org FK)
-- ============================================================

DELETE FROM claims.claims c
USING target_orgs o
WHERE c.organization_id = o.organization_id;

-- ============================================================
-- D) Core CPC tables
-- ============================================================

-- Endorsements reference policies (fk_endorsements_policy) and may reference each other
-- (parent_endorsement_id). Remove endorsements before policies; clear parent links first.
UPDATE cpc.endorsements e
SET parent_endorsement_id = NULL
FROM target_orgs o
WHERE e.organization_id = o.organization_id;

DELETE FROM cpc.endorsements e
USING target_orgs o
WHERE e.organization_id = o.organization_id;

DELETE FROM cpc.policies p
USING target_orgs o
WHERE p.organization_id = o.organization_id;

DELETE FROM cpc.customers c
USING target_orgs o
WHERE c.organization_id = o.organization_id;

-- wellness_access_logs.organization_id is NO ACTION (V66)
DELETE FROM cpc.wellness_access_logs w
USING target_orgs o
WHERE w.organization_id = o.organization_id;

-- ============================================================
-- E) Admin + audit references
-- ============================================================

-- Per-org feature flag rows (FK to cpc.organizations in DB)
DELETE FROM admin.feature_flag_companies ffc
USING target_orgs o
WHERE ffc.organization_id = o.organization_id;

-- Unified notifications (V75): company_id → organizations; deliveries cascade from notifications
DELETE FROM admin.notifications n
USING target_orgs o
WHERE n.company_id = o.organization_id;

DELETE FROM admin.admin_users u
USING target_orgs o
WHERE u.organization_id = o.organization_id;

DELETE FROM audit.audit_events a
USING target_orgs o
WHERE a.organization_id = o.organization_id;

-- ============================================================
-- F) Finally delete organizations
-- ============================================================

DELETE FROM cpc.organizations org
USING target_orgs o
WHERE org.organization_id = o.organization_id;

-- Verify
SELECT COUNT(*) AS remaining
FROM cpc.organizations
WHERE organization_id IN (SELECT organization_id FROM target_orgs);

COMMIT;
```

If anything looks wrong before `COMMIT`, run:

```sql
ROLLBACK;
```

---

## 6) Rollback plan (restore-from-backup)

### Option A (recommended): restore into a new DB, then cut over

- Create a new empty DB: `prod_rollback_YYYYMMDD`
- pgAdmin → right-click that DB → **Restore…** → choose the backup from Section 1
- Validate application behavior against the restored DB
- Cut over by switching DB connection configuration (or your standard DB cutover procedure)

### Option B: restore onto the same DB

Only if you can tolerate downtime and you have a clean “drop/recreate + restore” procedure.

