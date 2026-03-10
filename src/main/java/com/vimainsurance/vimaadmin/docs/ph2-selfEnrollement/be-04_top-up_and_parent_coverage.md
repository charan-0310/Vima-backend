---
name: BE-04 Top-Up and Parent Coverage
overview: Implement backend for top-up/super top-up plan options (entity, repo, CRUD, employee API with premium preview, employee_policy_map on opt-in) and parent/in-law coverage (CompanyEnrollmentConfig entity, premium and cost-sharing extensions, age limit validation, Scenario 2 policy mappings). Includes EnrollmentPlanSelection entity and audit.
todos: []
isProject: false
---

# BE-04 Top-Up Plans + Parent Coverage Backend — Implementation Plan

## Dependencies and scope

- **Depends on:** BE-00 (employee_policy_map), BE-02 (premium engine), BE-03 (cost-sharing).
- **DB:** Tables already exist from BE-01: [V38 topup_plan_options](Vima-backend/src/main/resources/db/migration/V38__create_topup_plan_options.sql), [V41 company_enrollment_config](Vima-backend/src/main/resources/db/migration/V41__modify_company_enrollment_config_phase2.sql), [V40 enrollment_plan_selections](Vima-backend/src/main/resources/db/migration/V40__modify_enrollment_tables_phase2.sql). No new migrations required.
- **Scope:** Top-up options admin CRUD + employee-facing options with premium preview; parent/in-law config and premium/cost-sharing/validation; optional persistence of plan selections in `enrollment_plan_selections`; creation of employee_policy_map for top-up and parent (Scenario 2) opt-in.

---

## 1. Top-Up Plan Options

### 1.1 Entity: TopupPlanOption

**Path:** `entity/TopupPlanOption.java` (new)

- Map to `cpc.topup_plan_options`: id (UUID), company_id, policy_id (Long), plan_type, name, description, insurer_name, deductible_amount, sum_insured_options (JSONB), pricing_model, covers_dependents, covers_parents, is_active, effective_from, effective_to, is_deleted, created_at, updated_at.
- **sum_insured_options:** Store as String (JSON array of numbers) with `@JdbcTypeCode(SqlTypes.JSON)` or a converter; backend can parse to `List<BigDecimal>` in service layer.
- **@ManyToOne(fetch = LAZY)** to Organization (company_id) and Policy (policy_id) optional; can use UUID companyId and Long policyId for simplicity and cache keying.
- **Soft delete:** `@Where(clause = "is_deleted = false")` (same as [CostSharingRule](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/entity/CostSharingRule.java)).
- plan_type: String; CHECK in DB is TOP_UP | SUPER_TOP_UP. pricing_model: AGE_BANDED | FLAT.

### 1.2 Repository: ITopupPlanOptionRepository

**Path:** `repository/ITopupPlanOptionRepository.java` (new)

- Extend `JpaRepository<TopupPlanOption, UUID>`.
- **findByCompanyIdAndIsActiveTrue**(UUID companyId): return list where company_id = ? and is_active = true and not deleted (or rely on @Where).
- **findByCompanyIdAndPlanType**(UUID companyId, String planType): filter by company and plan_type.
- **findByCompanyId**(UUID companyId): for cache/admin list.
- **softDeleteById**(UUID id): `@Modifying` UPDATE set is_deleted = true.

### 1.3 DTOs and mapper

- **TopupPlanOptionRequestDto:** companyId, policyId, planType, name, description, insurerName, deductibleAmount, sumInsuredOptions (List or String JSON), pricingModel, coversDependents, coversParents, effectiveFrom, effectiveTo. Validation: planType in (TOP_UP, SUPER_TOP_UP), pricingModel in (AGE_BANDED, FLAT).
- **TopupPlanOptionResponseDto:** same + id, isActive, createdAt, updatedAt.
- **TopupOptionsResponseDto** (employee): list of options, each with id, planType, name, deductibleAmount, sumInsuredOptions, and **premiumPreviewPerOption** (e.g. Map<BigDecimal, BigDecimal> sumInsured → annual premium) for the current employee/context.
- **Mapper:** TopupPlanOptionMapper.toEntity, toResponseDto; serialize sumInsuredOptions to/from JSON string.

### 1.4 Service: ITopupPlanOptionService + TopupPlanOptionServiceImpl

- **Admin CRUD:** create, update, soft-delete, getById, listByCompany(companyId, planType optional). Invalidate cache on mutation if cache added.
- **Employee:** getActiveOptionsForCompany(companyId) returning list of active options; **getActiveOptionsWithPremiumPreview(companyId, enrollmentToken or context)** that for each option and each sum_insured in sumInsuredOptions calls PremiumCalculationService (e.g. calculatePlanPremium or a dedicated helper) for the employee (and optionally dependents) and returns TopupOptionsResponseDto with premium per SI option.
- **Business rules:** Enforce "base GMC required before top-up" in service or controller (e.g. require employee to have at least one GMC mapping before showing top-up). Enforce "max 1 top-up + 1 super top-up per employee" when saving selections (e.g. in enrollment submit or plan-selection save).
- **On employee opt-in for top-up:** When enrollment submission is saved/submitted with a top-up selection, call IEmployeePolicyMapService (createMapping or a new method createMappingsForTopupOptIn) to create employee_policy_map row(s): individualId = employee (and optionally dependents), policyId = topup_plan_option.policy_id, organizationId = companyId, sumInsured = chosen SI, isVoluntary = true, relationship = SELF / dependent relationship, effectiveFrom = window or today, source = ENROLLMENT. Use [EmployeePolicyMapRequestDto](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/dto/EmployeePolicyMapRequestDto.java) or a bulk DTO.

### 1.5 Controller

- **Admin:** GET/POST/PUT/DELETE under `/api/v1/admin/companies/{companyId}/topup-options` (same pattern as [CostSharingRuleController](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/controller/CostSharingRuleController.java)). POST body TopupPlanOptionRequestDto; set companyId from path. @PreAuthorize same roles.
- **Employee:** GET `/api/v1/enrollment/{token}/topup-options` — validate token, get context (companyId, employeeId), return TopupOptionsResponseDto with premium preview. Public (token-based) like other enrollment endpoints.

### 1.6 Cache (optional)

- TopupPlanOptionCacheService keyed by companyId, 1h TTL, invalidate on CRUD; use for getActiveOptions to avoid repeated DB hits.

---

## 2. Parent / In-Law Coverage

### 2.1 CompanyEnrollmentConfig entity and repository

**Path:** `entity/CompanyEnrollmentConfig.java` (new)

- Map to `cpc.company_enrollment_config`: id, organization_id, created_at, updated_at, **parent_coverage_enabled**, **in_law_coverage_enabled**, **max_parents**, **max_in_laws**, **parent_age_limit**. Unique on organization_id.
- **Repository:** ICompanyEnrollmentConfigRepository — findByOrganizationId(UUID), save. Optional: findById.

### 2.2 Config DTOs and API

- **CompanyEnrollmentConfigRequestDto / ResponseDto:** organizationId, parentCoverageEnabled, inLawCoverageEnabled, maxParents, maxInLaws, parentAgeLimit.
- **API:** GET/PUT `/api/v1/admin/companies/{companyId}/enrollment-config` to read and update config. Create if not exists on first GET (defaults) or first PUT. @PreAuthorize same as other admin company endpoints.
- Include parent/in-law fields in response so frontend and premium flow can enforce limits.

### 2.3 Extend PremiumCalculationService for parents

- **Member types:** In buildMemberList or in the request, support dependents with relationship PARENT / PARENT_IN_LAW; pass memberType into MemberInfo (e.g. "parent", "parent_in_law") so rate lookup can use member_type = 'parent' for age-banded rates (Scenario 2). Rate table must have rows with member_type = parent (or equivalent) for the parent policy.
- **Age validation:** Before calculating parent premium, load CompanyEnrollmentConfig for the company. If parent_age_limit is set, for each parent/in-law member compute age from DOB and reject with clear error if age > parent_age_limit.
- **Scenario 1 (ESCP):** Parents in base GMC floater — no separate policy; they are just additional members in the same GMC calculation. No new mapping.
- **Scenario 2:** Parent coverage via separate policy — use a distinct plan_type or product (e.g. PARENT_GMC) and a separate policy_id. When parent coverage is opted, create employee_policy_map for each parent individual → parent policy with is_voluntary = true. Premium: age-banded using member_type = 'parent' / 'parent_in_law' and parent policy rates.

### 2.4 Cost-sharing for parents

- [CoverageCategory](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/enums/CoverageCategory.java) already has PARENT and PARENT_IN_LAW. In [PremiumCalculationServiceImpl](Vima-backend/src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl/PremiumCalculationServiceImpl.java), when building coverage category per plan, if the plan or member list includes parents, use CoverageCategory.PARENT or PARENT_IN_LAW for that segment and call applyCostSharing(companyId, planType, coverageCategory, totalPremium) so parent cost-sharing rules apply.

### 2.5 Max parents / in-laws enforcement

- When processing dependents for enrollment or premium, count dependents with relationship PARENT and PARENT_IN_LAW; compare to config max_parents and max_in_laws. Reject with clear error if exceeded.

### 2.6 Duplicate coverage declaration

- "Duplicate coverage declaration" can be stored in submission JSONB (e.g. a new field in personal_details or a dedicated JSONB column if added later). For BE-04, document that the question response is stored in submission (e.g. enrollment_submissions.personal_details or a dedicated column); implementation can be a simple key in existing JSONB.

---

## 3. Enrollment Plan Selections entity (optional for BE-04)

- **Entity:** EnrollmentPlanSelection mapping to `cpc.enrollment_plan_selections`: id, enrollment_submission_id, plan_type, opted, coverage_amount, premium, **sum_insured**, **deductible_amount**, **topup_plan_option_id** (UUID FK to topup_plan_options), **is_voluntary**, created_at, updated_at.
- Use when persisting plan choices (e.g. on submit or save draft) so that voluntary flag, sum insured, deductible, and top-up option are stored. If current flow only uses enrollment_submissions.plan_selections JSONB, this entity can be used in parallel or when migrating to normalized selections.

---

## 4. Employee policy mapping for top-up and parent (Scenario 2)

- **Top-up opt-in:** When employee selects a top-up (and optionally dependents), create one employee_policy_map per covered person: individualId, policyId = topup_plan_option.policy_id, organizationId, sumInsured = chosen SI, relationship, isVoluntary = true, effectiveFrom, source = ENROLLMENT (and enrollment_submission_id or enrollment_window_id if available). Reuse createMapping in loop or add createBulkMappings / createMappingsForTopupOptIn(companyId, employeeId, topupPlanOptionId, chosenSumInsured, dependentIndividualIds, enrollmentSubmissionId).
- **Parent coverage (Scenario 2):** When parent coverage is opted with a separate parent policy, create employee_policy_map for each parent dependent: individualId = parent customer id, primaryEmployeeId = employee, relationship = PARENT/PARENT_IN_LAW, policyId = parent policy id, organizationId, isVoluntary = true, sumInsured/coverage from config or selection, effectiveFrom, source = ENROLLMENT.

---

## 5. Audit

- **@AuditedOperation** on top-up CRUD: schemaName = "cpc", tableName = "topup_plan_options", entityType = "TOPUP_PLAN_OPTION", action = CREATE | UPDATE | DELETE.
- **@AuditedOperation** on company enrollment config update: schemaName = "cpc", tableName = "company_enrollment_config", entityType = "COMPANY_ENROLLMENT_CONFIG", action = CREATE | UPDATE.

---

## 6. File list (new/updated)

| Item       | File                                                                                                                                                                                      |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Entity     | TopupPlanOption.java, CompanyEnrollmentConfig.java; optional EnrollmentPlanSelection.java                                                                                                 |
| Repository | ITopupPlanOptionRepository.java, ICompanyEnrollmentConfigRepository.java                                                                                                                  |
| DTOs       | TopupPlanOptionRequestDto, TopupPlanOptionResponseDto, TopupOptionsResponseDto (employee); CompanyEnrollmentConfigRequestDto, CompanyEnrollmentConfigResponseDto                          |
| Mapper     | TopupPlanOptionMapper.java; optional CompanyEnrollmentConfigMapper                                                                                                                        |
| Service    | ITopupPlanOptionService, TopupPlanOptionServiceImpl; ICompanyEnrollmentConfigService, CompanyEnrollmentConfigServiceImpl                                                                  |
| Controller | TopupPlanOptionController (admin + employee GET by token); CompanyEnrollmentConfig endpoints (e.g. under admin/companies/{id}/enrollment-config)                                          |
| Premium    | PremiumCalculationServiceImpl: parent member types, load config and validate parent age limit and max parents/in-laws; use PARENT/PARENT_IN_LAW in cost-sharing category where applicable |
| EPM        | Use existing createMapping/createBulkMappings or add helper in IEmployeePolicyMapService for top-up and parent opt-in mappings                                                            |

---

## 7. Acceptance criteria mapping

| Criterion                                                       | Implementation                                                                                                                  |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| Admin CRUD for top-up options                                   | TopupPlanOptionController + service CRUD                                                                                        |
| Employee API returns top-up options with premium preview per SI | GET enrollment/{token}/topup-options, TopupPlanOptionService.getActiveOptionsWithPremiumPreview using PremiumCalculationService |
| Base GMC required                                               | In getActiveOptionsWithPremiumPreview or in submit flow: check employee has GMC mapping before allowing top-up                  |
| Max 1 top-up + 1 super top-up                                   | Validate in service when saving plan selections or on submit                                                                    |
| Parent premium age-banded for Scenario 2                        | Rate table with member_type parent; calculatePlanPremium with MemberInfo(memberType=parent, age)                                |
| Age limit validation                                            | Load config, reject if any parent age > parent_age_limit with clear message                                                     |
| Scenario 1 (ESCP): parents in base floater                      | No separate premium/policy; parents as dependents in existing GMC                                                               |
| Max 2 parents + 2 in-laws                                       | Config max_parents, max_in_laws; validate dependent list before premium/submit                                                  |
| Plan selections store voluntary, sum insured, deductible        | EnrollmentPlanSelection entity and save from submission flow; or extend JSONB structure                                         |
| Top-up opt-in creates employee_policy_map is_voluntary=true       | createMapping(s) in TopupPlanOptionService or enrollment submit flow                                                            |
| Parent Scenario 2 creates separate policy mappings              | createMapping for each parent → parent policy_id, is_voluntary=true                                                             |

Implement in order: CompanyEnrollmentConfig (entity, repo, service, config API) so parent limits and age limit are available; then TopupPlanOption (entity, repo, DTOs, mapper, service, admin CRUD, employee API with premium preview); then premium/cost-sharing extensions for parents and validation; then employee_policy_map creation for top-up and parent opt-in; then EnrollmentPlanSelection entity if persisting normalized selections; finally audit on new CRUD endpoints.
