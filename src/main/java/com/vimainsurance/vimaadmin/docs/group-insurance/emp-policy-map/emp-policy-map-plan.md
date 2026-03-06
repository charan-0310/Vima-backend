# Employee–Policy Mapping — Foundation Plan

> **Goal:** Introduce a proper many-to-many relationship between individuals (employees + dependents) and policies, replacing the current loose org-level linkage. This is the **prerequisite** for all Phase 2 self-enrollment features.

---

## 1. Problem Statement

### Current State

Today, policies are linked to **organizations**, not to individual employees:

```
organizations ──1:N──> policies    (organization_id on policy)
organizations ──1:N──> customers   (organization_id on customer)
```

The `policies` table has:
- `primary_individual_id` — intended for retail/individual policies, not group
- `covered_individuals` — UUID array column that is **never populated** in the current flow
- `applies_to_employees` — boolean flag, no granular mapping

There is **no junction table** between employees and policies. The system implicitly assumes "all employees in an org are covered by all org policies where `applies_to_employees = true`."

### Why This Breaks

This model cannot answer:
- "Which specific policy is employee X covered under?" (needed for premium calculation)
- "Is this dependent covered under the base GMC or the separate parent policy?" (needed for parent coverage — Scenario 2)
- "Did this employee opt into the top-up policy?" (needed for voluntary top-ups)
- "What is the sum insured for this employee under this policy?" (needed when SI varies by grade)
- "When did this employee's coverage start/end under this policy?" (needed for proration, claims validation)

### What Phase 2 Needs

Every Phase 2 feature depends on knowing the employee→policy relationship:

| Feature | Why It Needs Employee–Policy Mapping |
|---------|--------------------------------------|
| **Premium Engine (F1)** | Must look up rates for the specific policy the employee is on |
| **Cost-Sharing (F2)** | Rules are per plan_type — need to know which policy maps to which plan_type for this employee |
| **Top-Up Plans (F5)** | Top-ups are separate policies; employee opts in voluntarily — need to track this |
| **Parent Coverage (F4)** | Scenario 2: parents are on a **different policy** than the employee's base GMC |
| **Payroll Reports (F6)** | Report needs per-employee per-policy premium breakdown |
| **Life Events (F7)** | Adding/removing dependents changes who is covered under which policy |
| **Claims** | Claim validation needs to confirm the claimant is actually covered under the claimed policy |

---

## 2. Design

### 2.1 New Table: `cpc.employee_policy_map`

A many-to-many junction table linking individuals to policies, with coverage metadata.

```sql
CREATE TABLE cpc.employee_policy_map (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- WHO is covered
    individual_id UUID NOT NULL,                -- The person (employee or dependent)
    primary_employee_id UUID,                   -- The employee this person belongs to (NULL for SELF)
    relationship VARCHAR(50) NOT NULL,          -- SELF, SPOUSE, CHILD1-4, FATHER, MOTHER, FATHER_IN_LAW, MOTHER_IN_LAW

    -- WHICH policy
    policy_id BIGINT NOT NULL,                  -- FK to cpc.policies(policy_id)
    organization_id UUID NOT NULL,              -- Denormalized for query efficiency

    -- COVERAGE details
    sum_insured DECIMAL(15, 2),                 -- May differ by grade/tier within same policy
    coverage_tier VARCHAR(50),                  -- e.g., Basic, Standard, Enhanced

    -- OPT-IN tracking
    is_voluntary BOOLEAN DEFAULT FALSE,         -- TRUE for top-ups, parent coverage (Scenario 2)

    -- STATUS
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, CANCELLED, PENDING, SUSPENDED
    effective_from DATE NOT NULL,
    effective_to DATE,                          -- NULL = open-ended (until policy end date)
    cancellation_reason VARCHAR(255),
    cancelled_at TIMESTAMP WITH TIME ZONE,

    -- SOURCE (how this mapping was created)
    source VARCHAR(30) NOT NULL,                -- BULK_UPLOAD, ENROLLMENT, ENDORSEMENT, MANUAL, RENEWAL, BACKFILL
    enrollment_window_id UUID,
    endorsement_id UUID,
    enrollment_submission_id UUID,

    -- TIMESTAMPS
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- FOREIGN KEYS
    CONSTRAINT fk_epm_individual FOREIGN KEY (individual_id) REFERENCES cpc.customers(individual_id),
    CONSTRAINT fk_epm_primary_employee FOREIGN KEY (primary_employee_id) REFERENCES cpc.customers(individual_id),
    CONSTRAINT fk_epm_policy FOREIGN KEY (policy_id) REFERENCES cpc.policies(policy_id),
    CONSTRAINT fk_epm_organization FOREIGN KEY (organization_id) REFERENCES cpc.organizations(organization_id),
    CONSTRAINT fk_epm_enrollment_window FOREIGN KEY (enrollment_window_id) REFERENCES cpc.enrollment_windows(id),
    CONSTRAINT fk_epm_endorsement FOREIGN KEY (endorsement_id) REFERENCES cpc.endorsements(endorsement_id),
    CONSTRAINT fk_epm_submission FOREIGN KEY (enrollment_submission_id) REFERENCES cpc.enrollment_submissions(id)
);

-- Only one active mapping per individual per policy
CREATE UNIQUE INDEX idx_epm_unique_active
    ON cpc.employee_policy_map(individual_id, policy_id)
    WHERE status = 'ACTIVE';

-- Query patterns
CREATE INDEX idx_epm_individual ON cpc.employee_policy_map(individual_id);
CREATE INDEX idx_epm_policy ON cpc.employee_policy_map(policy_id);
CREATE INDEX idx_epm_organization ON cpc.employee_policy_map(organization_id);
CREATE INDEX idx_epm_primary_employee ON cpc.employee_policy_map(primary_employee_id) WHERE primary_employee_id IS NOT NULL;
CREATE INDEX idx_epm_status ON cpc.employee_policy_map(status);
CREATE INDEX idx_epm_enrollment_window ON cpc.employee_policy_map(enrollment_window_id) WHERE enrollment_window_id IS NOT NULL;
CREATE INDEX idx_epm_endorsement ON cpc.employee_policy_map(endorsement_id) WHERE endorsement_id IS NOT NULL;
```

### 2.2 Column Rationale

| Column | Why |
|--------|-----|
| `individual_id` | The covered person — can be employee (SELF) or any dependent |
| `primary_employee_id` | Links dependents back to their employee. NULL when relationship=SELF. Enables "get all mappings for an employee's family" |
| `relationship` | Matches the existing `cpc.customers.relationship` values. Denormalized here for query convenience (avoid joining back to customers just to filter by relationship) |
| `policy_id` | The specific policy this person is covered under. BIGINT to match the actual entity PK type |
| `organization_id` | Denormalized from policy. Avoids a join when querying "all mappings for org X" |
| `sum_insured` | Because SI can vary by grade/tier within the same policy. Employee L1 might have 2L, L4 might have 5L — same policy |
| `coverage_tier` | Grade-based tier label (Basic, Standard, Enhanced) for display and rate lookup |
| `is_voluntary` | Distinguishes automatic base coverage from employee-opted coverage (top-ups, parent coverage Scenario 2) |
| `status` | Lifecycle tracking. ACTIVE = currently covered. CANCELLED = removed via endorsement/exit. PENDING = awaiting endorsement approval |
| `effective_from/to` | Policy-period dates. Needed for proration, mid-year joins, claims date validation |
| `source` | Audit trail for how the mapping was created |
| `enrollment_window_id / endorsement_id / enrollment_submission_id` | Link back to the specific event that created this mapping |

### 2.3 Relationship Diagram

```
organizations ──1:N──> policies
     │                     │
     │                     │  (employee_policy_map)
     │                     │
     └──1:N──> customers <─┘
                (individuals)
```

After this change:

```
organizations ──1:N──> policies ──1:N──┐
     │                                  │
     │                          employee_policy_map
     │                                  │
     └──1:N──> customers ──────1:N──────┘
```

Each `employee_policy_map` row = "individual X is covered under policy Y".

---

## 3. Real-World Scenarios

### Scenario 1: Company with ESCP base GMC (parents included in base)

Company has 1 GMC policy (#101) covering Employee + Spouse + Children + Parents.

| individual_id | relationship | policy_id | is_voluntary | sum_insured |
|---------------|-------------|-----------|-------------|-------------|
| emp-001 (Ravi) | SELF | 101 | false | 500000 |
| dep-001 (Priya, spouse) | SPOUSE | 101 | false | 500000 |
| dep-002 (Arjun, child) | CHILD1 | 101 | false | 500000 |
| dep-003 (Ravi's father) | FATHER | 101 | false | 500000 |
| dep-004 (Ravi's mother) | MOTHER | 101 | false | 500000 |

All 5 people → same policy #101. Family floater SI = 5L for the family.

### Scenario 2: Company with ESC base + separate parent policy

Company has 2 GMC policies:
- Policy #201: Base GMC (Employee + Spouse + Children)
- Policy #202: Parent GMC (separate insurer policy for parents)

| individual_id | relationship | policy_id | is_voluntary | sum_insured |
|---------------|-------------|-----------|-------------|-------------|
| emp-001 (Ravi) | SELF | 201 | false | 500000 |
| dep-001 (Priya) | SPOUSE | 201 | false | 500000 |
| dep-002 (Arjun) | CHILD1 | 201 | false | 500000 |
| dep-003 (Ravi's father) | FATHER | **202** | **true** | 300000 |
| dep-004 (Ravi's mother) | MOTHER | **202** | **true** | 300000 |

Parents on a **different policy** (#202). Marked voluntary because employee opted in.

### Scenario 3: Employee opts for top-up

Company has:
- Policy #301: Base GMC (5L)
- Policy #302: ICICI Top-Up (10L deductible = base SI)

| individual_id | relationship | policy_id | is_voluntary | sum_insured |
|---------------|-------------|-----------|-------------|-------------|
| emp-001 | SELF | 301 | false | 500000 |
| dep-001 (spouse) | SPOUSE | 301 | false | 500000 |
| emp-001 | SELF | **302** | **true** | 1000000 |
| dep-001 (spouse) | SPOUSE | **302** | **true** | 1000000 |

Employee and spouse mapped to BOTH base GMC and top-up policy.

### Scenario 4: Company with 3 medical policies, employee on 2 of 3

Company has:
- Policy #401: ICICI GMC Standard (3L)
- Policy #402: ICICI GMC Enhanced (5L)
- Policy #403: ICICI Super Top-Up (20L)

Employee (L4 grade) gets Enhanced + Super Top-Up but not Standard:

| individual_id | relationship | policy_id | is_voluntary | sum_insured | coverage_tier |
|---------------|-------------|-----------|-------------|-------------|---------------|
| emp-001 | SELF | 402 | false | 500000 | Enhanced |
| emp-001 | SELF | 403 | true | 2000000 | — |

Only 2 out of 3 policies. Standard policy (#401) is for L1-L2 grades, not mapped to this employee.

### Scenario 5: Employee exits mid-year

When employee leaves, all their active mappings get cancelled:

| individual_id | policy_id | status | effective_from | effective_to | cancellation_reason |
|---------------|-----------|--------|---------------|-------------|---------------------|
| emp-001 | 201 | **CANCELLED** | 2026-04-01 | **2026-09-15** | EMPLOYEE_EXIT |
| dep-001 | 201 | **CANCELLED** | 2026-04-01 | **2026-09-15** | EMPLOYEE_EXIT |

Premium proration uses `effective_from` to `effective_to` (5.5 months of the year).

---

## 4. How Mappings Get Created

### 4.1 Bulk Upload (existing flow)

**When:** Admin uploads employee CSV via portal.
**What happens:** After employees/dependents are saved to `cpc.customers`, create `employee_policy_map` entries for each person → each applicable policy.

Logic:
1. Get all active policies for the organization where `applies_to_employees = true`
2. For each employee (SELF):
   - Determine their grade/tier (from `designation` or a future `grade` field)
   - Find matching policies for that tier
   - Create mapping: `source = 'BULK_UPLOAD'`, `effective_from = policy.start_date` (or date of joining)
3. For each dependent:
   - If relationship is FATHER/MOTHER/FATHER_IN_LAW/MOTHER_IN_LAW:
     - Check if there's a separate parent policy → map to that
     - If no separate parent policy (ESCP), map to the base GMC
   - Otherwise (SPOUSE, CHILD): map to same policies as the employee

### 4.2 Self-Enrollment (Phase 2 flow)

**When:** Employee submits enrollment and it gets approved.
**What happens:** Create mappings based on plan selections stored in the submission.

Logic:
1. Base plans (GMC, GPA, GTL): create mapping for employee + all dependents
2. Top-up plans (if selected): create mapping with `is_voluntary = true`
3. Parent coverage (Scenario 2, if opted): create mapping for parents → parent policy with `is_voluntary = true`
4. Source = `'ENROLLMENT'`, link to `enrollment_window_id` and `enrollment_submission_id`

### 4.3 Endorsement (addition)

**When:** Endorsement approved to add employees/dependents.
**What happens:** Create new mappings for the added people.

Logic:
1. For each person in the endorsement, create mappings to the appropriate policies
2. Source = `'ENDORSEMENT'`, link to `endorsement_id`
3. `effective_from` = endorsement effective date (may be mid-year → proration applies)

### 4.4 Endorsement (deletion)

**When:** Endorsement approved to remove employees/dependents.
**What happens:** Cancel active mappings.

Logic:
1. For each person being removed, set `status = 'CANCELLED'`, `effective_to = cancellation date`, `cancellation_reason`
2. If base GMC cancelled → auto-cancel linked top-up/parent mappings

### 4.5 Manual Admin Action

**When:** Vima Admin manually adjusts mappings (reassign employee to different policy, fix errors).
**What happens:** Source = `'MANUAL'`.

### 4.6 Policy Renewal

**When:** Annual policy renewal — new policy record created.
**What happens:** Bulk-create mappings for all active employees under the new policy, carry forward from previous year.
Source = `'RENEWAL'`.

---

## 5. Backend Implementation

### 5.1 Flyway Migration

File: `V{next}__create_employee_policy_map.sql`

This should be the **first migration** in the Phase 2 batch — all other Phase 2 tables/changes come after.

Content: The full CREATE TABLE + indexes from Section 2.1 above.

### 5.2 Entity

```java
@Entity
@Table(name = "employee_policy_map", schema = "cpc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeePolicyMap {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "individual_id", nullable = false)
    private UUID individualId;

    @Column(name = "primary_employee_id")
    private UUID primaryEmployeeId;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "sum_insured")
    private BigDecimal sumInsured;

    @Column(name = "coverage_tier", length = 50)
    private String coverageTier;

    @Column(name = "is_voluntary", nullable = false)
    private Boolean isVoluntary = false;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "enrollment_window_id")
    private UUID enrollmentWindowId;

    @Column(name = "endorsement_id")
    private UUID endorsementId;

    @Column(name = "enrollment_submission_id")
    private UUID enrollmentSubmissionId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

### 5.3 Repository

```java
@Repository
public interface IEmployeePolicyMapRepository
    extends JpaRepository<EmployeePolicyMap, UUID>, JpaSpecificationExecutor<EmployeePolicyMap> {

    // All active policies for a person
    List<EmployeePolicyMap> findByIndividualIdAndStatus(UUID individualId, String status);

    // All people covered by a policy
    List<EmployeePolicyMap> findByPolicyIdAndStatus(Long policyId, String status);

    // All mappings for an employee's family (employee + their dependents)
    List<EmployeePolicyMap> findByPrimaryEmployeeIdAndStatus(UUID primaryEmployeeId, String status);

    // All mappings for an employee (SELF entries only)
    List<EmployeePolicyMap> findByIndividualIdAndRelationshipAndStatus(
        UUID individualId, String relationship, String status);

    // All active mappings for an org
    Page<EmployeePolicyMap> findByOrganizationIdAndStatus(
        UUID organizationId, String status, Pageable pageable);

    // Check if person is actively covered under a policy
    boolean existsByIndividualIdAndPolicyIdAndStatus(UUID individualId, Long policyId, String status);

    // All active mappings from a specific enrollment window
    List<EmployeePolicyMap> findByEnrollmentWindowIdAndStatus(UUID windowId, String status);

    // All active mappings from a specific endorsement
    List<EmployeePolicyMap> findByEndorsementIdAndStatus(UUID endorsementId, String status);

    // Bulk cancel: used for employee exit
    @Modifying
    @Query("""
        UPDATE EmployeePolicyMap m
        SET m.status = 'CANCELLED', m.effectiveTo = :effectiveDate,
            m.cancellationReason = :reason, m.cancelledAt = CURRENT_TIMESTAMP,
            m.updatedAt = CURRENT_TIMESTAMP
        WHERE (m.individualId = :employeeId OR m.primaryEmployeeId = :employeeId)
        AND m.status = 'ACTIVE'
    """)
    int cancelAllForEmployee(
        @Param("employeeId") UUID employeeId,
        @Param("effectiveDate") LocalDate effectiveDate,
        @Param("reason") String reason);
}
```

### 5.4 DTOs

**Request:**
```java
@Data
public class EmployeePolicyMapRequestDto {
    @NotNull private UUID individualId;
    private UUID primaryEmployeeId;
    @NotBlank private String relationship;
    @NotNull private Long policyId;
    @NotNull private UUID organizationId;
    private BigDecimal sumInsured;
    private String coverageTier;
    private Boolean isVoluntary;
    @NotNull private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String source;
}
```

**Response:**
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeePolicyMapResponseDto {
    private UUID id;
    private UUID individualId;
    private String individualName;         // Joined from customers
    private UUID primaryEmployeeId;
    private String primaryEmployeeName;    // Joined from customers
    private String relationship;
    private Long policyId;
    private String policyNumber;           // Joined from policies
    private String productType;            // Joined from policies
    private String insurerName;            // Joined from policies → insurance_providers
    private UUID organizationId;
    private BigDecimal sumInsured;
    private String coverageTier;
    private Boolean isVoluntary;
    private String status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String source;
    private LocalDateTime createdAt;
}
```

**Bulk Request (for backfill / bulk upload):**
```java
@Data
public class BulkEmployeePolicyMapRequestDto {
    @NotNull private UUID organizationId;
    @NotNull private Long policyId;
    @NotNull private List<IndividualMappingDto> individuals;

    @Data
    public static class IndividualMappingDto {
        @NotNull private UUID individualId;
        private UUID primaryEmployeeId;
        @NotBlank private String relationship;
        private BigDecimal sumInsured;
        private String coverageTier;
    }
}
```

### 5.5 Service Interface

```java
public interface IEmployeePolicyMapService {

    // CRUD
    ResponseEntity<ResponseDto<EmployeePolicyMapResponseDto>> createMapping(EmployeePolicyMapRequestDto dto);
    ResponseEntity<ResponseDto<String>> createBulkMappings(BulkEmployeePolicyMapRequestDto dto, String source);
    ResponseEntity<ResponseDto<String>> cancelMapping(UUID mappingId, String reason);
    ResponseEntity<ResponseDto<String>> cancelAllForEmployee(UUID employeeId, LocalDate effectiveDate, String reason);

    // Queries
    ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForIndividual(UUID individualId);
    ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForPolicy(Long policyId);
    ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForEmployeeFamily(UUID employeeId);
    ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForOrganization(
        UUID organizationId, int page, int size, String status);

    // Validation
    boolean isIndividualCoveredByPolicy(UUID individualId, Long policyId);

    // Lifecycle (called by other services)
    void createMappingsFromBulkUpload(UUID organizationId, List<UUID> employeeIds, String source);
    void createMappingsFromEnrollmentSubmission(UUID submissionId);
    void createMappingsFromEndorsement(UUID endorsementId, String endorsementType);
    void cancelMappingsFromEndorsement(UUID endorsementId);
}
```

### 5.6 Controller

```java
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/employee-policy-map")
public class EmployeePolicyMapController {

    @GetMapping("/individual/{individualId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>>
        getMappingsForIndividual(@PathVariable UUID individualId) { ... }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>>
        getMappingsForPolicy(@PathVariable Long policyId) { ... }

    @GetMapping("/employee/{employeeId}/family")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>>
        getMappingsForFamily(@PathVariable UUID employeeId) { ... }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>>
        getMappingsForOrganization(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "ACTIVE") String status) { ... }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<EmployeePolicyMapResponseDto>>
        createMapping(@RequestBody @Valid EmployeePolicyMapRequestDto dto) { ... }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>>
        createBulkMappings(@RequestBody @Valid BulkEmployeePolicyMapRequestDto dto) { ... }

    @DeleteMapping("/{mappingId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN')")
    public ResponseEntity<ResponseDto<String>>
        cancelMapping(@PathVariable UUID mappingId, @RequestParam String reason) { ... }
}
```

---

## 6. Integration with Existing Flows

### 6.1 Bulk Upload (`EmployeeService.uploadEmployees`)

After employees + dependents are saved:

```
uploadEmployees()
  └── save employees & dependents to cpc.customers
  └── NEW: call employeePolicyMapService.createMappingsFromBulkUpload(orgId, employeeIds, "BULK_UPLOAD")
        └── get all active policies for org
        └── for each employee + their dependents → create mappings to applicable policies
```

### 6.2 Enrollment Submission Approval (`HRApprovalServiceImpl`)

After submission is approved:

```
approveSubmission()
  └── update submission status
  └── NEW: call employeePolicyMapService.createMappingsFromEnrollmentSubmission(submissionId)
        └── read plan_selections from submission
        └── create mappings for base plans (employee + dependents)
        └── create mappings for top-ups if selected (is_voluntary = true)
        └── create mappings for parent coverage if opted (is_voluntary = true)
```

### 6.3 Endorsement Approval (`EndorsementServiceImpl`)

For addition endorsements:

```
approveEndorsement() [type = ADDITION]
  └── NEW: call employeePolicyMapService.createMappingsFromEndorsement(endorsementId, "ADDITION")
        └── for each person in deal_endorsements → create mappings
```

For deletion endorsements:

```
approveEndorsement() [type = DELETION]
  └── NEW: call employeePolicyMapService.cancelMappingsFromEndorsement(endorsementId)
        └── for each person in deal_endorsements → cancel active mappings
        └── if base GMC cancelled → auto-cancel top-up/parent mappings
```

### 6.4 Claims Validation

```
createClaim() / submitClaim()
  └── NEW: validate employeePolicyMapService.isIndividualCoveredByPolicy(employeeId, policyId)
        └── reject claim if no active mapping exists
```

### 6.5 Premium Calculation (Phase 2)

```
calculateEnrollmentPremium()
  └── for each plan selection → look up employee_policy_map to find the exact policy
  └── use policy details (insurer, rates) for premium calculation
```

---

## 7. Data Backfill

For existing data (companies already with employees and policies), run a one-time backfill.

### Migration Script: `V{next}__backfill_employee_policy_map.sql`

```sql
-- Backfill: Map all active employees (SELF) to all active org policies (applies_to_employees = true)
INSERT INTO cpc.employee_policy_map (
    individual_id, primary_employee_id, relationship, policy_id,
    organization_id, sum_insured, is_voluntary, status,
    effective_from, source, created_at, updated_at
)
SELECT
    c.individual_id,
    NULL,                                           -- SELF: no primary_employee_id
    'SELF',
    p.policy_id,
    c.organization_id,
    p.sum_insured,                                  -- Default to policy-level SI
    FALSE,
    'ACTIVE',
    GREATEST(p.start_date, c.date_of_joining, c.created_at::date),
    'BACKFILL',
    NOW(),
    NOW()
FROM cpc.customers c
JOIN cpc.policies p ON p.organization_id = c.organization_id
WHERE c.relationship = 'SELF'
  AND c.status != 'INACTIVE'
  AND p.applies_to_employees = TRUE
  AND p.status != 'INACTIVE'
ON CONFLICT DO NOTHING;

-- Backfill: Map all active dependents to the same policies as their primary employee
INSERT INTO cpc.employee_policy_map (
    individual_id, primary_employee_id, relationship, policy_id,
    organization_id, sum_insured, is_voluntary, status,
    effective_from, source, created_at, updated_at
)
SELECT
    d.individual_id,
    d.primary_individual_id,                        -- The employee they belong to
    d.relationship,
    epm.policy_id,                                  -- Same policy as their employee
    d.organization_id,
    epm.sum_insured,
    FALSE,
    'ACTIVE',
    epm.effective_from,
    'BACKFILL',
    NOW(),
    NOW()
FROM cpc.customers d
JOIN cpc.employee_policy_map epm ON epm.individual_id = d.primary_individual_id AND epm.status = 'ACTIVE'
WHERE d.primary_individual_id IS NOT NULL
  AND d.status != 'INACTIVE'
ON CONFLICT DO NOTHING;
```

> **Note:** This backfill assumes all employees in an org are on all org policies (the current implicit behavior). After backfill, admins can adjust individual mappings via the admin API.

---

## 8. What Changes on the `policies` Table

### Fields to Deprecate (not remove yet)

| Field | Current Use | After This Change |
|-------|------------|-------------------|
| `primary_individual_id` | For retail policies (individual → policy) | Keep for retail; for group policies, use `employee_policy_map` instead |
| `covered_individuals` | UUID array, never populated | Deprecated — `employee_policy_map` replaces this |
| `applies_to_employees` | Boolean flag for group policies | Still useful as a quick check; `employee_policy_map` is the source of truth |

No columns are removed. We just stop relying on `covered_individuals` and use the junction table for all group insurance queries.

### Possible Future Additions to `policies`

| Field | Purpose |
|-------|---------|
| `policy_sub_type` | Distinguish base GMC vs parent GMC vs top-up (complements `product_type`) |
| `parent_policy_id` | For top-ups: reference to the base policy they extend |

These are optional enhancements, not required for the initial mapping.

---

## 9. Frontend Impact

### Admin Portal

1. **Employee Detail Page** — New "Policy Coverage" section showing all active policies for the employee
2. **Policy Detail Page** — New "Covered Members" tab showing all individuals mapped to the policy
3. **Enrollment Flow** — Plan selections during enrollment create mappings on approval
4. **Endorsement Detail** — Show which policy mappings were created/cancelled

### Employee Portal (Self-Enrollment)

No direct impact — employee doesn't see the mapping table. But the enrollment flow **creates** mappings when the submission is approved.

---

## 10. API Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/employee-policy-map/individual/{id}` | All policies for a person |
| GET | `/api/v1/employee-policy-map/policy/{id}` | All people on a policy |
| GET | `/api/v1/employee-policy-map/employee/{id}/family` | Employee + dependents' policies |
| GET | `/api/v1/employee-policy-map/organization/{id}` | All mappings for an org (paginated) |
| POST | `/api/v1/employee-policy-map` | Create single mapping |
| POST | `/api/v1/employee-policy-map/bulk` | Create bulk mappings |
| DELETE | `/api/v1/employee-policy-map/{id}?reason=X` | Cancel a mapping |

---

## 11. Implementation Order

| Step | What | Effort |
|------|------|--------|
| 1 | Flyway migration: create `employee_policy_map` table + indexes | 0.25d |
| 2 | Entity + Repository | 0.25d |
| 3 | DTOs + Mapper | 0.25d |
| 4 | Service (CRUD + bulk + cancellation logic) | 0.5d |
| 5 | Controller + authorization | 0.25d |
| 6 | Wire into bulk upload flow (`EmployeeService`) | 0.5d |
| 7 | Wire into enrollment approval flow (`HRApprovalServiceImpl`) | 0.5d |
| 8 | Wire into endorsement flows (`EndorsementServiceImpl`) | 0.5d |
| 9 | Data backfill migration for existing data | 0.25d |
| 10 | Audit integration (`@AuditedOperation`) | 0.25d |
| **Total** | | **~3.5 days** |

---

## 12. Open Questions

| # | Question | Suggested Default |
|---|----------|-------------------|
| 1 | Should `sum_insured` on the mapping override the policy-level SI? | Yes — allows grade-based SI within same policy |
| 2 | Should we track premium amount per mapping? | No for now — premium is calculated dynamically. Snapshot in `enrollment_submissions` |
| 3 | How to handle policy renewal? Bulk-create new mappings or carry forward? | Bulk-create with `source = 'RENEWAL'`, copy from previous year's active mappings |
| 4 | Should the backfill run as a Flyway migration or a manual admin job? | Flyway migration (V{n+1}) for consistency, with ON CONFLICT DO NOTHING for safety |
| 5 | Do we need a `coverage_type` column (BASE, TOP_UP, PARENT_COVERAGE) on the mapping? | No — derive from the policy's `product_type` / `policy_category`. Keep the mapping lean |

---

*Document Version: 1.0 | Created: March 5, 2026*
*Location: `docs/group-insurance/emp-policy-map/`*
