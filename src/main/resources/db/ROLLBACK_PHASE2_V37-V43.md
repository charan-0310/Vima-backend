# BE-01 Phase 2: Manual Rollback Scripts (V37–V43)

Use these scripts only when you need to manually roll back Phase 2 schema changes. Run in **reverse order** (V43 first, V37 last). After running, remove the corresponding rows from `flyway_schema_history` if you want Flyway to re-apply those migrations in the future.

**Prerequisite:** Ensure no application code or other migrations depend on these objects before rollback.

---

## Rollback order

Execute in this order: **V43 → V42 → V41 → V40 → V39 → V38 → V37**.

---

### 1. Rollback V43 — `life_event_type` on endorsements

```sql
-- BE-01 Phase 2 rollback: V43__add_endorsement_life_event
ALTER TABLE cpc.endorsements DROP COLUMN IF EXISTS life_event_type;
```

---

### 2. Rollback V42 — payroll_deduction_schedules

```sql
-- BE-01 Phase 2 rollback: V42__create_payroll_deduction_schedules
DROP TABLE IF EXISTS cpc.payroll_deduction_schedules;
```

---

### 3. Rollback V41 — company_enrollment_config

```sql
-- BE-01 Phase 2 rollback: V41__modify_company_enrollment_config_phase2
DROP TABLE IF EXISTS cpc.company_enrollment_config;
```

---

### 4. Rollback V40 — enrollment_submissions columns + enrollment_plan_selections

```sql
-- BE-01 Phase 2 rollback: V40__modify_enrollment_tables_phase2
-- Drop dependent table first (references enrollment_submissions and topup_plan_options)
DROP TABLE IF EXISTS cpc.enrollment_plan_selections;

-- Remove Phase 2 columns from enrollment_submissions
ALTER TABLE cpc.enrollment_submissions DROP COLUMN IF EXISTS deduction_frequency;
ALTER TABLE cpc.enrollment_submissions DROP COLUMN IF EXISTS total_employee_annual_premium;
ALTER TABLE cpc.enrollment_submissions DROP COLUMN IF EXISTS total_employer_annual_premium;
ALTER TABLE cpc.enrollment_submissions DROP COLUMN IF EXISTS cost_sharing_snapshot;
ALTER TABLE cpc.enrollment_submissions DROP COLUMN IF EXISTS deduction_amount_per_period;
ALTER TABLE cpc.enrollment_submissions DROP COLUMN IF EXISTS consent_timestamp;
ALTER TABLE cpc.enrollment_submissions DROP COLUMN IF EXISTS consent_text_snapshot;
```

---

### 5. Rollback V39 — premium_rate_tables

```sql
-- BE-01 Phase 2 rollback: V39__modify_premium_rate_tables_phase2
DROP TABLE IF EXISTS cpc.premium_rate_tables;
```

---

### 6. Rollback V38 — topup_plan_options

```sql
-- BE-01 Phase 2 rollback: V38__create_topup_plan_options
-- enrollment_plan_selections (if still present) references topup_plan_options; drop that first (see V40 rollback)
DROP TABLE IF EXISTS cpc.topup_plan_options;
```

---

### 7. Rollback V37 — cost_sharing_rules

```sql
-- BE-01 Phase 2 rollback: V37__create_cost_sharing_rules
DROP TABLE IF EXISTS cpc.cost_sharing_rules;
```

---

## Clean Flyway history (optional)

To allow Flyway to re-apply these migrations later, delete the corresponding version rows (run only after applying the rollback SQL above):

```sql
DELETE FROM flyway_schema_history WHERE version IN ('37', '38', '39', '40', '41', '42', '43');
```

---

## Testing rollback

1. Take a backup or use a copy of the database.
2. Apply rollback scripts in order (V43 → V37).
3. Verify tables/columns are removed and no broken references remain.
4. Optionally run application tests against the rolled-back schema.

---

## Verifying migrations (acceptance criteria)

- **Validate migration checksums (requires running PostgreSQL):**
  ```bash
  cd Vima-backend
  mvn flyway:validate -Dflyway.url=jdbc:postgresql://localhost:5433/vima_dev -Dflyway.user=<user> -Dflyway.password=<password>
  ```
  Or start the application (e.g. `mvn spring-boot:run -Dspring-boot.run.profiles=dev`); Flyway runs on startup and will apply pending migrations.

- **Fresh vs existing DB:** Run migrations once on an empty DB and once on a DB that has only V0–V36 applied; confirm no errors and that all Phase 2 tables/columns exist.
