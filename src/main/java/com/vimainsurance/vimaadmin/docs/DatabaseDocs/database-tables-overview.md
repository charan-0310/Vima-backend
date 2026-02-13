# Vima Database — Tables & Relationships Overview

> **Purpose:** Catalog of all database tables, their columns, primary/foreign keys, and relationships.
> Prepared as a foundation for **audit planning**.
>
> Schema diagrams source: ER diagrams (Feb 2026)

---

## Schema: `cpc` (Core Business Tables)

All business tables live under the `cpc` schema. UUID primary keys are used throughout.

---

### 1. `organizations`

Core tenant table — every company/client in the system.

| Column | Type | Notes |
|--------|------|-------|
| **organization_id** (PK) | UUID | |
| organization_name | VARCHAR | |
| gstin | VARCHAR | |
| pan_number | VARCHAR | |
| primary_contact_email | VARCHAR | |
| primary_contact_phone | VARCHAR | |
| status | ENUM | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| registered_address | VARCHAR | |
| industry | VARCHAR | |
| primary_contact_name | VARCHAR | |

**Referenced by:** policies, endorsements, enrollment_windows, customers/individuals, admin_users

---

### 2. `customers` (Individuals / Employees)

Central people table — employees, dependents, and leads.

| Column | Type | Notes |
|--------|------|-------|
| **individual_id** (PK) | UUID | |
| first_name | VARCHAR | |
| last_name | VARCHAR | |
| full_name | VARCHAR | |
| email | VARCHAR | |
| phone | VARCHAR | |
| date_of_birth | DATE | |
| gender | ENUM | |
| pan_number | VARCHAR | |
| aadhaar_number | VARCHAR | |
| address | VARCHAR | |
| city | VARCHAR | |
| state | VARCHAR | |
| pincode | VARCHAR | |
| account_type | ENUM | |
| status | ENUM | |
| organization_id | UUID | FK → organizations |
| employee_number | VARCHAR | |
| primary_individual_id | UUID | FK → self (for dependents) |
| relationship | VARCHAR | Relationship to primary member |
| is_primary_member | BOOLEAN | |
| username | VARCHAR | |
| password_hash | VARCHAR | |
| preferred_language | VARCHAR | |
| lead_id | UUID | |
| cust_id | VARCHAR | Legacy CRM ID |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| date_of_joining | DATE | |
| designation | VARCHAR | |
| marital_status | VARCHAR | |
| sum_insured | NUMERIC | |
| date_of_exit | DATE | |
| endorsement_id | UUID | FK → endorsements |
| department | VARCHAR | |
| reason_for_exit | VARCHAR | |
| health_id | VARCHAR | |
| enrollment_status | ENUM | |
| enrollment_window_id | UUID | FK → enrollment_windows |
| enrollment_submission_id | UUID | FK → enrollment_submissions |

**Relationships:**
- Belongs to → `organizations` (via organization_id)
- Self-referencing → primary_individual_id (dependent → primary member)
- Referenced by → policies, quotes, enrollment_invitations, enrollment_submissions, nominees, deal_endorsements

---

### 3. `policies`

Insurance policies issued to organizations/individuals.

| Column | Type | Notes |
|--------|------|-------|
| **policy_id** (PK) | UUID | |
| policy_number | VARCHAR | Insurer-assigned number |
| primary_individual_id | UUID | FK → customers |
| insurance_provider_id | UUID | FK → insurance_providers |
| insurance_product_id | UUID | |
| organization_id | UUID | FK → organizations |
| product_type | ENUM | |
| coverage_type | VARCHAR | |
| status | ENUM | |
| sum_insured | NUMERIC | |
| total_premium_amount | NUMERIC | |
| start_date | DATE | |
| end_date | DATE | |
| renewal_date | DATE | |
| lead_id | UUID | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| covered_individuals | JSONB | |
| payment_frequency | VARCHAR | |
| document_id | UUID | |
| net_amount | NUMERIC | |
| gst | NUMERIC | |
| tpa_organization_name | VARCHAR | |
| tpa_contact_info | VARCHAR | |
| policy_category | VARCHAR | |
| sum_insured_multiplier | NUMERIC | |
| applies_to_employees | BOOLEAN | |

**Relationships:**
- Belongs to → `organizations`, `customers` (primary_individual_id), `insurance_providers`
- Has many → `nominees`, `motor_policy_details`

---

### 4. `motor_policy_details`

Vehicle-specific details for motor insurance policies.

| Column | Type | Notes |
|--------|------|-------|
| **motor_policy_id** (PK) | UUID | |
| policy_id | UUID | FK → policies |
| vehicle_registration_number | VARCHAR | |
| vehicle_make | VARCHAR | |
| vehicle_model | VARCHAR | |
| vehicle_type | VARCHAR | |
| manufacturing_year | INTEGER | |
| registration_date | DATE | |
| idv_value | NUMERIC | Insured Declared Value |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Relationships:**
- Belongs to → `policies`

---

### 5. `endorsements`

Batch change requests (additions/deletions of employees) on a policy.

| Column | Type | Notes |
|--------|------|-------|
| **endorsement_id** (PK) | UUID | |
| organization_id | UUID | FK → organizations |
| document_id | UUID | |
| endorsement_type | ENUM | ADDITION / DELETION |
| status | ENUM | |
| total_employees | INTEGER | |
| total_dependents | INTEGER | |
| approved_at | TIMESTAMP | |
| confirmation_method | VARCHAR | |
| insurer_ref_number | VARCHAR | |
| premium_change_type | VARCHAR | |
| premium_amount | NUMERIC | |
| uploaded_by | UUID | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| approved_by | UUID | |
| source | VARCHAR | |
| enrollment_window_id | UUID | FK → enrollment_windows |
| submission_count | INTEGER | |
| source_metadata | JSONB | |

**Relationships:**
- Belongs to → `organizations`, `enrollment_windows`
- Has many → `deal_endorsements`, `enrollment_submissions`

---

### 6. `deal_endorsements`

Junction table linking individuals to endorsements.

| Column | Type | Notes |
|--------|------|-------|
| **deal_endorsement_id** (PK) | UUID | |
| individual_id | UUID | FK → customers |
| endorsement_id | UUID | FK → endorsements |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Relationships:**
- Belongs to → `customers`, `endorsements`

---

### 7. `enrollment_windows`

Time-bound enrollment periods for an organization.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| organization_id | UUID | FK → organizations |
| name | VARCHAR | |
| description | VARCHAR | |
| start_date | TIMESTAMP | |
| end_date | TIMESTAMP | |
| status | ENUM | |
| config | JSONB | Window configuration |
| created_by | UUID | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| closed_at | TIMESTAMP | |

**Relationships:**
- Belongs to → `organizations`
- Has many → `enrollment_invitations`, `enrollment_submissions`, `endorsements`

---

### 8. `enrollment_invitations`

Magic-link invitations sent to employees for self-enrollment.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| enrollment_window_id | UUID | FK → enrollment_windows |
| employee_id | UUID | FK → customers |
| token_hash | VARCHAR | Hashed magic-link token |
| status | ENUM | |
| sent_at | TIMESTAMP | |
| opened_at | TIMESTAMP | |
| completed_at | TIMESTAMP | |
| expires_at | TIMESTAMP | |
| reminder_count | INTEGER | |
| last_reminder_at | TIMESTAMP | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| token_deterministic | VARCHAR | |

**Relationships:**
- Belongs to → `enrollment_windows`, `customers`
- Has many → `enrollment_submissions`

---

### 9. `enrollment_submissions`

Employee self-enrollment form submissions.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| employee_id | UUID | FK → customers |
| enrollment_window_id | UUID | FK → enrollment_windows |
| invitation_id | UUID | FK → enrollment_invitations |
| endorsement_id | UUID | FK → endorsements |
| reference_number | VARCHAR | |
| status | ENUM | |
| plan_selections | JSONB | Chosen plans |
| nominee_data | JSONB | |
| premium_breakdown | JSONB | |
| submitted_at | TIMESTAMP | |
| reviewed_by | UUID | |
| reviewed_at | TIMESTAMP | |
| rejection_reason | VARCHAR | |
| declaration_accepted | BOOLEAN | |
| declaration_timestamp | TIMESTAMP | |
| declaration_ip_address | VARCHAR | |
| version | INTEGER | Optimistic locking |
| idempotency_key | VARCHAR | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| personal_details | JSONB | |
| dependents | JSONB | |
| stage | VARCHAR | |

**Relationships:**
- Belongs to → `customers`, `enrollment_windows`, `enrollment_invitations`, `endorsements`
- Has many → `nominees`

---

### 10. `nominees`

Beneficiaries linked to a policy or enrollment submission.

| Column | Type | Notes |
|--------|------|-------|
| **nominee_id** (PK) | UUID | |
| policy_id | UUID | FK → policies |
| first_name | VARCHAR | |
| last_name | VARCHAR | |
| full_name | VARCHAR | |
| date_of_birth | DATE | |
| gender | ENUM | |
| relationship | VARCHAR | |
| nominee_percentage | NUMERIC | |
| is_active | BOOLEAN | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| submission_id | UUID | FK → enrollment_submissions |
| customer_id | UUID | FK → customers |

**Relationships:**
- Belongs to → `policies`, `enrollment_submissions`, `customers`

---

### 11. `quotes`

Insurance quote requests from customers/leads.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| quote_id | VARCHAR | |
| coverage_amount | NUMERIC | |
| best_premium | NUMERIC | |
| status | ENUM | |
| created_date | TIMESTAMP | |
| customer_id | UUID | FK → customers |
| companies | JSONB | |
| features | JSONB | |

**Relationships:**
- Belongs to → `customers`
- Has many → `quote_companies`

---

### 12. `quote_companies`

Individual insurer quotes within a quote request.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| quote_id | UUID | FK → quotes |
| company_name | VARCHAR | |
| premium | NUMERIC | |
| coverage_amount | NUMERIC | |
| claim_ratio | NUMERIC | |
| key_features | JSONB | |
| recommended | BOOLEAN | |

**Relationships:**
- Belongs to → `quotes`

---

### 13. `insurance_providers`

Master list of insurance companies.

| Column | Type | Notes |
|--------|------|-------|
| **provider_id** (PK) | UUID | |
| provider_name | VARCHAR | |
| provider_code | VARCHAR | |
| is_active | BOOLEAN | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| product_type | VARCHAR | |

**Referenced by:** policies

---

## Schema: `admin` (Users & Auth)

---

### 14. `admin_users`

All system users (Vima admins, sales agents, HR admins, etc.).

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| username | VARCHAR | |
| email | VARCHAR | |
| full_name | VARCHAR | |
| role | ENUM | user_role_enum |
| is_active | BOOLEAN | |
| last_login | TIMESTAMP | |
| password_hash | VARCHAR | |
| auth_provider | VARCHAR | |
| auth_provider_id | VARCHAR | |
| created_at | TIMESTAMP | |
| zoho_crm_id | VARCHAR | |
| nonce | VARCHAR | |
| nonce_timestamp | TIMESTAMP | |
| nonce_expires_at | TIMESTAMP | |
| agent_id | UUID | Self-ref for agent hierarchy |
| reporting_to | UUID | FK → self (manager) |
| organization_id | UUID | FK → organizations |

**Relationships:**
- Belongs to → `organizations` (for HR Admins)
- Self-referencing → reporting_to (agent → manager)

---

## Feature Flags

---

### 15. `feature_flags`

Global feature toggle definitions.

| Column | Type | Notes |
|--------|------|-------|
| **flag_id** (PK) | UUID | |
| flag_key | VARCHAR | Unique key for lookup |
| description | VARCHAR | |
| parent_flag_id | UUID | FK → self (hierarchy) |
| is_active | BOOLEAN | |
| created_at | TIMESTAMP | |

**Relationships:**
- Self-referencing → parent_flag_id
- Has many → `feature_flag_companies`, `feature_flag_roles`

---

### 16. `feature_flag_companies`

Per-organization overrides for feature flags.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| flag_id | UUID | FK → feature_flags |
| organization_id | UUID | FK → organizations |
| is_active | BOOLEAN | |
| actions | JSONB | |

**Relationships:**
- Belongs to → `feature_flags`, `organizations`

---

### 17. `feature_flag_roles`

Per-role overrides for feature flags.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| flag_id | UUID | FK → feature_flags |
| role_name | VARCHAR | |
| is_active | BOOLEAN | |
| actions | JSONB | |

**Relationships:**
- Belongs to → `feature_flags`

---

## Vendor Integration

---

### 18. `vendors`

External API vendor registrations (e.g., insurer APIs).

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| name | VARCHAR | |
| base_url | VARCHAR | |
| type | VARCHAR | |
| active | BOOLEAN | |
| config | JSONB | |
| created_at | TIMESTAMP | |

**Has many:** `vendor_tokens`, `vendor_api_endpoints`

---

### 19. `vendor_tokens`

OAuth tokens for vendor API access.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| vendor_id | UUID | FK → vendors |
| access_token | TEXT | |
| refresh_token | TEXT | |
| expires_at | TIMESTAMP | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Belongs to:** `vendors`

---

### 20. `vendor_api_endpoints`

Registered API endpoints for each vendor.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| vendor_id | UUID | FK → vendors |
| api_key | VARCHAR | |
| path | VARCHAR | |
| method | VARCHAR | GET/POST/etc. |
| active | BOOLEAN | |
| description | VARCHAR | |
| created_at | TIMESTAMP | |

**Relationships:**
- Belongs to → `vendors`
- Has many → `vendor_api_headers`

---

### 21. `vendor_api_headers`

Custom headers for vendor API endpoints.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| endpoint_id | UUID | FK → vendor_api_endpoints |
| header_key | VARCHAR | |
| header_value | VARCHAR | |
| is_secret | BOOLEAN | |
| created_at | TIMESTAMP | |

**Belongs to:** `vendor_api_endpoints`

---

## Sales & Incentives

---

### 22. `agent_targets`

Monthly sales targets assigned to agents.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| agent_id | UUID | FK → admin_users |
| month | INTEGER | |
| year | INTEGER | |
| target_count | INTEGER | |
| package_id | UUID | FK → incentive_packages |
| created_at | TIMESTAMP | |

**Belongs to:** `admin_users`, `incentive_packages`

---

### 23. `incentive_packages`

Named incentive programs for sales agents.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| name | VARCHAR | |
| description | VARCHAR | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Has many:** `incentive_rules`, `agent_targets`

---

### 24. `incentive_rules`

Payout rules within an incentive package.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| package_id | UUID | FK → incentive_packages |
| fixed_payout | NUMERIC | |
| created_at | TIMESTAMP | |
| rule_type | VARCHAR | |

**Relationships:**
- Belongs to → `incentive_packages`
- Has many → `incentive_rule_slabs`

---

### 25. `incentive_rule_slabs`

Tiered payout slabs for slab-based incentive rules.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| rule_id | UUID | FK → incentive_rules |
| min_value | NUMERIC | |
| max_value | NUMERIC | |
| payout_amount | NUMERIC | |

**Belongs to:** `incentive_rules`

---

## External Integrations

---

### 26. `zoho_token`

Zoho CRM OAuth token storage.

| Column | Type | Notes |
|--------|------|-------|
| **id** (PK) | UUID | |
| access_token | TEXT | |
| refresh_token | TEXT | |
| expiry_time | TIMESTAMP | |

Standalone table — no foreign keys.

---

## Relationship Summary (Key Connections)

```
organizations
 ├── policies              (organization_id)
 ├── endorsements           (organization_id)
 ├── enrollment_windows     (organization_id)
 ├── customers/individuals  (organization_id)
 ├── admin_users            (organization_id)  — HR Admins
 └── feature_flag_companies (organization_id)

policies
 ├── motor_policy_details   (policy_id)
 ├── nominees               (policy_id)
 ├── insurance_providers    (insurance_provider_id)
 └── customers              (primary_individual_id)

endorsements
 ├── deal_endorsements      (endorsement_id)
 ├── enrollment_submissions (endorsement_id)
 └── enrollment_windows     (enrollment_window_id)

enrollment_windows
 ├── enrollment_invitations (enrollment_window_id)
 ├── enrollment_submissions (enrollment_window_id)
 └── endorsements           (enrollment_window_id)

customers (individuals)
 ├── policies               (primary_individual_id)
 ├── quotes                 (customer_id)
 ├── enrollment_invitations (employee_id)
 ├── enrollment_submissions (employee_id)
 ├── deal_endorsements      (individual_id)
 ├── nominees               (customer_id)
 └── self-ref: dependents   (primary_individual_id)

feature_flags
 ├── feature_flag_companies (flag_id)
 ├── feature_flag_roles     (flag_id)
 └── self-ref: hierarchy    (parent_flag_id)

vendors
 ├── vendor_tokens          (vendor_id)
 └── vendor_api_endpoints   (vendor_id)
      └── vendor_api_headers (endpoint_id)

incentive_packages
 └── incentive_rules        (package_id)
      └── incentive_rule_slabs (rule_id)

admin_users
 ├── agent_targets          (agent_id)
 └── self-ref: hierarchy    (reporting_to)
```

---

## Table Count Summary

| Category | Tables | Count |
|----------|--------|-------|
| **Core Business** | organizations, customers, policies, motor_policy_details, endorsements, deal_endorsements | 6 |
| **Enrollment** | enrollment_windows, enrollment_invitations, enrollment_submissions, nominees | 4 |
| **Quotes** | quotes, quote_companies | 2 |
| **Users & Auth** | admin_users | 1 |
| **Feature Flags** | feature_flags, feature_flag_companies, feature_flag_roles | 3 |
| **Vendor Integration** | vendors, vendor_tokens, vendor_api_endpoints, vendor_api_headers | 4 |
| **Sales & Incentives** | agent_targets, incentive_packages, incentive_rules, incentive_rule_slabs | 4 |
| **External** | insurance_providers, zoho_token | 2 |
| **Total** | | **26** |
