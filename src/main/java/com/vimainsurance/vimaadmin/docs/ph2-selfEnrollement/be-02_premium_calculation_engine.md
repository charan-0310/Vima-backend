---
name: BE-02 Premium Calculation Engine
overview: "Implement the full premium calculation engine: enums, PremiumRateTable entity and repository, DTOs, mapper, premium calculation service (with employee_policy_map integration), rate table CRUD with CSV upload, in-memory cache, controller (employee and admin), and audit."
todos: []
isProject: false
---

# BE-02 Premium Calculation Engine — Implementation Plan

## Dependency and scope

- **Depends on:** BE-00 (employee_policy_map), BE-01 (Phase 2 migrations V37–V43).
- **Key rule:** `lookupRate()` and `calculatePlanPremium()` must resolve policy/sum_insured/coverage_tier from **employee_policy_map** for the employee (not org-level defaults).

---

## 1. Enums

**Path:** [Vima-backend/src/main/java/com/vimainsurance/vimaadmin/enums/](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/enums/)

- **PricingModel.java** (new): `FLAT`, `AGE_BANDED`, `FAMILY_FLOATER`. Follow existing enum style (e.g. [ProductType.java](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/enums/ProductType.java)) with `@JsonValue`/`@JsonCreator`, `getValue()`, `fromValue(String)`.
- **RateSource.java** (new): `INSURER_CARD`, `NEGOTIATED`. Same pattern.

---

## 2. Entity: PremiumRateTable

**Path:** `entity/PremiumRateTable.java` (new)

- Map to `cpc.premium_rate_tables` (columns from [V39__modify_premium_rate_tables_phase2.sql](Vima-backend/src/main/resources/db/migration/V39__modify_premium_rate_tables_phase2.sql)): `id` (UUID), `organization_id`, `policy_id` (Long), `product_type`, `member_type`, `age_band_min`, `age_band_max`, `rate`, `effective_from`, `effective_to`, `created_at`, `updated_at`, plus Phase 2: `pricing_model`, `sum_insured_amount`, `family_size_min`, `family_size_max`, `rate_source`, `gst_inclusive`, `gst_percentage`, `is_deleted`.
- Use `@Enumerated(EnumType.STRING)` for `pricing_model` and `rate_source` (store as VARCHAR; enums above).
- **Soft delete:** `@Where(clause = "is_deleted = false")` on the entity (or `@SQLRestriction("is_deleted = false")` for Hibernate 6) so reads exclude deleted rows by default.
- Use `@Table(schema = "cpc", name = "premium_rate_tables")`, Lombok, and `@CreationTimestamp`/`@UpdateTimestamp` for audit fields. Reference `Organization` (organization_id) and `Policy` (policy_id) if you add `@ManyToOne`; otherwise keep as UUID/Long for simplicity.

---

## 3. Repository: IPremiumRateTableRepository

**Path:** `repository/IPremiumRateTableRepository.java` (new)

- Extend `JpaRepository<PremiumRateTable, UUID>` and `JpaSpecificationExecutor<PremiumRateTable>`.
- **findByCompanyIdAndPlanTypeAndPricingModel**(UUID companyId, String planType, PricingModel pricingModel): filter by organization_id, product_type (or plan_type column if different), pricing_model, effective date range (effective_from <= today, effective_to is null or >= today), and non-deleted. Return `List<PremiumRateTable>`.
- **findByCompanyIdAndPlanTypeAndMemberTypeAndAgeBandRange**(UUID companyId, String planType, String memberType, int ageMin, int ageMax): for age-banded; match organization_id, product_type, member_type, and age in [age_band_min, age_band_max) (or equivalent band logic). Return list or optional first.
- **findByCompanyIdAndPlanTypeAndFamilySizeRange**(UUID companyId, String planType, int familySize): match organization_id, product_type, and family_size_min <= familySize <= family_size_max. Return list or optional first.

Use `@Query` or specifications; ensure effective-date and `is_deleted` filtering. If the table uses `product_type` for plan (GMC, GTL, etc.), use that as "plan type".

---

## 4. DTOs

**Path:** [Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/)

- **PremiumRateTableRequestDto** – fields for create/update: companyId (UUID), policyId (Long), productType/planType, memberType, ageBandMin/Max, rate, effectiveFrom/To, pricingModel, sumInsuredAmount, familySizeMin/Max, rateSource, gstInclusive, gstPercentage. Validation where needed.
- **PremiumRateTableResponseDto** – same plus id, createdAt, updatedAt; used for list/get/response.
- **PremiumCalculationRequestDto** – planSelections (list of { planType, opted, sumInsured?, coverageTier?, … }), dependents (list of { name, relationship, dateOfBirth }). Align with employee flow; employeeId/context can come from token.
- **PremiumCalculationResponseDto** – totalAnnualPremium, totalEmployerShare, totalEmployeeShare, gstAmount, perPlanBreakdown (list of { planType, premium, employerShare, employeeShare, … }), deductionOptions (e.g. by frequency).
- **PremiumPreviewRequestDto** / **PremiumPreviewResponseDto** – admin calculator: inputs (company, plan, members/dependents, sum insured, etc.); response includes rate source and matched age band (for acceptance criteria).

Use Lombok and consistent naming with existing DTOs ([BaseResponse](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/BaseResponse.java), [ResponseDto](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/ResponseDto.java)).

---

## 5. Mapper

**Path:** `mapper/PremiumRateTableMapper.java` (new)

- Static methods: `toEntity(PremiumRateTableRequestDto)`, `toResponseDto(PremiumRateTable)` (and optionally toRequestDto for edits). Map all fields including enums and Phase 2 columns.

---

## 6. Service: IPremiumCalculationService + Impl

**Path:** `service/IPremiumCalculationService.java`, `serviceimpl/PremiumCalculationServiceImpl.java`

**Core methods:**

- **BigDecimal lookupRate(UUID companyId, String planType, String memberType, int memberAge, String coverageTier, BigDecimal sumInsured)**  
  - Resolve which rate row to use based on pricing model (flat vs age-banded vs family floater). Use repository methods above. Return rate or throw clear exception if no rate found ("No rate found for …").

- **PlanPremiumBreakdown calculatePlanPremium(UUID companyId, String planType, String coverageTier, BigDecimal sumInsured, List<MemberInfo> coveredMembers)**  
  - MemberInfo: at least age (and optionally DOB for consistency). Use employee_policy_map–derived sumInsured and coverageTier when called in enrollment context (see below). Dispatch by pricing model: flat → single lookup; age-banded → sum of per-member lookups (age bands [18,26), [26,36), [36,46), [46,56), [56,66), [66,76)); family floater → family size + eldest age lookup. Apply GST (inclusive/exclusive) from rate row. Return breakdown (premium, employer share, employee share, gst, etc.).

- **PremiumCalculationResponse calculateEnrollmentPremium(EnrollmentContext context, List<PlanSelectionRequest> planSelections, List<DependentInfo> dependents)**  
  - **Employee–policy mapping:** For the employee in `context`, call [IEmployeePolicyMapService](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/service/IEmployeePolicyMapService.java) (e.g. getMappingsForEmployeeFamily(employeeId)). Use the active mapping(s) to get sum_insured and coverage_tier per plan (or default if not present). Do **not** use org-level defaults when a mapping exists.  
  - Build "covered members" (employee + dependents with ages from DOB). For each plan selection, call calculatePlanPremium with the mapping-derived sumInsured and coverageTier. Aggregate totals, apply cost-sharing if needed (from cost_sharing_rules later or stub), compute deductionOptions (e.g. by frequency). Proration: annual_premium × (remaining_months / 12), minimum 1 month (use context.windowStartDate/windowEndDate or submission date).  
  - Return PremiumCalculationResponseDto shape.

**Calculation rules:**

- **Age bands:** [18,26), [26,36), [36,46), [46,56), [56,66), [66,76). Age from DOB: use `Period.between(dob, LocalDate.now()).getYears()`; handle leap year (LocalDate is safe).
- **Proration:** remaining months from today (or enrollment start) to period end; min 1 month; multiply annual premium by (months/12).
- **GST:** if gst_inclusive, derive base from rate; else add gst_percentage to rate. Expose gst amount in breakdown.

Introduce small DTOs/types: `MemberInfo`, `PlanSelectionRequest`, `DependentInfo`, `PlanPremiumBreakdown` (can be inner or separate classes) as needed for the above signatures.

---

## 7. Rate Table CRUD Service

**Path:** `service/IPremiumRateTableService.java`, `serviceimpl/PremiumRateTableServiceImpl.java`

- **CRUD:** create (from PremiumRateTableRequestDto), update (by id), soft-delete (set is_deleted = true), get by id, list with filters (company, plan type, pricing model, effective date). Use repository + specifications. After create/update/delete (and CSV bulk save), **invalidate cache** for that companyId (call cache service).
- **CSV bulk upload:** Parse CSV (headers: align with PremiumRateTable columns), validate each row (plan type, pricing model, age bands, dates, numbers). Return per-row errors (e.g. list of { rowIndex, message }) and only persist valid rows in a batch (e.g. saveAll). Target: 1000 rows < 5s. Return summary (inserted count, error count, list of errors).

---

## 8. In-Memory Cache

**Path:** `service/PremiumRateTableCacheService.java` (new)

- **ConcurrentHashMap<UUID, List<PremiumRateTable>>** keyed by companyId (organization_id).  
- Methods: `getRatesForCompany(UUID companyId)` – if absent or stale, load from repository (by organization_id, effective, not deleted) and put in map.  
- **TTL 1 hour:** store timestamp per key; on get, if older than 1 hour, reload.  
- **Invalidate(companyId):** remove key so next get refetches.  
- **@Scheduled hourly:** refresh all keys (or clear and lazy-refill). Call from [PremiumRateTableServiceImpl](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl/) on mutations and from premium calculation when looking up rates (prefer cache first, then DB).

---

## 9. Controller

**Path:** `controller/PremiumCalculationController.java` (new)

- **Employee (token-based):**  
  - **POST /api/v1/enrollment/{token}/calculate-premium**  
  - Validate token (reuse [IEnrollmentTokenService](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/service/IEnrollmentTokenService.java) or IEnrollmentService to get context). Build EnrollmentContext from validated token. Request body: PremiumCalculationRequestDto (planSelections, dependents). Call premiumCalculationService.calculateEnrollmentPremium(context, …). Return PremiumCalculationResponseDto.  
  - **Rate limit:** 100 req/min per token. Use [@RateLimit](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/annotation/RateLimit.java) with `limit = 100, periodMinutes = 1`. Current [RateLimitAspect](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/aspect/RateLimitAspect.java) is per-IP; extend or add a second aspect/keyed limiter by token (e.g. rate limit key = token) so limit is per token. Document in plan if implementation uses token as key.

- **Admin:**  
  - **Preview:** POST `/api/v1/admin/premium/preview` (or under existing admin prefix) – body PremiumPreviewRequestDto, return PremiumPreviewResponseDto with rate source and matched age band.  
  - **Rate table CRUD:** GET/POST/PUT/DELETE for rate tables (e.g. by company, id).  
  - **CSV upload:** POST multipart/file for bulk upload; return validation result + per-row errors.

Use [BaseResponse](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/BaseResponse.java)/ResponseDto and ResponseEntity as in [EnrollmentController](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/controller/EnrollmentController.java). Secure admin endpoints with existing admin auth.

---

## 10. Audit

- On rate table **create/update/delete** (and bulk CSV create), add **@AuditedOperation**(schemaName = "cpc", tableName = "premium_rate_tables", entityType = "PREMIUM_RATE_TABLE", action = "CREATE" | "UPDATE" | "DELETE"). Follow [EmployeePolicyMapServiceImpl](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl/EmployeePolicyMapServiceImpl.java) pattern. Ensure audit events are written to audit.audit_events (existing aspect handles this).

---

## 11. Wiring and config

- Register new services and cache in Spring (component scan). Ensure PremiumCalculationController is under `/api/v1/enrollment` for the employee endpoint and under admin path for admin endpoints. Add any new admin path to [SecurityConfig](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/config/SecurityConfig.java) if required.

---

## 12. Acceptance criteria checklist (implementation targets)

| Criterion | Implementation |
|-----------|----------------|
| Flat rate: correct premium, clear error for missing rate | lookupRate returns or throws; controller returns 4xx with message |
| Age-banded: 25→[18,26), 26→[26,36); leap year DOB | Age computed with Period.between; bands implemented as half-open intervals |
| Family floater: family size + eldest age | findByCompanyIdAndPlanTypeAndFamilySizeRange + eldest age in lookup |
| Proration: mid-year, min 1 month | remaining_months/12 in calculateEnrollmentPremium |
| GST: inclusive/exclusive, breakdown | gst_inclusive/gst_percentage from rate row; expose in response |
| CSV: validate, per-row errors, 1000 rows < 5s | Validation loop + batch saveAll; benchmark |
| Cache: first from DB, then cache; mutation invalidates | Cache service get/invalidate; CRUD and CSV call invalidate |
| Employee API < 200ms with cache hit | Use cache in lookup path |
| Admin preview: rate source + matched age band | PremiumPreviewResponseDto fields; set in service |
| Mutations create audit events | @AuditedOperation on CRUD and bulk |
| Premium from employee_policy_map | getMappingsForEmployeeFamily in calculateEnrollmentPremium; use sum_insured, coverage_tier |

---

## File list (new/updated)

| Item | File |
|------|------|
| Enums | `enums/PricingModel.java`, `enums/RateSource.java` |
| Entity | `entity/PremiumRateTable.java` |
| Repository | `repository/IPremiumRateTableRepository.java` |
| DTOs | `dto/PremiumRateTableRequestDto.java`, `PremiumRateTableResponseDto.java`, `PremiumCalculationRequestDto.java`, `PremiumCalculationResponseDto.java`, `PremiumPreviewRequestDto.java`, `PremiumPreviewResponseDto.java` (+ small types: MemberInfo, PlanSelectionRequest, DependentInfo, PlanPremiumBreakdown as needed) |
| Mapper | `mapper/PremiumRateTableMapper.java` |
| Services | `service/IPremiumCalculationService.java`, `serviceimpl/PremiumCalculationServiceImpl.java`, `service/IPremiumRateTableService.java`, `serviceimpl/PremiumRateTableServiceImpl.java`, `service/PremiumRateTableCacheService.java` |
| Controller | `controller/PremiumCalculationController.java` |
| Rate limit | Extend rate limiting for 100 req/min per token (new aspect or key by token) |

No changes to the plan file itself; implement in the order above so that entity/repository/cache and DTOs are in place before the calculation and CRUD services and controller.
