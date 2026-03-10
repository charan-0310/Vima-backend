---
name: BE-01 Phase 2 Migrations
overview: The 7 Phase 2 schema migrations already exist in the codebase as V37–V43. This plan maps them to the BE-01 ticket (V12–V18), confirms alignment with the spec, and lists verification steps and any small fixes.
todos: []
isProject: false
---

# BE-01 Database Migrations — Phase 2 Schema (V37–V43)

## Current state

All 7 Phase 2 migrations are already implemented in [Vima-backend/src/main/resources/db/migration/](Vima-backend/src/main/resources/db/migration/) as **V37–V43** (not V12–V18). In this repo, V12–V18 are used by earlier migrations (e.g. V12__demo.sql, V13 self_enrollment, V14–V18 claims/customers). The "V12–V18" names in the ticket are PRD/logical names; the correct Flyway versions here are V37–V43, which run after BE-00 (V35 employee_policy_map, V36 backfill).


| Ticket (logical) | Actual file                                                                                                                                       | Purpose                                             |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------- |
| V12              | [V37__create_cost_sharing_rules.sql](Vima-backend/src/main/resources/db/migration/V37__create_cost_sharing_rules.sql)                             | cost_sharing_rules table                            |
| V13              | [V38__create_topup_plan_options.sql](Vima-backend/src/main/resources/db/migration/V38__create_topup_plan_options.sql)                             | topup_plan_options table                            |
| V14              | [V39__modify_premium_rate_tables_phase2.sql](Vima-backend/src/main/resources/db/migration/V39__modify_premium_rate_tables_phase2.sql)             | premium_rate_tables Phase 2 columns                 |
| V15              | [V40__modify_enrollment_tables_phase2.sql](Vima-backend/src/main/resources/db/migration/V40__modify_enrollment_tables_phase2.sql)                 | enrollment_submissions + enrollment_plan_selections |
| V16              | [V41__modify_company_enrollment_config_phase2.sql](Vima-backend/src/main/resources/db/migration/V41__modify_company_enrollment_config_phase2.sql) | company_enrollment_config Phase 2 columns           |
| V17              | [V42__create_payroll_deduction_schedules.sql](Vima-backend/src/main/resources/db/migration/V42__create_payroll_deduction_schedules.sql)           | payroll_deduction_schedules table                   |
| V18              | [V43__add_endorsement_life_event.sql](Vima-backend/src/main/resources/db/migration/V43__add_endorsement_life_event.sql)                           | life_event_type on endorsements                     |


---

## Migration-by-migration alignment

### V37 — cost_sharing_rules

- Matches ticket DDL: table, constraints (`unique_cost_sharing_rule`, `valid_employer_share_type`, `valid_percentage`), indexes. No change needed.

### V38 — topup_plan_options

- Ticket says `policy_id UUID REFERENCES cpc.policies(id)`. Baseline defines `cpc.policies` with PK `policy_id` (bigserial). Current migration correctly uses `policy_id BIGINT REFERENCES cpc.policies(policy_id)`. No change.

### V39 — premium_rate_tables (Phase 2)

- Ticket: "Add to cpc.premium_rate_tables: pricing_model, sum_insured_amount, family_size_min/max, rate_source, gst_inclusive, gst_percentage, is_deleted".
- Current: `CREATE TABLE IF NOT EXISTS cpc.premium_rate_tables (...)` with all base + Phase 2 columns. No other migration creates this table, so on a fresh or existing DB (without the table) this is correct.
- **Recommendation**: If `premium_rate_tables` is ever introduced in an earlier migration with fewer columns, add a follow-up migration that uses `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for the Phase 2 columns so existing DBs get them. Not required for current history.

### V40 — enrollment_submissions + enrollment_plan_selections

- **enrollment_submissions**: All requested columns added with `ADD COLUMN IF NOT EXISTS` and safe defaults (`cost_sharing_snapshot JSONB DEFAULT '{}'`). Existing data unaffected.
- **enrollment_plan_selections**: Table created with id, enrollment_submission_id, plan_type, opted, coverage_amount, premium, sum_insured, deductible_amount, topup_plan_option_id, is_voluntary, timestamps. Matches ticket. Indexes on submission and plan_type. No change.

### V41 — company_enrollment_config (Phase 2)

- Ticket: add parent_coverage_enabled, in_law_coverage_enabled, max_parents, max_in_laws, parent_age_limit.
- Current: `CREATE TABLE IF NOT EXISTS` with those columns and unique(organization_id). Table is not created elsewhere, so this is correct. Same "if table ever created earlier" note as V39.

### V42 — payroll_deduction_schedules

- Full table with organization_id, employee_id, enrollment_submission_id, product_type, plan_type, coverage_amount, total/employer/employee premium, deduction_frequency, deduction_amount_per_period, effective_from/to, timestamps. Indexes on org, employee, submission, effective. No change.

### V43 — endorsements.life_event_type

- `ALTER TABLE cpc.endorsements ADD COLUMN IF NOT EXISTS life_event_type VARCHAR(50);` plus COMMENT. Nullable, so existing data unaffected. No change.

---

## Acceptance criteria


| Criterion                                 | Status                                                                                                                               |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| All 7 migrations run cleanly              | V37–V43 are in order; BE-00 (V35, V36) runs first.                                                                                   |
| Fresh and existing DBs                    | New columns/tables use nullable or defaulted values; ALTERs use `IF NOT EXISTS` where used.                                          |
| Rollback tested                           | Not implemented in repo (Flyway has no undo by default). Plan: document manual rollback steps or add optional undo scripts and test. |
| Indexes created and verified              | All migrations that create tables/indexes include the required indexes.                                                              |
| employee_policy_map exists before Phase 2 | V35 creates table, V36 backfill; V37 runs after.                                                                                     |


---

## Optional: rollback and verification

1. **Rollback**: For each of V37–V43, document or add a manual rollback (e.g. `DROP TABLE` / `ALTER TABLE ... DROP COLUMN`) and run once on a copy of the DB to satisfy "Rollback tested".
2. **Verification**: Run `mvn flyway:validate` (or equivalent) and a test that applies migrations on a clean DB and on a DB that already has V0–V36 applied, and confirm no errors and expected tables/columns present.

---

## Summary

- No code changes are strictly required for BE-01: the 7 Phase 2 migrations exist as V37–V43 and match the ticket.
- Optional: add ALTER-based migrations for V39/V41 if `premium_rate_tables` or `company_enrollment_config` are later introduced in an earlier version; document and test rollback for each of V37–V43.
