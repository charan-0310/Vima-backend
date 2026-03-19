---
name: BE-03 Cost-Sharing Engine
overview: "Implement the cost-sharing system: enums, CostSharingRule entity/repository/cache, CRUD service with getEffectiveRule and applyCostSharing, deduction frequency and DeductionOptions, wiring into PremiumCalculationService, EnrollmentSubmission Phase 2 fields and DTOs, admin controller, and audit."
todos: []
isProject: false
---

# BE-03 Cost-Sharing Engine — Implementation Plan

## Dependencies and scope

- **Depends on:** BE-01 (Phase 2 migrations: `cpc.cost_sharing_rules` from V37, enrollment_submissions Phase 2 columns from V40), BE-02 (PremiumCalculationService, deduction options).
- **Schema:** [V37__create_cost_sharing_rules.sql](Vima-backend/src/main/resources/db/migration/V37__create_cost_sharing_rules.sql) already defines the table. [V40](Vima-backend/src/main/resources/db/migration/V40__modify_enrollment_tables_phase2.sql) added submission columns; the **entity** [EnrollmentSubmission](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/entity/EnrollmentSubmission.java) does not yet map them.

---

## 1. Enums

**Path:** [Vima-backend/src/main/java/com/vimainsurance/vimaadmin/enums/](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/enums/)

- **EmployerShareType.java** (new): `PERCENTAGE`, `FIXED_AMOUNT`. Follow existing enum style (e.g. [ProductType](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/enums/ProductType.java)) with `@JsonValue` / `@JsonCreator`, `getValue()`, `fromValue(String)`.
- **CoverageCategory.java** (new): `SELF`, `SPOUSE`, `CHILD`, `PARENT`, `PARENT_IN_LAW`, `ALL_DEPENDENTS`. Same pattern.
- **DeductionFrequency.java** (new): `MONTHLY`, `QUARTERLY`, `YEARLY`. Same pattern.

---

## 2. Entity: CostSharingRule

**Path:** `entity/CostSharingRule.java` (new)

- Map to `cpc.cost_sharing_rules`: `id` (UUID), `company_id` → `organizationId` (UUID), `plan_type`, `coverage_category`, `employer_share_type`, `employer_share_value`, `excess_allowed`, `effective_from`, `effective_to`, `is_deleted`, `created_at`, `updated_at`.
- **Soft delete:** `@Where(clause = "is_deleted = false")` (same as [PremiumRateTable](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/entity/PremiumRateTable.java)).
- **@ManyToOne(fetch = FetchType.LAZY)** to `Organization` on `company_id` (or keep as UUID and add optional `@ManyToOne` for admin use). Use `@JoinColumn(name = "company_id", referencedColumnName = "organization_id")`. Table uses `company_id`; entity can use `Organization company` or `UUID organizationId` — use `UUID organizationId` for consistency with repository queries and cache keying; optional `@ManyToOne` can be added later.
- Use `@Enumerated(EnumType.STRING)` for `employerShareType`; `coverage_category` and `plan_type` stored as VARCHAR (String or enum CoverageCategory). Use enums where applicable.
- Lombok, `@Table(schema = "cpc", name = "cost_sharing_rules")`, `@CreationTimestamp` / `@UpdateTimestamp`.

---

## 3. Repository: ICostSharingRuleRepository

**Path:** `repository/ICostSharingRuleRepository.java` (new)

- Extend `JpaRepository<CostSharingRule, UUID>` and optionally `JpaSpecificationExecutor<CostSharingRule>` for filtered list.
- **findByCompanyIdAndPlanType**(UUID companyId, String planType): return list of rules for company + plan (effective-date filter in service or via custom query).
- **findByCompanyIdAndPlanTypeAndCoverageCategory**(UUID companyId, String planType, String coverageCategory): same with category.
- **findActiveRulesForCompany**(UUID companyId, LocalDate effectiveDate): JPQL that returns rules where `organizationId = :companyId` AND `effectiveFrom <= :date` AND (`effectiveTo IS NULL` OR `effectiveTo >= :date`) AND `isDeleted = false`. For soft-deleted exclusion with `@Where`, normal `findBy` will exclude deleted; for "active on date" use a single `@Query` with the date predicates.
- **softDeleteById**(UUID id): `@Modifying` `@Query` to set `isDeleted = true` (same pattern as [IPremiumRateTableRepository.softDeleteById](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/repository/IPremiumRateTableRepository.java)).

---

## 4. DTOs

**Path:** [Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/)

- **CostSharingRuleRequestDto:** companyId (UUID), planType, coverageCategory, employerShareType, employerShareValue (BigDecimal), effectiveFrom, effectiveTo, excessAllowed (Boolean). Validation: employerShareType PERCENTAGE ⇒ value 0–100.
- **CostSharingRuleResponseDto:** same fields plus id, createdAt, updatedAt.
- **CostShareSplit** (class or record): employerShare (BigDecimal), employeeShare (BigDecimal), shareType (String or enum), shareValue (BigDecimal), ruleId (UUID). Used as the result of applyCostSharing.
- **DeductionOptions** (expand or formalize): currently [PremiumCalculationResponseDto](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/PremiumCalculationResponseDto.java) has `Map<String, BigDecimal> deductionOptions`. Add a small DTO or keep map with keys MONTHLY, QUARTERLY, YEARLY; document that values are per-period amounts (rounded to nearest rupee). Optionally add a DeductionOptionsDto with fields like annualEmployeeShare, monthlyAmount, quarterlyAmount, yearlyAmount, selectedFrequency.

---

## 5. Mapper

**Path:** `mapper/CostSharingRuleMapper.java` (new)

- Static: `toEntity(CostSharingRuleRequestDto)`, `toResponseDto(CostSharingRule)`. Map all fields including enums.

---

## 6. Service: ICostSharingRuleService + Impl

**Path:** `service/ICostSharingRuleService.java`, `serviceimpl/CostSharingRuleServiceImpl.java`

**CRUD:** create, update, soft-delete, getById, list by company (with optional filters: planType, effective date). After create/update/delete, **invalidate cost-sharing cache** for that companyId.

**Core logic:**

- **getEffectiveRule(companyId, planType, coverageCategory, date):** Load active rules for company (from cache) filtered by planType and effective date. Precedence: rule with exact `coverageCategory` first; if none, use rule with `coverageCategory = ALL_DEPENDENTS`. Return null if no rule (caller treats as 100% employer).
- **applyCostSharing(companyId, planType, coverageCategory, totalPremium):** Get effective rule. If none → return CostShareSplit(employerShare = totalPremium, employeeShare = 0, no ruleId). If PERCENTAGE: employerShare = totalPremium × (value/100), employeeShare = totalPremium - employerShare. If FIXED_AMOUNT: employerShare = min(value, totalPremium), employeeShare = totalPremium - employerShare (never negative). Round to 2 decimals. GST: split is applied to **total premium (base + GST)** as per ticket.
- **calculateDeductions(annualEmployeeShare, frequency):** Return per-period amount: MONTHLY → annual/12, QUARTERLY → annual/4, YEARLY → annual. Round to nearest rupee (e.g. `setScale(0, RoundingMode.HALF_UP)`). For proration (mid-year): accept optional proratedAnnual and divide by same divisor; document that callers pass prorated annual when needed.

Expose **calculateDeductions(BigDecimal annualEmployeeShare)** returning a structure (map or DTO) with MONTHLY, QUARTERLY, YEARLY amounts so that deduction options in the premium response can be filled from cost-sharing engine.

---

## 7. In-memory cache: CostSharingRuleCacheService

**Path:** `service/CostSharingRuleCacheService.java` (new)

- Same pattern as [PremiumRateTableCacheService](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/service/PremiumRateTableCacheService.java): `ConcurrentHashMap<UUID, CacheEntry>` keyed by companyId, TTL 1 hour, **getRulesForCompany(UUID companyId)**, **invalidate(UUID companyId)**, **@Scheduled(fixedRate = 3600000)** hourly refresh. CacheEntry holds `List<CostSharingRule>` and timestamp. Load via repository method that returns active rules (effective date = today and not deleted); or load all non-deleted for company and filter by date in service.

---

## 8. Wire into PremiumCalculationService

**Path:** [PremiumCalculationServiceImpl](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl/PremiumCalculationServiceImpl.java)

- In **calculateEnrollmentPremium**: After computing plan premium (e.g. from `calculatePlanPremium`), for each plan determine **coverage category** (e.g. SELF for employee-only; or derive from dependents — if plan covers family, aggregate by category or use a single category for the plan). Call **costSharingRuleService.applyCostSharing(companyId, planType, coverageCategory, planTotalPremium)** to get employer/employee split instead of the current hardcoded 70/30. Use the returned employerShare/employeeShare for that plan's breakdown and for aggregating totalEmployerShare and totalEmployeeShare.
- **Deduction options:** Use **costSharingRuleService.calculateDeductions(totalEmployeeShare)** (or prorated total employee share) to build deductionOptions: MONTHLY, QUARTERLY, YEARLY with rounded per-period amounts. Replace or extend the current `deductionOptions.put("MONTHLY", ...)` logic so that deduction amounts are based on **employee share** and frequency divisors.
- Ensure **total premium** (base + GST) is what is passed to applyCostSharing so that split is on the full amount.

---

## 9. EnrollmentSubmission entity and DTOs

**Path:** [EnrollmentSubmission](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/entity/EnrollmentSubmission.java), [EnrollmentSubmissionResponseDto](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/EnrollmentSubmissionResponseDto.java), [EnrollmentSubmissionRequestDto](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/EnrollmentSubmissionRequestDto.java), [EnrollmentSubmissionMapper](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/mapper/EnrollmentSubmissionMapper.java)

- **Entity:** Add columns (already in DB via V40): `deductionFrequency` (String, length 20), `totalEmployeeAnnualPremium` (BigDecimal), `totalEmployerAnnualPremium` (BigDecimal), `costSharingSnapshot` (String, JSONB / `@JdbcTypeCode(SqlTypes.JSON)`), `deductionAmountPerPeriod` (BigDecimal), `consentTimestamp` (LocalDateTime), `consentTextSnapshot` (String, TEXT). Match DB names: `deduction_frequency`, `total_employee_annual_premium`, `total_employer_annual_premium`, `cost_sharing_snapshot`, `deduction_amount_per_period`, `consent_timestamp`, `consent_text_snapshot`.
- **EnrollmentSubmissionResponseDto / RequestDto:** Add same fields so submission read/update and submit flow can persist and return them.
- **EnrollmentSubmissionMapper:** In `mapToEntity`, `updateEntityFromDto`, and entity-to-response mapping, include the new fields. When saving a submission (e.g. on submit or when saving premium breakdown), set cost-sharing snapshot from the calculated breakdown (immutable JSONB snapshot), deduction frequency, totals, deduction amount per period, and when consent is given: consentTimestamp and consentTextSnapshot.

---

## 10. Controller: CostSharingRuleController

**Path:** `controller/CostSharingRuleController.java` (new)

- Base path: **GET/POST** ` /api/v1/admin/companies/{companyId}/cost-sharing-rules`, **PUT/DELETE** `.../cost-sharing-rules/{ruleId}`.
- **GET** (list): return list (or page) of CostSharingRuleResponseDto for company; optional query params: planType, effectiveDate.
- **POST** (create): body CostSharingRuleRequestDto; set companyId from path; validate; create; return 201 + body.
- **PUT** (update): body CostSharingRuleRequestDto; update existing rule by ruleId; companyId in path must match rule's company.
- **DELETE** (soft delete): soft-delete by ruleId; return 200.
- **@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")** on class or each method.
- Use [BaseResponse](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/BaseResponse.java) / ResponseDto and ResponseEntity like [EmployeePolicyMapController](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/controller/EmployeePolicyMapController.java).

---

## 11. Audit

- On cost-sharing rule **create, update, soft-delete**, add **@AuditedOperation**(schemaName = "cpc", tableName = "cost_sharing_rules", entityType = "COST_SHARING_RULE", action = "CREATE" | "UPDATE" | "DELETE"). Same pattern as [EmployeePolicyMapServiceImpl](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl/EmployeePolicyMapServiceImpl.java).

---

## 12. Acceptance criteria mapping

| Criterion | Implementation |
|-----------|----------------|
| Percentage 80/20 on Rs. 11,800 → Employer 9,440 / Employee 2,360 | applyCostSharing with PERCENTAGE 80; unit test or manual test. |
| Fixed amount capped at total premium, no negative employee share | employerShare = min(value, totalPremium); employeeShare = totalPremium - employerShare. |
| No rules → 100% employer | getEffectiveRule returns null → applyCostSharing returns (totalPremium, 0). |
| Rule precedence: specific category beats ALL_DEPENDENTS | getEffectiveRule: first match exact coverageCategory, else ALL_DEPENDENTS. |
| Deduction frequency: monthly/quarterly/yearly, rounding | calculateDeductions: divide annual by 12/4/1; round to nearest rupee. |
| Submission stores cost_sharing_snapshot as JSONB | Entity + mapper; on submit/save, set costSharingSnapshot from response. |
| Consent timestamp + text snapshot | Entity fields consentTimestamp, consentTextSnapshot; set when user accepts consent. |
| Admin CRUD + authorization | Controller with @PreAuthorize; companyId from path. |
| Audit for config changes | @AuditedOperation on create/update/delete in CostSharingRuleServiceImpl. |

---

## File list (new/updated)

| Item | File |
|------|------|
| Enums | `enums/EmployerShareType.java`, `enums/CoverageCategory.java`, `enums/DeductionFrequency.java` |
| Entity | `entity/CostSharingRule.java` |
| Repository | `repository/ICostSharingRuleRepository.java` |
| DTOs | `dto/CostSharingRuleRequestDto.java`, `dto/CostSharingRuleResponseDto.java`, `dto/CostShareSplit.java` (or record); optionally `DeductionOptionsDto` |
| Mapper | `mapper/CostSharingRuleMapper.java` |
| Cache | `service/CostSharingRuleCacheService.java` |
| Service | `service/ICostSharingRuleService.java`, `serviceimpl/CostSharingRuleServiceImpl.java` |
| Wire | `PremiumCalculationServiceImpl` (inject ICostSharingRuleService; use applyCostSharing per plan; use calculateDeductions for deductionOptions) |
| Submission | `entity/EnrollmentSubmission.java` (add Phase 2 fields), `EnrollmentSubmissionResponseDto`, `EnrollmentSubmissionRequestDto`, `EnrollmentSubmissionMapper` |
| Controller | `controller/CostSharingRuleController.java` |

Implement in order: enums → entity → repository → cache → DTOs/mapper → service (CRUD + getEffectiveRule/applyCostSharing/calculateDeductions) → wire into premium calculation → submission entity/DTOs/mapper → controller. Audit and cache invalidation in the service layer.
