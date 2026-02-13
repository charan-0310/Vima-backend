# Vima Audit Trail — Implementation Plan

> **Goal:** Track WHO did WHAT, WHEN, WHERE, and capture before/after state for all critical data changes.
> **Approach:** Hibernate Envers (replaces existing Javers usage)
> **Phase:** 1 — Core Implementation

---

## Current State

| Item | Status |
|------|--------|
| Spring Boot | 3.4.6, Java 21 |
| Hibernate Envers | **Not present** — needs to be added |
| Javers 7.9.0 | Present — `@DiffIgnore` on 3 entities (Deals, DealEndorsement, Endorsement). **Will be replaced by Envers.** |
| User context | `JwtUserExtractor` — provides userId, email, role |
| Request tracing | `CorrelationIdFilter` — sets `correlationId` in MDC |
| Next Flyway migration | V16 |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   HTTP Request                       │
│  CorrelationIdFilter → sets correlationId in MDC     │
│  SecurityFilter → sets JWT in SecurityContext         │
└──────────────────────┬──────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────┐
│              Service Layer (existing)                  │
│  repository.save(entity)                              │
└──────────────────────┬───────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────┐
│           Hibernate Envers (automatic)                │
│                                                       │
│  1. Intercepts INSERT/UPDATE/DELETE                    │
│  2. CustomRevisionListener populates revision with:   │
│     - userId, userEmail, userRole                     │
│     - ipAddress, correlationId                        │
│     - actionSource (WEB / BULK / SYSTEM / ENROLLMENT) │
│  3. Writes snapshot to *_AUD table + REVINFO          │
└──────────────────────────────────────────────────────┘
```

**Key point:** No changes to existing service/controller code. Envers hooks into Hibernate automatically. We only annotate entities.

---

## Ticket 1 — Setup Hibernate Envers & Global Revision Listener

### 1.1 Add Dependency

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-envers</artifactId>
</dependency>
```

> `spring-data-envers` pulls in `hibernate-envers` transitively. Version managed by Spring Boot BOM.

### 1.2 Enable Envers Repositories

```java
// On the main application class or a @Configuration class
@EnableJpaRepositories(repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
```

### 1.3 Custom Revision Entity

Location: `entity/audit/AuditRevisionEntity.java`

```java
@Entity
@Table(name = "audit_revisions", schema = "audit")
@RevisionEntity(AuditRevisionListener.class)
@Getter @Setter
public class AuditRevisionEntity extends DefaultRevisionEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_role")
    private String userRole;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "action_source")
    @Enumerated(EnumType.STRING)
    private ActionSource actionSource;  // WEB, BULK, SYSTEM, ENROLLMENT
}
```

### 1.4 Revision Listener

Location: `entity/audit/AuditRevisionListener.java`

```java
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        AuditRevisionEntity rev = (AuditRevisionEntity) revisionEntity;

        // User context from SecurityContextHolder
        // (JwtUserExtractor cannot be @Autowired here — Envers
        //  instantiates this class directly. Extract from JWT manually
        //  or use a static ThreadLocal helper.)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            rev.setUserEmail(jwt.getClaimAsString("email"));
            rev.setUserRole(/* extract from DB or token */);
            rev.setUserId(/* lookup or token claim */);
        }

        // Request context
        ServletRequestAttributes attrs = (ServletRequestAttributes)
            RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            rev.setIpAddress(request.getRemoteAddr());
        }

        // MDC
        rev.setCorrelationId(MDC.get("correlationId"));

        // Default action source
        rev.setActionSource(ActionSourceHolder.get());  // ThreadLocal, default WEB
    }
}
```

### 1.5 ActionSource Enum + ThreadLocal Holder

```java
public enum ActionSource { WEB, BULK, SYSTEM, ENROLLMENT }
```

```java
// Set by service methods before bulk/system operations
public class ActionSourceHolder {
    private static final ThreadLocal<ActionSource> HOLDER =
        ThreadLocal.withInitial(() -> ActionSource.WEB);

    public static void set(ActionSource source) { HOLDER.set(source); }
    public static ActionSource get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}
```

### 1.6 Envers Configuration

```properties
# application.properties
spring.jpa.properties.org.hibernate.envers.audit_table_suffix=_aud
spring.jpa.properties.org.hibernate.envers.default_schema=audit
spring.jpa.properties.org.hibernate.envers.store_data_at_delete=true
spring.jpa.properties.org.hibernate.envers.global_with_modified_flag=true
```

| Property | Purpose |
|----------|---------|
| `audit_table_suffix=_aud` | Audit tables named `customers_aud`, `policies_aud`, etc. |
| `default_schema=audit` | All audit tables live in a separate `audit` schema |
| `store_data_at_delete=true` | Capture full snapshot on DELETE (not just the ID) |
| `global_with_modified_flag=true` | Add `*_MOD` boolean columns to know which fields changed |

### 1.7 Flyway Migration — V16

```sql
-- V16__create_audit_schema_and_revinfo.sql

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_revisions (
    id          SERIAL PRIMARY KEY,
    timestamp   BIGINT NOT NULL,
    user_id     UUID,
    user_email  VARCHAR(255),
    user_role   VARCHAR(50),
    ip_address  VARCHAR(45),
    correlation_id VARCHAR(36),
    action_source VARCHAR(20) DEFAULT 'WEB'
);
```

> Envers will auto-create the `*_aud` tables via Hibernate DDL if `spring.jpa.hibernate.ddl-auto` allows it. For production, we write explicit Flyway migrations per entity (see below).

---

## Ticket 2 — Enable Auditing: `cpc.customers` & `cpc.organizations`

### Entity: Deals.java (maps to `cpc.customers`)

```java
@Audited
@Table(name = "customers", schema = "cpc")
public class Deals {

    @NotAudited   // SECURITY — never store in audit trail
    private String passwordHash;

    @NotAudited   // PII — audit only if compliance requires
    private String aadhaarNumber;

    // panNumber — AUDIT (needed for compliance tracking)
    // All other fields — audited by default
}
```

**Self-referencing FK** (`primaryIndividual`): Envers handles `@ManyToOne` self-references automatically. The `_aud` table will store the `primary_individual_id` FK value at each revision.

### Entity: Organization.java (maps to `cpc.organizations`)

```java
@Audited
@Table(name = "organizations", schema = "cpc")
public class Organization {
    // All fields audited — no sensitive exclusions needed
    // gstin, panNumber are business-critical, keep audited
}
```

### Flyway Migration — V16.1

```sql
-- V16.1__audit_customers_organizations.sql

-- Customers audit table
CREATE TABLE audit.customers_aud (
    individual_id UUID NOT NULL,
    rev           INTEGER NOT NULL REFERENCES audit.audit_revisions(id),
    revtype       SMALLINT,  -- 0=ADD, 1=MOD, 2=DEL

    -- All columns from cpc.customers EXCEPT password_hash, aadhaar_number
    first_name VARCHAR(255),
    first_name_mod BOOLEAN,
    last_name VARCHAR(255),
    last_name_mod BOOLEAN,
    email VARCHAR(255),
    email_mod BOOLEAN,
    status VARCHAR(50),
    status_mod BOOLEAN,
    organization_id UUID,
    organization_id_mod BOOLEAN,
    enrollment_status VARCHAR(50),
    enrollment_status_mod BOOLEAN,
    -- ... (all other audited columns + _MOD flags)

    PRIMARY KEY (individual_id, rev)
);

-- Organizations audit table
CREATE TABLE audit.organizations_aud (
    organization_id UUID NOT NULL,
    rev             INTEGER NOT NULL REFERENCES audit.audit_revisions(id),
    revtype         SMALLINT,

    organization_name VARCHAR(255),
    organization_name_mod BOOLEAN,
    status VARCHAR(50),
    status_mod BOOLEAN,
    -- ... (all columns + _MOD flags)

    PRIMARY KEY (organization_id, rev)
);
```

### Acceptance Criteria

- [ ] Updating a customer's `status` creates a new row in `audit.customers_aud` with `revtype=1`
- [ ] The linked `audit.audit_revisions` row has the correct userId, email, role
- [ ] Dependent ↔ primary member link tracked via `primary_individual_id` in audit table
- [ ] `password_hash` and `aadhaar_number` are NOT present in `customers_aud`

---

## Ticket 3 — Enable Auditing: `cpc.policies` & `cpc.motor_policy_details`

### Entity: Policy.java

```java
@Audited
@Table(name = "policies", schema = "cpc")
public class Policy {

    // covered_individuals is UUID[] — Envers stores array types as-is
    // If issues arise, convert to JSONB or use @NotAudited + manual tracking
    @Column(columnDefinition = "uuid[]")
    private UUID[] coveredIndividuals;

    // sum_insured, total_premium_amount — audited (BigDecimal precision preserved)

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @OneToOne
    private Document document;  // Document entity itself not audited, but FK is tracked
}
```

### Entity: MotorPolicyDetails.java

```java
@Audited
@Table(name = "motor_policy_details", schema = "cpc")
public class MotorPolicyDetails {

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @OneToOne
    private Policy policy;  // FK tracked, policy has its own audit
}
```

### Flyway Migration — V16.2

```sql
-- V16.2__audit_policies.sql
CREATE TABLE audit.policies_aud ( ... );
CREATE TABLE audit.motor_policy_details_aud ( ... );
```

### Acceptance Criteria

- [ ] Changing `sum_insured` or `total_premium_amount` creates a revision with before/after visible
- [ ] Motor policy detail changes linked to same revision as parent policy (when saved in same transaction)
- [ ] `covered_individuals` array changes are captured

---

## Ticket 4 — Enable Auditing: Endorsements

### Entities to Annotate

| Entity | Table | Notes |
|--------|-------|-------|
| `Endorsement` | `cpc.endorsements` | Remove existing Javers `@DiffIgnore`, add `@Audited` |
| `DealEndorsement` | `cpc.deal_endorsements` | Remove Javers `@DiffIgnore`, add `@Audited` |

```java
@Audited
public class Endorsement {
    // Track status changes: PENDING → APPROVED → SUBMITTED → CONFIRMED
    // Track premium_amount, premium_change_type changes
    // source_metadata (JSONB) — audited as JSON string snapshot

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne
    private Document document;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne
    private AdminUser uploadedBy;
}
```

### Acceptance Criteria

- [ ] Endorsement status change from PENDING → APPROVED creates a revision
- [ ] `approved_by`, `approved_at` captured in the same revision
- [ ] DealEndorsement (individual ↔ endorsement link) creation is tracked

---

## Ticket 5 — Enable Auditing: Enrollment Flow

### Entities to Annotate

| Entity | Table | Notes |
|--------|-------|-------|
| `EnrollmentWindows` | `cpc.enrollment_windows` | Status lifecycle, config changes |
| `EnrollmentInvitation` | `cpc.enrollment_invitations` | `@NotAudited` on `tokenHash` |
| `EnrollmentSubmission` | `cpc.enrollment_submissions` | JSONB fields (plan_selections, nominee_data, etc.) |
| `Nominee` | `cpc.nominees` | Beneficiary changes |

```java
@Audited
public class EnrollmentInvitation {
    @NotAudited  // SECURITY — token hash must not appear in audit trail
    private String tokenHash;

    @NotAudited
    private String tokenDeterministic;
}
```

```java
@Audited
public class EnrollmentSubmission {
    // JSONB fields (plan_selections, nominee_data, personal_details, dependents)
    // stored as JSON string snapshots in audit table
    // declarationIpAddress — keep audited (compliance value)

    @NotAudited  // PII — excluded from audit
    private String personalDetails;

    @NotAudited  // PII — excluded from audit
    private String dependents;

    @NotAudited  // PII — excluded from audit
    private String nomineeData;
}
```

### ActionSource for Enrollment

Enrollment submissions come from unauthenticated employee tokens. The `AuditRevisionListener` must handle this:

```java
// In AuditRevisionListener.newRevision():
if (auth == null || auth instanceof AnonymousAuthenticationToken) {
    // Enrollment flow — user context comes from the token/submission
    rev.setActionSource(ActionSource.ENROLLMENT);
    rev.setUserEmail("enrollment-token");
    // employee_id is on the entity being saved, not in security context
}
```

### Acceptance Criteria

- [ ] Enrollment window status changes (SCHEDULED → ACTIVE → CLOSED) are tracked
- [ ] Invitation status changes (SENT → OPENED → COMPLETED) are tracked without exposing token hashes
- [ ] Submission stage/status changes are tracked with full JSONB snapshots
- [ ] Nominee additions/modifications linked to submission revisions

---

## Sensitive Fields Summary

| Entity | Field | Decision | Reason |
|--------|-------|----------|--------|
| Deals (customers) | `passwordHash` | `@NotAudited` | Security |
| Deals (customers) | `aadhaarNumber` | `@NotAudited` | PII |
| EnrollmentInvitation | `tokenHash` | `@NotAudited` | Security |
| EnrollmentInvitation | `tokenDeterministic` | `@NotAudited` | Security |
| VendorToken | `accessToken`, `refreshToken` | Not audited (entity excluded) | Security |
| ZohoToken | `accessToken`, `refreshToken` | Not audited (entity excluded) | Security |

---

## Entities NOT Audited in Phase 1

These are low-risk or reference/config tables. Can be added later if needed.

| Entity | Table | Reason |
|--------|-------|--------|
| AdminUser | `admin.admin_users` | Phase 2 — user/access management audit |
| Customer | `admin.customers` | Legacy CRM table, low write volume |
| Quotes / QuoteCompany | `admin.quotes` | Read-heavy, low compliance risk |
| FeatureFlag / FlagCompany / FlagRole | `admin.feature_flags` | Config data, Phase 2 |
| Vendor / VendorToken / VendorApiEndpoint / VendorApiHeader | `admin.vendor_*` | Integration config, Phase 2 |
| IncentivePackage / IncentiveRule / IncentiveRuleSlab | `admin.incentive_*` | Sales config, Phase 2 |
| InsuranceProvider | `admin.insurance_providers` | Master data, rarely changes |
| Document | `document.documents` | Phase 2 — document management audit |
| ZohoToken | `admin.zoho_token` | Token storage, security risk to audit |

---

## Querying Audit Data

Envers provides a built-in query API. Extend repositories with `RevisionRepository`:

```java
public interface IDealRepository
    extends JpaRepository<Deals, UUID>,
            JpaSpecificationExecutor<Deals>,
            RevisionRepository<Deals, UUID, Integer> {  // ← add this
}
```

This gives us:

```java
// Get all revisions for a customer
Revisions<Integer, Deals> history = dealRepository.findRevisions(customerId);

// Get entity state at a specific revision
Optional<Revision<Integer, Deals>> snapshot = dealRepository.findRevision(customerId, revisionNumber);

// Get last change
Optional<Revision<Integer, Deals>> latest = dealRepository.findLastChangeRevision(customerId);
```

For admin-facing audit log API (future):
```java
// AuditReader for complex queries
AuditReader reader = AuditReaderFactory.get(entityManager);
List<Object[]> results = reader.createQuery()
    .forRevisionsOfEntity(Deals.class, false, true)
    .add(AuditEntity.id().eq(customerId))
    .add(AuditEntity.revisionProperty("userEmail").eq("admin@vima.com"))
    .getResultList();
```

---

## Javers Cleanup (Part of Ticket 1)

Remove Javers entirely during Envers setup — clean cut, no dual-running:

1. Remove `@DiffIgnore` annotations from Deals, DealEndorsement, Endorsement entities
2. Remove `javers-core` dependency from `pom.xml`
3. Remove any Javers configuration or comparison logic

---

## Implementation Order

```
Ticket 1 → Ticket 2 → Ticket 3 → Ticket 4 → Ticket 5
  Setup      Core        Financial   Endorsements  Enrollment
  (1-2 days) (1 day)     (1 day)     (1 day)       (1-2 days)
```

**Total estimate:** ~5-7 days

---

## Decisions (Resolved)

| Question | Decision |
|----------|----------|
| `personal_details` JSONB in enrollment_submissions | **Excluded** — `@NotAudited`. Not needed for now. |
| Javers removal | **Remove in Ticket 1** alongside Envers setup. Clean cut — no dual-running. |
| Admin audit log UI / API | **Phase 1 = storage only.** Retrieval API planned for Phase 2. |
| GDPR / hard deletes | **Not applicable.** No hard deletes in the system — all data soft-deleted. Audit entries persist indefinitely. |
| Bulk operations | **One batch revision** per bulk upload. All employee changes within a single endorsement bulk upload share one revision ID. |
