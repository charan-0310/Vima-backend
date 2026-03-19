# Postman – Phase 2 API URLs and Payloads

**Base URL:** `http://localhost:7220/dev` (or your server + context path)  
**Prefix:** `/api/v1`  
**Auth:** Use Bearer token (JWT) for admin endpoints in the `Authorization` header unless noted.

---

## 1. Cost-Sharing Rules

**Feature flag:** `enrollment.cost-sharing` must be enabled for your role.

Replace `{companyId}` with a valid organization UUID (e.g. `e212fcbe-26af-44ef-adeb-aa4c810e1c0d`).

| Method | URL | Description |
|--------|-----|-------------|
| GET | `{{baseUrl}}/api/v1/admin/companies/{companyId}/cost-sharing-rules` | List rules (optional: `?planType=GMC&effectiveDate=2025-01-01`) |
| GET | `{{baseUrl}}/api/v1/admin/companies/{companyId}/cost-sharing-rules/{ruleId}` | Get rule by ID |
| POST | `{{baseUrl}}/api/v1/admin/companies/{companyId}/cost-sharing-rules` | Create rule |
| PUT | `{{baseUrl}}/api/v1/admin/companies/{companyId}/cost-sharing-rules/{ruleId}` | Update rule |
| DELETE | `{{baseUrl}}/api/v1/admin/companies/{companyId}/cost-sharing-rules/{ruleId}` | Soft-delete rule |

### POST / PUT body (Cost-Sharing Rule)

```json
{
  "planType": "GMC",
  "coverageCategory": "SELF",
  "employerShareType": "PERCENTAGE",
  "employerShareValue": 80,
  "effectiveFrom": "2025-01-01",
  "effectiveTo": null,
  "excessAllowed": false
}
```

- **coverageCategory:** `SELF`, `SPOUSE`, `CHILD`, `PARENT`, `PARENT_IN_LAW`, `ALL_DEPENDENTS`
- **employerShareType:** `PERCENTAGE` or `FIXED_AMOUNT`
- **effectiveTo:** `null` for no end date

---

## 2. Top-Up Plan Options

**Feature flag:** `enrollment.topup-plans` must be enabled.

Replace `{companyId}` with a valid organization UUID.

| Method | URL | Description |
|--------|-----|-------------|
| GET | `{{baseUrl}}/api/v1/admin/companies/{companyId}/topup-options` | List options (optional: `?planType=TOP_UP`) |
| GET | `{{baseUrl}}/api/v1/admin/companies/{companyId}/topup-options/{id}` | Get option by ID |
| POST | `{{baseUrl}}/api/v1/admin/companies/{companyId}/topup-options` | Create option |
| PUT | `{{baseUrl}}/api/v1/admin/companies/{companyId}/topup-options/{id}` | Update option |
| DELETE | `{{baseUrl}}/api/v1/admin/companies/{companyId}/topup-options/{id}` | Soft-delete option |
| GET | `{{baseUrl}}/api/v1/enrollment/{token}/topup-options` | Employee: options with premium preview (no JWT; use enrollment token) |

### POST / PUT body (Top-Up Option)

```json
{
  "planType": "TOP_UP",
  "name": "Super Top-Up 5L",
  "description": "Super top-up cover",
  "insurerName": null,
  "deductibleAmount": 25000,
  "sumInsuredOptions": [500000, 1000000],
  "pricingModel": "FLAT",
  "coversDependents": true,
  "coversParents": false,
  "effectiveFrom": "2025-01-01",
  "effectiveTo": null,
  "policyId": null
}
```

- **planType:** `TOP_UP` or `SUPER_TOP_UP`
- **pricingModel:** `FLAT` or `AGE_BANDED`
- **policyId:** optional; use a valid `cpc.policies.policy_id` or `null`

---

## 3. Company Enrollment Config (Parent Coverage)

**Feature flag:** `enrollment.parent-coverage` must be enabled.

| Method | URL | Description |
|--------|-----|-------------|
| GET | `{{baseUrl}}/api/v1/admin/companies/{companyId}/enrollment-config` | Get config |
| PUT | `{{baseUrl}}/api/v1/admin/companies/{companyId}/enrollment-config` | Create or update config |

### PUT body (Enrollment Config)

```json
{
  "parentCoverageEnabled": true,
  "inLawCoverageEnabled": true,
  "maxParents": 2,
  "maxInLaws": 2,
  "parentAgeLimit": 65
}
```

---

## 4. Payroll Reports

**Feature flag:** `enrollment.payroll-reports` must be enabled.

| Method | URL | Description |
|--------|-----|-------------|
| GET | `{{baseUrl}}/api/v1/admin/reports/payroll-deductions?companyId={companyId}&windowId={windowId}&format=xlsx` | Download report (file) |
| GET | `{{baseUrl}}/api/v1/admin/reports/payroll-deductions/schedules?companyId={companyId}` | List schedules |
| POST | `{{baseUrl}}/api/v1/admin/reports/payroll-deductions/schedule` | Create schedule |
| DELETE | `{{baseUrl}}/api/v1/admin/reports/payroll-deductions/schedule/{scheduleId}` | Delete schedule |

### Query params (Download report)

- **companyId** (required): organization UUID
- **windowId** (optional): enrollment window UUID; omit for all schedules
- **format:** `xlsx` (default) or `csv`

### POST body (Create Payroll Report Schedule)

```json
{
  "companyId": "e212fcbe-26af-44ef-adeb-aa4c810e1c0d",
  "windowId": null,
  "frequency": "MONTHLY",
  "recipientEmails": ["hr@company.com", "finance@company.com"]
}
```

- **windowId:** optional enrollment window UUID
- **frequency:** e.g. `MONTHLY`
- **recipientEmails:** array of email addresses for the monthly report

---

## 5. Quick copy-paste URLs (replace `{companyId}` and IDs)

```
GET    http://localhost:7220/dev/api/v1/admin/companies/{companyId}/cost-sharing-rules
POST   http://localhost:7220/dev/api/v1/admin/companies/{companyId}/cost-sharing-rules
PUT    http://localhost:7220/dev/api/v1/admin/companies/{companyId}/cost-sharing-rules/{ruleId}
DELETE http://localhost:7220/dev/api/v1/admin/companies/{companyId}/cost-sharing-rules/{ruleId}

GET    http://localhost:7220/dev/api/v1/admin/companies/{companyId}/topup-options
POST   http://localhost:7220/dev/api/v1/admin/companies/{companyId}/topup-options
PUT    http://localhost:7220/dev/api/v1/admin/companies/{companyId}/topup-options/{id}
DELETE http://localhost:7220/dev/api/v1/admin/companies/{companyId}/topup-options/{id}

GET    http://localhost:7220/dev/api/v1/admin/companies/{companyId}/enrollment-config
PUT    http://localhost:7220/dev/api/v1/admin/companies/{companyId}/enrollment-config

GET    http://localhost:7220/dev/api/v1/admin/reports/payroll-deductions?companyId={companyId}&format=xlsx
GET    http://localhost:7220/dev/api/v1/admin/reports/payroll-deductions/schedules?companyId={companyId}
POST   http://localhost:7220/dev/api/v1/admin/reports/payroll-deductions/schedule
DELETE http://localhost:7220/dev/api/v1/admin/reports/payroll-deductions/schedule/{scheduleId}
```

---

## 6. Feature flags

If you get **403** with message like `Feature enrollment.xxx is not enabled`, enable the flag for your role:

- **enrollment.cost-sharing** – Cost-sharing rules
- **enrollment.topup-plans** – Top-up options (admin + employee preview)
- **enrollment.parent-coverage** – Company enrollment config (parent/in-law)
- **enrollment.payroll-reports** – Payroll deduction reports and schedules

Use your Feature Flag management API or DB to set the corresponding flag for `ROLE_VIMA_ADMIN` / `ROLE_HR_ADMIN` (or your role).
