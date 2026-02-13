# Keycloak Migration Plan - Vima Backend

**From:** Authentik (OAuth2/OIDC Provider)  
**To:** Keycloak (OAuth2/OIDC Provider)  
**Date:** February 13, 2026  
**Status:** Planning Phase

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current Authentik Integration Analysis](#current-authentik-integration-analysis)
3. [Product Context: Endorsements and Batch-Level Employees](#product-context-endorsements-and-batch-level-employees)
4. [Migration Objectives](#migration-objectives)
5. [Keycloak Architecture Design](#keycloak-architecture-design)
6. [Backend Code Changes](#backend-code-changes)
7. [Keycloak Configuration](#keycloak-configuration)
8. [Keycloak Flows Plan](#keycloak-flows-plan)
9. [Other Keycloak Features to Discuss](#other-keycloak-features-to-discuss)
10. [Keycloak: Disadvantages and Current Vulnerabilities](#keycloak-disadvantages-and-current-vulnerabilities)
11. [Audit-Ready Compliance Implementation Plan](#audit-ready-compliance-implementation-plan)
12. [Data Migration Strategy](#data-migration-strategy)
13. [Migration Users Plan to Keycloak](#migration-users-plan-to-keycloak)
14. [Testing Strategy](#testing-strategy)
15. [Rollout Plan](#rollout-plan)
16. [Rollback Strategy](#rollback-strategy)
17. [Open Questions](#open-questions)

---

## Executive Summary

This document outlines the migration strategy from Authentik to Keycloak for the Vima Insurance Backend. The migration will maintain two separate realms (dev and prod) in Keycloak, preserve all existing roles and organization groups, and ensure zero downtime during the transition.

**Key Goals:**
- Migrate JWT-based authentication from Authentik to Keycloak
- Maintain existing user roles (SUPER_ADMIN, VIMA_ADMIN, HR_ADMIN, SALES_MANAGER, SALES_AGENT)
- Preserve organization-based multi-tenancy (ORG_* groups)
- Implement custom UI themes for Keycloak
- Configure email templates and policies
- Update AuthentikUtil to KeycloakUtil with full API compatibility
- **Future:** Keycloak setup will support a planned **React Native (mobile) app** with a dedicated mobile client and PKCE
- **Compliance:** Full audit-ready implementation plan for **IRDAI (ISNP)**, **SOC 2 Type I & II**, and **ISO 27001** with control mappings, checklists, evidence packages, and continuous monitoring procedures; see [Audit-Ready Compliance Implementation Plan](#audit-ready-compliance-implementation-plan)

---

## Current Authentik Integration Analysis

### 1. Integration Points

#### A. JWT Token Validation (`SecurityConfig.java`)
```java
// Current Authentik Configuration
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://api.vimainsurance.com/application/o/vima/
```

**Files Affected:**
- `src/main/java/com/vimainsurance/vimaadmin/config/SecurityConfig.java`
- `src/main/java/com/vimainsurance/vimaadmin/config/AuthentikJwtAuthenticationConverter.java`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`
- `src/main/resources/application-uat.properties`

**Current JWT Claims Used:**
- `groups` - List of groups (roles + organizations)
- `email` - User email
- `preferred_username` - Username
- `sub` - User subject/ID
- `realm_access.roles` - Keycloak-style roles (already supported!)
- `resource_access` - Client-specific roles
- Custom claims: `company_id`, `organization_ids`, `organizations`

#### B. Authentik REST API Integration (`AuthentikUtil.java`)

**Current Authentik API Endpoints Used:**
1. **User Management:**
   - `GET /core/users/` - Get users with pagination, filtering, search
   - `POST /core/users/` - Create user
   - `POST /core/users/{id}/set_password/` - Set user password

2. **Group Management:**
   - `GET /core/groups/` - Get all groups (roles + organizations)
   - `POST /core/groups/` - Create group

**API Features:**
- Pagination (page, page_size)
- Search (username, email, name)
- Filtering (is_active, groups_by_name)
- Sorting (ordering)
- Custom attributes (user_id in attributes field)

**Files Affected:**
- `src/main/java/com/vimainsurance/vimaadmin/util/AuthentikUtil.java` (568 lines)
- `src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl/AdminUserServiceImpl.java`
- `src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl/EndorsementServiceImpl.java`

#### C. JWT User Extraction (`JwtUserExtractor.java`)

**Current Claims Extraction:**
- Email: `jwt.getClaimAsString("email")`
- Username: `jwt.getClaimAsString("preferred_username")`
- Groups: `jwt.getClaim("groups")` (also checks `realm_access.roles`)
- Organizations: `jwt.getClaim("organization_ids")` or `organizations`
- Company ID: `jwt.getClaimAsString("company_id")`

**Files Affected:**
- `src/main/java/com/vimainsurance/vimaadmin/util/JwtUserExtractor.java` (402 lines)

#### D. DTOs for Authentik API

**Files Affected:**
- `src/main/java/com/vimainsurance/vimaadmin/dto/AuthentikUserCreationDto.java`
- `src/main/java/com/vimainsurance/vimaadmin/dto/AuthentikPaginatedResponse.java`
- `src/main/java/com/vimainsurance/vimaadmin/dto/AuthentikGroupsResponseDto.java`
- `src/main/java/com/vimainsurance/vimaadmin/dto/AuthentikGroupCreationDto.java`

### 2. Current Group/Role Structure

**Role Groups (ROLE_* prefix):**
- `ROLE_SUPER_ADMIN`
- `ROLE_ADMIN`
- `ROLE_VIMA_ADMIN`
- `ROLE_SALES_MANAGER`
- `ROLE_SALES_AGENT`
- `ROLE_HR_ADMIN`

**Organization Groups (ORG_* prefix):**
- `ORG_MAIN`
- `ORG_{ORGANIZATION_NAME}` (dynamic, per organization)

**Current Behavior:**
- Users can have multiple roles (e.g., ROLE_VIMA_ADMIN + ORG_MAIN)
- Groups are used for both RBAC (roles) and multi-tenancy (organizations)
- JWT token includes all groups in the `groups` claim

### 3. Email Configuration

**Current Email Templates:**
- `src/main/resources/templates/email/welcome.html` - Welcome email
- `src/main/resources/templates/email/password-reset.html` - Password reset
- `src/main/resources/templates/email/enrollment-invitation.html` - Enrollment invitation
- `src/main/resources/templates/email/quote-notification.html` - Quote notification

**Current Email Provider:**
- SMTP via Gmail (`smtp.gmail.com:587`)
- Configured in `application-*.properties`
- Emails sent from backend using Spring Mail

**Note:** Authentik does NOT control these emails currently - they are sent by the backend application.

---

## Product Context: Endorsements and Batch-Level Employees

This section describes the **endorsement (batch)** model and **batch-level employees** in Vima. Endorsements are the unit of work for adding/removing employees on a policy; employees are created and viewed at the **batch (endorsement) level**. This is product scope that the Keycloak migration does not change, but it is documented here for context and so that any future identity or reporting work (e.g. linking batch employees to orgs/roles) is consistent.

### Endorsement = Batch

- An **endorsement** is a batch of changes (additions/deletions of employees) for an organization, optionally tied to an enrollment window.
- Stored in `cpc.endorsements` (fields: `endorsement_id`, `organization_id`, `total_employees`, `total_dependents`, `status`, `enrollment_window_id`, `source`, etc.).
- Sources: CSV upload, self-enrollment (HR approval flow), or other batch flows.

### Batch-Level Employees

- **Batch-level employees** are employees (and dependents) that belong to a specific endorsement (batch).
- They are stored in `cpc.customers` (Deals) with `endorsement_id` set, and optionally in the junction table `cpc.deal_endorsements` (linking `individual_id` to `endorsement_id`).
- Each endorsement has:
  - **total_employees** / **total_dependents** (counts)
  - A list of employees/dependents that can be **created** and **viewed** at the batch level.

### Creating Batch-Level Employees

Employees can be associated with an endorsement (batch) in these ways:

| Method | Description | API / flow |
|--------|-------------|------------|
| **CSV upload** | Bulk create/update employees and attach to endorsement | Endorsement approve/upload flow |
| **Self-enrollment** | Employee submits via enrollment; HR approval creates/links to endorsement | `HRApprovalServiceImpl` → `createEndorsementAndDealEndorsementsForSubmission` |
| **Employee onboarding** | Trigger onboarding for an endorsement (batch) | `POST /api/v1/endorsements/{endorsementId}/employee-onboarding` |
| **Batch insert (backend)** | Programmatic batch insert of Deals for an org, then link to endorsement | `EmployeeBatchService.batchInsertDeals()`; set `Deals.endorsementId` and create `DealEndorsement` records |

**Planned / in scope:** Support for **creating** batch-level employees explicitly (e.g. “Add employees to this endorsement” via API or UI) and ensuring they are visible in the endorsement detail view.

### Creating Keycloak Users for the Batch

When batch-level employees are created for an endorsement, **create corresponding users in Keycloak for that batch** so they can authenticate (e.g. for self-service enrollment, portal login, or password reset emails).

**Flow:**

1. **After employees are created/linked to the endorsement (batch):**
   - For each **primary employee** (relationship = SELF/EMPLOYEE) in the batch who should have login access:
     - Call Keycloak Admin API (via **KeycloakUtil**) to create a user.
   - Dependents typically do not get Keycloak users unless they have their own login.

2. **Keycloak user creation per batch employee:**
   - **Username:** e.g. employee email or `employee_number` (must be unique in realm).
   - **Email:** from Deals/customer record.
   - **Name:** full name from Deals.
   - **Realm role:** `ROLE_EMPLOYEE` (or equivalent) so they can access employee-facing flows only.
   - **Group(s):** organization group for the endorsement’s organization (e.g. `ORG_<org_name>`), so tenant isolation and JWT claims are correct.
   - **Attributes (optional):** `user_id` = Vima internal id (e.g. `individual_id` or admin user id if linked), `company_id` if used.
   - **Password:** set temporary password + required action `UPDATE_PASSWORD`, or send Keycloak “reset password” link (no temp password).

3. **Link Vima data to Keycloak:**
   - Store Keycloak user ID (`sub` or user UUID) on the customer/Deals record or in a mapping table (e.g. `oauth_provider_id` if you extend the schema for employees) so you can later update password, disable user, or sync.

4. **Batch processing:**
   - For large batches, create Keycloak users in a loop with rate limiting (e.g. Bucket4j or Keycloak built-in).
   - On failure for one user, log and continue; optionally retry or queue for manual fix.

**Backend (post–Keycloak migration):**
- Use **KeycloakUtil** (or equivalent) in the same flow that creates batch-level employees:
  - After `EmployeeBatchService.batchInsertDeals()` (or after CSV import / HR approval creates Deals and links to endorsement), call `KeycloakUtil.createUser(...)` for each primary employee in that endorsement.
- Realm: use the realm for the current environment (e.g. `vima-prod` / `vima-dev`).
- Idempotency: check by username/email if user already exists in Keycloak; skip or update.

**Example (pseudo):**
```text
For endorsement E with organization O:
  1. Get employees: Deals where endorsement_id = E and relationship in (SELF, EMPLOYEE).
  2. For each employee:
       KeycloakUtil.createUser(name, username, email, "EMPLOYEE", ["ORG_"+O.name], true, null, individualId);
       Optionally: send password reset link or set temporary password.
  3. Store Keycloak user id on Deals/customer if needed for future operations.
```

**Summary:** Creating batch-level employees for an endorsement includes **creating users in Keycloak for that batch** (one Keycloak user per primary employee, with role and org group), so they can log in and are consistent with the rest of the Keycloak migration.

### Viewing Batch-Level Employees in Endorsement

- **Get employees by endorsement:**  
  `GET /api/v1/endorsements/{endorsementId}/employees`  
  Returns paginated list of employees (and optionally dependents) for that endorsement (batch).
- **Endorsement detail:**  
  `GET /api/v1/endorsements/{endorsementId}`  
  Returns endorsement summary including `totalEmployees`, `totalDependents`; the list of individuals is available via the employees endpoint above.
- **Health ID upload (batch-level):**  
  `POST /api/v1/endorsements/{endorsementId}/health-id/upload`  
  Upload health IDs for employees in that endorsement.

### Backend Components (Reference)

| Component | Purpose |
|-----------|---------|
| `Endorsement` (entity) | Endorsement (batch) record; has `totalEmployees`, `totalDependents`, `dealEndorsements` |
| `DealEndorsement` (entity) | Links a Deal (individual) to an endorsement |
| `Deals` (entity) | Customer/employee; `endorsementId` links to batch |
| `EmployeeBatchService` | Batch insert/update of Deals (`batchInsertDeals`, `batchUpdateDeals`) |
| **`KeycloakUtil`** | **Create users in Keycloak for the batch** (role, org group, attributes); used after batch employees are created |
| `EndorsementController` | Endpoints: get by id, list employees (`/endorsements/{id}/employees`), employee-onboarding, confirm, etc. |
| `EndorsementEmployeeController` | Health ID upload for endorsement employees |
| `IEndorsementService` / `EndorsementServiceImpl` | Endorsement business logic, employee-onboarding, and orchestration of batch employee + Keycloak user creation |
| `IDealsRepository.findByEndorsementId()` | List deals (employees) by endorsement |

### Summary

- **Endorsement** = batch of employee changes for an org (and optionally an enrollment window).
- **Batch-level employees** = employees/dependents linked to that endorsement (`endorsement_id` on Deals, plus `deal_endorsements`).
- **Create:** Via CSV, self-enrollment + HR approval, employee-onboarding, or batch insert; in-scope to support explicit “create batch-level employees” for an endorsement.
- **Create users in Keycloak for that batch:** For each primary employee in the batch, create a Keycloak user (role e.g. `ROLE_EMPLOYEE`, org group, optional attributes) via KeycloakUtil so they can log in; use temp password or reset link and store Keycloak user id if needed.
- **View:** Use `GET /api/v1/endorsements/{endorsementId}/employees` to see batch-level employees in the endorsement; endorsement detail shows aggregate counts.

---

## Migration Objectives

### 1. Realm Structure

**Two Keycloak Realms:**

#### **Realm: `vima-dev`**
- Purpose: Development and UAT environment
- URL: `https://keycloak.vimainsurance.com/realms/vima-dev`
- JWT Issuer: `https://keycloak.vimainsurance.com/realms/vima-dev`
- Users: Development users + test accounts
- Connected to: `vima_dev` PostgreSQL database

#### **Realm: `vima-prod`**
- Purpose: Production environment
- URL: `https://keycloak.vimainsurance.com/realms/vima-prod`
- JWT Issuer: `https://keycloak.vimainsurance.com/realms/vima-prod`
- Users: Production users
- Connected to: `vima_prod` PostgreSQL database

### 2. Role Mapping Strategy

**Keycloak Roles (Realm Roles):**
- `SUPER_ADMIN` (realm role)
- `ADMIN` (realm role)
- `VIMA_ADMIN` (realm role)
- `SALES_MANAGER` (realm role)
- `SALES_AGENT` (realm role)
- `HR_ADMIN` (realm role)

**Keycloak Groups (for Organizations):**
- `ORG_MAIN` (group)
- `ORG_{ORGANIZATION_NAME}` (groups, dynamic)

**Mapping Strategy:**
- **Authentik Groups starting with `ROLE_`** → **Keycloak Realm Roles** (without ROLE_ prefix, will be added in JWT via mapper)
- **Authentik Groups starting with `ORG_`** → **Keycloak Groups** (preserved as-is)
- Users assigned to groups in Keycloak
- JWT will include both realm roles and group names

### 3. JWT Claims Mapping

**Required JWT Claims:**

| Claim Name | Source in Keycloak | Purpose |
|------------|-------------------|---------|
| `sub` | User ID | Unique user identifier |
| `email` | User email attribute | User email |
| `preferred_username` | Username | User login name |
| `groups` | Group membership paths | Organization groups (ORG_*) |
| `realm_access.roles` | Realm roles | User roles (VIMA_ADMIN, etc.) |
| `resource_access.{client}.roles` | Client roles (optional) | Client-specific roles |
| `organization_ids` | Custom mapper from groups | List of organization UUIDs |
| `company_id` | Custom user attribute | Company UUID (if applicable) |
| `user_id` | Custom user attribute | Internal user ID |

**Custom Mappers Needed:**
1. **Group Mapper** - Include group names in `groups` claim
2. **Organization IDs Mapper** - Extract organization UUIDs from groups and add to `organization_ids`
3. **Custom Attributes Mapper** - Map user attributes (user_id, company_id) to JWT

---

## Keycloak Architecture Design

### 1. Keycloak Deployment

**Hosting Options:**
- [ ] **Option A:** Self-hosted Keycloak on AWS EC2 (Docker/Kubernetes)
- [ ] **Option B:** Managed Keycloak service (e.g., Red Hat SSO, Cloudentity)
- [ ] **Option C:** Keycloak on AWS ECS/Fargate

**Recommended:** Option A - Self-hosted on AWS EC2/ECS with RDS PostgreSQL backend

#### EC2 Infrastructure Plan (Approved: Option 1 + Option 2)

Current setup: **One EC2 t3.medium (4GB RAM)** running Nginx, dev app, prod app, Authentik server, and Authentik worker. Adding Keycloak requires more memory. The following two options are approved for planning.

---

**Option 1: Upgrade to t3.large (Single Instance)**

| Item | Detail |
|------|--------|
| **Instance** | t3.large (2 vCPU, 8 GB RAM) |
| **Cost** | ~$60/month (+$30/month from current) |
| **Use case** | Simplest migration; all services on one instance |

**Layout (during migration – Authentik + Keycloak):**

| Service | Est. Memory |
|---------|-------------|
| Nginx | ~100 MB |
| Dev app | ~1 GB |
| Prod app | ~1 GB |
| Authentik server/worker | ~800 MB |
| Keycloak | ~2.5 GB |
| System overhead | ~500 MB |
| **Total** | **~5.9 GB / 8 GB (~74%)** ✅ |

**Layout (after migration – Authentik removed):**

| Service | Est. Memory |
|---------|-------------|
| Nginx | ~100 MB |
| Dev app | ~1 GB |
| Prod app | ~1 GB |
| Keycloak | ~2.5 GB |
| System overhead | ~500 MB |
| **Total** | **~5.1 GB / 8 GB (~64%)** ✅ |

**Pros:** Single instance, simple ops, enough headroom.  
**Cons:** Dev and prod still share one box; no HA.

---

**Option 2: Separate Instance for Keycloak (Two Instances)**

| Item | Detail |
|------|--------|
| **Instance 1** | t3.medium (4 GB) – Apps (Nginx, dev app, prod app) |
| **Instance 2** | t3.medium (4 GB) – Keycloak (+ Nginx for Keycloak SSL) |
| **Cost** | ~$60/month total (same as Option 1) |

**Instance 1 – Application server:**

| Service | Est. Memory |
|---------|-------------|
| Nginx (apps + reverse proxy) | ~100 MB |
| Prod app | ~1 GB |
| Dev app | ~1 GB |
| System | ~500 MB |
| **Total** | **~2.6 GB / 4 GB** ✅ |

**Instance 2 – Keycloak server:**

| Service | Est. Memory |
|---------|-------------|
| Nginx (Keycloak SSL only) | ~100 MB |
| Keycloak | ~2.5 GB |
| System | ~500 MB |
| **Total** | **~3.1 GB / 4 GB** ✅ |

**Pros:** Isolation (Keycloak issues don’t affect apps), same cost as Option 1, easier to add HA later.  
**Cons:** Two instances to manage; Nginx/ALB config for routing.

---

**Decision:** Plan supports **both Option 1 and Option 2**. Choose Option 1 for fastest migration; choose Option 2 for better isolation and future HA. Long-term, consider separating dev to its own instance and keeping prod + Keycloak on dedicated instances.

### 2. Keycloak Database

**Keycloak Database:**
- Separate PostgreSQL database for Keycloak itself (not `vima_dev` or `vima_prod`)
- Schema: `public` (Keycloak default)
- Database name: `keycloak`

**User Data Sync:**
- Keycloak manages authentication users
- Vima backend `admin.admin_users` table maintains user metadata
- Sync via `oauth_provider_id` field (Keycloak user UUID)

### 3. Keycloak Realm Configuration

#### **Realm: vima-dev**

**Realm Settings:**
- Display name: "Vima Development"
- Enabled: Yes
- User registration: Disabled (admin-created only)
- Email as username: No (separate username)
- Login with email: Yes
- Duplicate emails: Not allowed
- Verify email: Yes (optional for dev)
- Require SSL: All requests

**Authentication Flow:**
- Standard browser flow (username + password)
- Direct access grants enabled (for API clients)
- MFA/2FA: Optional (can enable later)

**Token Settings:**
- Access token lifespan: 15 minutes (900 seconds) - matches current `jwt.expiration`
- Refresh token lifespan: 7 days (604800 seconds) - matches current `jwt.refreshexpiration`
- Access token type: Bearer

**Client Configuration:**

**Client: `vima-webapp`** (React frontend)
- Client ID: `vima-webapp`
- Client protocol: `openid-connect`
- Access type: Public
- Standard flow enabled: Yes
- Direct access grants: No
- Valid redirect URIs:
  - `https://www.vimainsurance.com/*`
  - `https://preview--vima-webapp.lovable.app/*`
  - `http://localhost:8080/*` (dev only)
- Web origins: Same as redirect URIs
- Backchannel logout: Enabled

**Client: `vima-backend`** (Spring Boot backend - Resource Server)
- Client ID: `vima-backend`
- Client protocol: `openid-connect`
- Access type: Confidential
- Client authentication: Yes
- Authorization enabled: No (just resource server)
- Service accounts enabled: Yes (for admin API access)
- Valid redirect URIs: Not applicable (resource server only)

**Client: `vima-mobile`** (Future – React Native app)
- Client ID: `vima-mobile`
- Client protocol: `openid-connect`
- Access type: Public (no client secret; use PKCE)
- Standard flow enabled: Yes (Authorization Code + PKCE)
- Direct access grants: No
- Valid redirect URIs: Custom scheme for deep link (e.g. `com.vimainsurance.app://callback`, `vima://callback`)
- Web origins: N/A for native; ensure backend allows same JWT issuer for web and mobile
- **Purpose:** When the React Native app is built, users will log in via Keycloak in a browser or in-app WebView; same JWT is validated by the existing Vima backend (no backend change for auth).

#### **Realm: vima-prod**
- Same configuration as `vima-dev` but with production URLs
- Verify email: Yes (enforced)
- MFA/2FA: Optional (recommended for VIMA_ADMIN, SUPER_ADMIN)

### 4. Custom Keycloak Configuration

#### **User Attributes (Custom Fields):**
- `user_id` - Internal Vima user ID (UUID, mapped from `admin.admin_users.id`)
- `company_id` - Company UUID (for multi-company support)
- `agent_id` - Agent ID (for sales agents)

#### **Protocol Mappers (JWT Claims):**

**Mapper 1: Group Membership**
- Name: `groups`
- Mapper Type: Group Membership
- Token Claim Name: `groups`
- Full group path: Yes
- Add to ID token: Yes
- Add to access token: Yes
- Add to userinfo: Yes

**Mapper 2: Realm Roles**
- Name: `realm-roles`
- Mapper Type: User Realm Role
- Token Claim Name: `realm_access.roles`
- Add to ID token: Yes
- Add to access token: Yes

**Mapper 3: Organization IDs (Custom Script)**
- Name: `organization_ids`
- Mapper Type: Script Mapper
- Script: Extract UUIDs from groups starting with `ORG_`
- Token Claim Name: `organization_ids`
- Claim JSON Type: JSON Array

**Mapper 4: User Attributes**
- Name: `user-attributes`
- Mapper Type: User Attribute
- User Attribute: `user_id`, `company_id`, `agent_id`
- Token Claim Name: Same as attribute name
- Add to ID token: Yes
- Add to access token: Yes

### 5. Keycloak Admin API Access

**Service Account for Backend:**
- Client: `vima-backend`
- Service account enabled: Yes
- Service account roles:
  - `realm-admin` (for user/group management)
  - `manage-users`
  - `manage-groups`
  - `view-users`
  - `view-groups`

**Authentication:**
- Client credentials grant
- Client ID: `vima-backend`
- Client secret: Generated by Keycloak (store in `application-*.properties`)

---

## Backend Code Changes

### Phase 1: Create Keycloak Integration (Parallel to Authentik)

**Goal:** Build Keycloak integration without breaking existing Authentik functionality.

#### 1.1 Create KeycloakUtil.java

**File:** `src/main/java/com/vimainsurance/vimaadmin/util/KeycloakUtil.java`

**Features:**
- Admin API client using Keycloak Admin Client library
- Methods matching AuthentikUtil interface:
  - `getUsers(Integer page, Integer pageSize)`
  - `getUsersWithFilters(String search, Boolean isActive, String ordering, List<String> groupsByName, Integer page, Integer pageSize)`
  - `getAllUsers()`
  - `createUser(String name, String username, String email, String role, List<String> organizations, Boolean isActive, String temporaryPassword, String individualId)`
  - `setUserPassword(String userId, String password)`
  - `getRoles()` - Get realm roles
  - `getOrganizations()` - Get groups (ORG_*)
  - `getGroupIdByName(String groupName)`
  - `createGroup(GroupCreationDto groupCreationDto)`

**Dependencies to Add (pom.xml):**
```xml
<!-- Keycloak Admin Client -->
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-admin-client</artifactId>
    <version>26.0.0</version> <!-- Latest stable -->
</dependency>
```

**Configuration Properties:**
```properties
# Keycloak Configuration (dev)
keycloak.auth-server-url=https://keycloak.vimainsurance.com
keycloak.realm=vima-dev
keycloak.client-id=vima-backend
keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET}
keycloak.admin.username=${KEYCLOAK_ADMIN_USERNAME} # Alternative: service account
keycloak.admin.password=${KEYCLOAK_ADMIN_PASSWORD}

# OAuth2 Resource Server Configuration for Keycloak JWT Validation
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://keycloak.vimainsurance.com/realms/vima-dev
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://keycloak.vimainsurance.com/realms/vima-dev/protocol/openid-connect/certs
```

#### 1.2 Update/Rename AuthentikJwtAuthenticationConverter

**Option A: Rename to KeycloakJwtAuthenticationConverter**
- File: `src/main/java/com/vimainsurance/vimaadmin/config/KeycloakJwtAuthenticationConverter.java`
- Update to extract roles from `realm_access.roles` first, then fallback to `groups`
- Keep support for Authentik claims for backward compatibility during migration

**Option B: Create Unified JwtAuthenticationConverter**
- File: `src/main/java/com/vimainsurance/vimaadmin/config/UnifiedJwtAuthenticationConverter.java`
- Support both Authentik and Keycloak JWT structures
- Auto-detect provider based on issuer claim

**Recommended:** Option B for zero-downtime migration

#### 1.3 Update DTOs

**Rename/Create DTOs:**
- `KeycloakUserCreationDto.java` (or reuse AuthentikUserCreationDto with abstraction)
- `KeycloakPaginatedResponse.java` (or create common PaginatedResponse interface)
- `KeycloakGroupsResponseDto.java`

**Alternative:** Create abstraction layer with interfaces:
- `IAuthProvider` interface
- `AuthProviderUserDto` common DTO
- AuthentikUtil and KeycloakUtil both implement IAuthProvider

#### 1.4 Configuration Property Updates

**Add Feature Flag:**
```properties
# Auth Provider Selection (authentik or keycloak)
auth.provider=authentik  # Change to 'keycloak' when ready
```

**Update SecurityConfig.java:**
- Read `auth.provider` property
- Load appropriate JWT authentication converter
- Update issuer-uri based on provider

#### 1.5 Update AdminUserServiceImpl

**Strategy:** Use dependency injection to switch between AuthentikUtil and KeycloakUtil

```java
@Service
public class AdminUserServiceImpl implements IAdminUserService {
    
    @Autowired(required = false)
    private AuthentikUtil authentikUtil;
    
    @Autowired(required = false)
    private KeycloakUtil keycloakUtil;
    
    @Value("${auth.provider:authentik}")
    private String authProvider;
    
    private IAuthProviderUtil getAuthUtil() {
        return "keycloak".equals(authProvider) ? keycloakUtil : authentikUtil;
    }
    
    // Use getAuthUtil() for all auth provider operations
}
```

### Phase 2: Keycloak-Specific Features

#### 2.1 Custom Protocol Mappers (Keycloak SPI)

**If needed:** Develop custom Keycloak SPI for complex JWT claims transformations

**Example: Organization IDs Mapper**
```java
// Custom Protocol Mapper to extract organization UUIDs from groups
public class OrganizationIdsMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {
    
    @Override
    protected void setClaim(IDToken token, PrototypicalProtocolMapper mappingModel,
                          UserSessionModel userSession, KeycloakSession keycloakSession,
                          ClientSessionContext clientSessionCtx) {
        
        // Extract groups starting with ORG_
        // Look up organization UUIDs from database
        // Add to token as organization_ids claim
    }
}
```

**Deployment:**
- Package as JAR
- Deploy to Keycloak `providers/` directory
- Restart Keycloak

#### 2.2 User Federation (Optional)

**If existing users are in external directory:**
- Configure LDAP/AD user federation
- Custom user storage SPI for syncing with `admin.admin_users` table

**For Vima:** Not needed - users will be migrated via Admin API

---

## Keycloak Configuration

### 1. Realm Setup Steps

#### Step 1: Create vima-dev Realm

1. Login to Keycloak Admin Console
2. Create new realm: `vima-dev`
3. Configure realm settings:
   - Display name: "Vima Development"
   - Email settings: SMTP server configuration
   - Login settings: Email as username, verify email
   - Tokens: Access token 15min, refresh 7 days

#### Step 2: Create Realm Roles

Create the following realm roles:
- `SUPER_ADMIN`
- `ADMIN`
- `VIMA_ADMIN`
- `SALES_MANAGER`
- `SALES_AGENT`
- `HR_ADMIN`

#### Step 3: Create Groups

Create groups for organizations:
- `ORG_MAIN`
- (Other ORG_* groups will be created dynamically via KeycloakUtil)

#### Step 4: Create Clients

**Client: vima-webapp**
- Client ID: `vima-webapp`
- Root URL: `https://www.vimainsurance.com`
- Valid redirect URIs: `https://www.vimainsurance.com/*`, `http://localhost:8080/*`
- Web origins: `+` (same as redirect)
- Access type: Public

**Client: vima-backend**
- Client ID: `vima-backend`
- Access type: Confidential
- Service accounts enabled: Yes
- Get client secret from Credentials tab

#### Step 5: Configure Protocol Mappers

For `vima-webapp` and `vima-backend` clients:

1. **Add Group Membership Mapper:**
   - Name: `groups`
   - Mapper Type: Group Membership
   - Token Claim Name: `groups`
   - Full group path: No (just group name)

2. **Add User Attribute Mappers:**
   - Name: `user_id`
   - User Attribute: `user_id`
   - Token Claim Name: `user_id`
   
   - Name: `company_id`
   - User Attribute: `company_id`
   - Token Claim Name: `company_id`

3. **Add Realm Roles Mapper:**
   - Should be included by default
   - Verify `realm_access.roles` is in JWT

#### Step 6: Configure Service Account Permissions

For `vima-backend` client:
1. Go to Service Account Roles tab
2. Assign `realm-admin` role (from realm-management client)
3. Or assign specific roles: `manage-users`, `manage-groups`, `view-users`, `view-groups`

### 2. Email Configuration

#### Keycloak Email Settings (Realm Settings → Email)

```properties
Host: smtp.gmail.com
Port: 587
From: noreply@vimainsurance.com
From Display Name: Vima Insurance
Reply To: support@vimainsurance.com
Enable StartTLS: Yes
Enable Authentication: Yes
Username: charan@vimainsurance.com
Password: [SMTP Password]
```

#### Email Templates Customization

**Keycloak Email Theme Location:**
```
keycloak/themes/{theme-name}/email/
  ├── messages/
  │   └── messages_en.properties
  └── html/
      ├── email-verification.ftl
      ├── password-reset.ftl
      ├── event-login_error.ftl
      └── ...
```

**Email Templates to Customize:**
1. **email-verification.ftl** - Email verification link
2. **password-reset.ftl** - Password reset link
3. **event-login_error.ftl** - Login error notification
4. **executeActions.ftl** - Required actions (e.g., update password)

**Strategy:**
- Create custom theme: `vima-theme`
- Copy base email templates from Keycloak base theme
- Customize HTML/CSS to match Vima branding
- Update text in `messages_en.properties`

**Example: password-reset.ftl**
```ftl
<html>
<body style="background-color: #f4f4f4; font-family: Arial, sans-serif;">
    <div style="max-width: 600px; margin: 0 auto; background: white; padding: 20px;">
        <h1 style="color: #2563eb;">Vima Insurance - Password Reset</h1>
        <p>Hello ${user.firstName!'there'},</p>
        <p>You requested to reset your password. Click the link below to reset it:</p>
        <a href="${link}" style="background: #2563eb; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;">Reset Password</a>
        <p>If you didn't request this, please ignore this email.</p>
        <p>This link will expire in ${linkExpiration} minutes.</p>
        <hr>
        <p style="font-size: 12px; color: #888;">Vima Insurance | support@vimainsurance.com</p>
    </div>
</body>
</html>
```

**Deployment:**
1. Create theme directory: `keycloak/themes/vima-theme/email/`
2. Copy and customize email templates
3. Update realm settings to use `vima-theme`

### 3. User-Facing UI Flows & Screen Templates (Highly Customizable)

**Important:** The screens described below are **user-facing authentication flows** (login, reset password, verify email, etc.). They are **not** the Keycloak Admin Console. The Admin Console is a separate UI for administrators only. All end-user screens are **highly customizable** via Keycloak themes (FreeMarker templates, CSS, and message bundles).

#### 3.1 User-Facing Flows vs Admin UI

| Type | Purpose | Customizable? | Who sees it |
|------|---------|---------------|-------------|
| **Login / Account theme** | Login, reset password, verify email, update password, register | **Yes – highly** (templates, CSS, copy) | End users (Vima app users) |
| **Email theme** | Password reset email, verify email, etc. | **Yes – highly** (FreeMarker + HTML) | End users (inbox) |
| **Keycloak Admin Console** | Manage realms, users, roles, clients | No (Keycloak default UI) | Admins only |

All flow screens below are **screen templates** you can fully restyle and reword to match Vima branding.

---

#### 3.2 Login Theme – Screen Templates (Flow Screens)

**Theme base path:** `keycloak/themes/vima-theme/login/`

**Directory structure:**
```
keycloak/themes/vima-theme/login/
  ├── theme.properties
  ├── resources/
  │   ├── css/
  │   │   └── login.css
  │   ├── js/
  │   │   └── login.js
  │   └── img/
  │       ├── vima-logo.png
  │       └── vima-favicon.ico
  ├── messages/
  │   └── messages_en.properties
  └── [FreeMarker templates – one per screen]
```

**Customizable flow screens (one template per user-facing screen):**

| Screen | Template file | When user sees it | Customization |
|--------|----------------|-------------------|---------------|
| **Login** | `login.ftl` | Username/password entry | Layout, logo, fields, buttons, links, copy |
| **Login (with OTP)** | `login-otp.ftl` | After password when OTP enabled | Same as login |
| **Forgot password (request)** | `login-reset-password.ftl` | User clicks “Forgot password?” | Form, copy, branding |
| **Reset password (set new)** | `login-reset-password.ftl` (or dedicated) | After clicking email reset link | Form, validation message, branding |
| **Update password** | `login-update-password.ftl` | First login / required action | Form, policy text, branding |
| **Email verification** | `login-verify-email.ftl` | After signup / verify-email required | Message, resend link, branding |
| **Registration** | `register.ftl` | Self-registration (if enabled) | Fields, validation, terms, branding |
| **Error** | `error.ftl` | Login/flow error | Error message, styling, back link |
| **Info** | `info.ftl` | Generic info (e.g. “Check your email”) | Message, styling |
| **Account (optional)** | Account theme | User profile/sessions (if used) | Same level of control |

Each template is **FreeMarker (.ftl)**. You control:
- **HTML structure** – full page layout, sections, containers
- **CSS** – via `resources/css/login.css` and inline if needed
- **Copy and labels** – via `messages/messages_en.properties` (e.g. `loginTitle=Vima Sign In`)
- **Images** – logo, favicon, backgrounds in `resources/img/`
- **Scripts** – optional `resources/js/login.js` for client-side behavior

**theme.properties:**
```properties
parent=keycloak
import=common/keycloak
styles=css/login.css
```

**login.css – Brand colors (example):**
```css
:root {
    --vima-primary: #2563eb;
    --vima-secondary: #1e40af;
}

.login-pf body {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

#kc-header-wrapper {
    background: white;
    padding: 20px;
    border-radius: 10px 10px 0 0;
}

.btn-primary {
    background-color: var(--vima-primary);
    border-color: var(--vima-primary);
}
```

---

#### 3.3 Email Theme – Email Templates (Flow Emails)

**Theme base path:** `keycloak/themes/vima-theme/email/`

**Customizable email templates (user-facing only, not admin UI):**

| Email flow | Template file | When sent | Customization |
|------------|---------------|-----------|---------------|
| **Password reset** | `password-reset.ftl` | User requested reset | Subject, body HTML, link, expiry text |
| **Email verification** | `email-verification.ftl` | Verify email required | Subject, body, verification link |
| **Execute actions** | `executeActions.ftl` | e.g. “Set password” / required actions | Subject, body, CTA link |
| **Event (e.g. login)** | `event-*.ftl` | Security events (optional) | Subject, body, event text |

All are **highly customizable**: HTML structure, styling, copy, and variables (e.g. `${user.firstName}`, `${link}`) in FreeMarker.

---

#### 3.4 Flow Customization Summary

- **All user-facing flow screens** (login, reset password, verify email, update password, register, error, info) are **highly customizable** via the **login theme** (templates + CSS + messages).
- **All user-facing flow emails** are **highly customizable** via the **email theme** (FreeMarker + HTML).
- **Keycloak Admin Console** is **not** customized for branding; it remains the default admin UI.
- Realm setting to use custom themes:
  - **Realm → Themes:** Login theme = `vima-theme`, Email theme = `vima-theme`.

**Deployment:**
1. Create theme directories under `keycloak/themes/vima-theme/` (login + email).
2. Customize the flow templates and resources as above.
3. Set realm login theme and email theme to `vima-theme`.
4. Restart Keycloak (or hot-reload if dev mode enabled).

---

## Keycloak Flows Plan

This section describes the OAuth2/OIDC flows used between the Vima frontend, Keycloak, and the Vima backend. Configuring these flows correctly in Keycloak is required for login, token refresh, logout, and backend API access.

### Flow Overview

| Flow | Used by | Purpose |
|------|---------|---------|
| **Authorization Code** | React frontend (vima-webapp) | User login via browser; get access + refresh tokens |
| **Token validation (OAuth2 Resource Server)** | Vima backend | Validate JWT on each API request |
| **Client Credentials** | Vima backend (KeycloakUtil) | Admin API: create users, assign roles/groups |
| **Refresh Token** | React frontend | Get new access token when expired |
| **Logout / End Session** | React frontend | Log user out and invalidate session |
| **Resource Owner Password** | Optional (e.g. legacy) | Direct username/password; avoid for new apps |

---

### 1. Authorization Code Flow (User Login)

**Used when:** User logs in from the React app (browser).

**Keycloak settings (client `vima-webapp`):**
- **Standard Flow Enabled:** Yes
- **Direct Access Grants:** No (no password in frontend)
- **Valid Redirect URIs:** e.g. `https://www.vimainsurance.com/*`, `http://localhost:8080/*`
- **Web Origins:** Same as redirect URIs (or `+`)

**Sequence:**

1. User clicks “Login” → frontend redirects to Keycloak:
   ```
   GET https://keycloak.../realms/vima-prod/protocol/openid-connect/auth
     ?client_id=vima-webapp
     &redirect_uri=https://www.vimainsurance.com/callback
     &response_type=code
     &scope=openid profile email
     &state=<random-state>
   ```

2. User enters username/password on Keycloak login page (custom theme).

3. Keycloak authenticates, creates session, redirects back with `code`:
   ```
   GET https://www.vimainsurance.com/callback?code=...&state=...
   ```

4. Frontend (secure, server-side or SPA) exchanges `code` for tokens:
   ```
   POST https://keycloak.../realms/vima-prod/protocol/openid-connect/token
   Content-Type: application/x-www-form-urlencoded

   grant_type=authorization_code
   &client_id=vima-webapp
   &code=<code>
   &redirect_uri=https://www.vimainsurance.com/callback
   ```

5. Keycloak returns:
   - **access_token** (JWT) – used in `Authorization: Bearer <access_token>` for API calls
   - **refresh_token** – used to get new access tokens without re-login
   - **id_token** (JWT) – optional; user identity for frontend
   - **expires_in** (e.g. 900 for 15 minutes)

**Plan:**
- [ ] Configure `vima-webapp` with correct redirect URIs and web origins per environment (dev/prod).
- [ ] Frontend stores access_token and refresh_token securely (e.g. memory + httpOnly cookie for refresh).
- [ ] Frontend sends access_token in `Authorization` header to Vima backend.

---

### 2. Token Flow (Access Token, Refresh Token, ID Token)

**Access token (JWT):**
- **Lifespan:** 15 minutes (realm setting: Realm → Tokens → Access Token Lifespan).
- **Use:** Sent by frontend to Vima backend on every API request.
- **Claims:** `sub`, `email`, `preferred_username`, `realm_access.roles`, `groups`, custom (`user_id`, `company_id`, etc.) via protocol mappers.

**Refresh token:**
- **Lifespan:** 7 days (Realm → Tokens → Refresh Token Lifespan).
- **Use:** When access token expires, frontend calls token endpoint with `grant_type=refresh_token` to get a new access_token (and optionally new refresh_token).
- **Keycloak endpoint:** `POST .../realms/{realm}/protocol/openid-connect/token` with `grant_type=refresh_token` and `refresh_token=<refresh_token>`.

**ID token:**
- Optional for frontend (e.g. display user info). Same issuer and expiry as access token; use access_token for API authorization.

**Plan:**
- [ ] Set realm token lifespans: Access = 15 min, Refresh = 7 days (match current JWT settings).
- [ ] Ensure protocol mappers add all required claims to the **access** token (backend only validates access token).
- [ ] Frontend implements refresh before access_token expiry (e.g. refresh when &lt; 2 min left).

---

### 3. Backend Resource Server Flow (JWT Validation)

**Used when:** Vima backend receives `GET /api/v1/...` with `Authorization: Bearer <access_token>`.

**No token request from backend:** Backend does not call Keycloak to “login”. It only validates the JWT.

**Sequence:**

1. Client sends: `Authorization: Bearer <access_token>`.
2. Spring Security OAuth2 Resource Server:
   - Reads `iss` (issuer) from JWT (e.g. `https://keycloak.../realms/vima-prod`).
   - Fetches JWK Set from `{issuer}/protocol/openid-connect/certs` (cached).
   - Verifies signature and expiry.
3. `JwtAuthenticationConverter` (or Keycloak-specific converter) maps JWT to Spring Security authorities (e.g. from `realm_access.roles` and `groups`).
4. Request is allowed or denied based on `@PreAuthorize` and tenant filters.

**Backend configuration:**
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://keycloak.../realms/vima-prod
# JWKS endpoint is derived: .../realms/vima-prod/protocol/openid-connect/certs
```

**Plan:**
- [ ] Set `issuer-uri` per environment (vima-dev vs vima-prod).
- [ ] No client_id/client_secret needed on backend for validation (public JWKS).
- [ ] Ensure JWT contains roles and groups so converter can build authorities and tenant context.

---

### 4. Client Credentials Flow (Backend Admin API)

**Used when:** Vima backend (KeycloakUtil) creates users, assigns roles/groups, resets passwords via Keycloak Admin API.

**Keycloak settings (client `vima-backend`):**
- **Access Type:** Confidential
- **Service Accounts Enabled:** Yes
- **Valid Redirect URIs:** Not required for client credentials

**Sequence:**

1. Backend obtains token for itself (no user context):
   ```
   POST https://keycloak.../realms/vima-prod/protocol/openid-connect/token
   Content-Type: application/x-www-form-urlencoded

   grant_type=client_credentials
   &client_id=vima-backend
   &client_secret=<secret>
   ```

2. Keycloak returns **access_token** (for service account).
3. Backend calls Keycloak Admin REST API with `Authorization: Bearer <service-account-access-token>`:
   - Create user: `POST /admin/realms/vima-prod/users`
   - Assign roles: `POST /admin/realms/vima-prod/users/{id}/role-mappings/realm`
   - Add to group: `PUT /admin/realms/vima-prod/users/{id}/groups/{groupId}`
   - Reset password: `PUT /admin/realms/vima-prod/users/{id}/reset-password`

**Plan:**
- [ ] Create confidential client `vima-backend` with service account enabled.
- [ ] Assign realm-management roles to service account (e.g. `manage-users`, `manage-groups`).
- [ ] Store client secret in config (e.g. `keycloak.client-secret`) or secret manager.
- [ ] KeycloakUtil (or equivalent) implements token retrieval (with caching) and Admin API calls.

---

### 5. Logout (End Session) Flow

**Used when:** User clicks “Logout” in the React app.

**Options:**

**A. Frontend-only logout (clear tokens):**
- Delete access_token and refresh_token from frontend storage.
- Backend has no session; next request without token is 401. No Keycloak call.

**B. Keycloak logout (recommended):**
- Redirect user to Keycloak end-session endpoint so Keycloak session is invalidated and refresh_token is invalidated:
  ```
  GET https://keycloak.../realms/vima-prod/protocol/openid-connect/logout
    ?post_logout_redirect_uri=https://www.vimainsurance.com
    &id_token_hint=<id_token>
  ```
- Then clear tokens in frontend.

**Plan:**
- [ ] Configure **Valid post logout redirect URIs** for `vima-webapp` in Keycloak (Realm → Clients → vima-webapp).
- [ ] Frontend: clear local tokens and optionally redirect to Keycloak logout URL with `post_logout_redirect_uri` and `id_token_hint` (if using OIDC logout).
- [ ] Backend: no change (stateless JWT; revoked refresh is enough for “no new access tokens”).

---

### 6. Password Reset Flow

**Used when:** User clicks “Forgot password” or after migration (required action).

**Sequence:**

1. User requests password reset (frontend or Keycloak login page “Forgot password?”).
2. Keycloak sends email with reset link (using realm SMTP and email theme).
3. User clicks link → Keycloak page to set new password (custom theme: `login-reset-password.ftl`).
4. Keycloak updates password and clears `UPDATE_PASSWORD` required action if set.
5. User can log in with new password.

**Backend-triggered reset (e.g. admin resets user password):**
- Call Admin API: `PUT .../users/{id}/reset-password` with new temporary password.
- Optionally add required action `UPDATE_PASSWORD` so user must change on first login.
- Optionally use Keycloak “execute actions” email to send reset link instead of sending temp password.

**Plan:**
- [ ] Configure realm SMTP and custom email theme for password reset emails.
- [ ] Customize `password-reset.ftl` and related messages.
- [ ] If backend triggers resets: implement Admin API call and optional “execute actions” email.

---

### 7. Required Actions Flow (First Login / Forced Actions)

**Used when:** New user or migrated user must set password or verify email.

**Common required actions:**
- **UPDATE_PASSWORD** – User must set new password on next login (e.g. after migration or admin reset).
- **VERIFY_EMAIL** – User must click link in email before full access.

**Sequence:**

1. User is created with required action (e.g. via Admin API or broker).
2. User logs in (username + temporary password or reset link).
3. Keycloak sees required action → redirects to action page (e.g. “Update password”).
4. User completes action → Keycloak clears that action → user is redirected to app.

**Plan:**
- [ ] For migrated users: set temporary password + required action `UPDATE_PASSWORD`.
- [ ] Customize `login-update-password.ftl` in theme.
- [ ] Optional: enable `VERIFY_EMAIL` in realm and customize verification email template.

---

### 8. Flow Summary Diagram

```
┌─────────────┐     Authorization Code      ┌─────────────┐     Token (JWT)      ┌─────────────┐
│   React     │ ──────────────────────────► │  Keycloak   │ ◄─────────────────── │   Vima     │
│   (SPA)     │  1. Redirect to login       │  (IdP)      │  2. Validate JWT     │   Backend   │
│             │  2. Redirect back with code │              │     (no call to KC)   │   (API)     │
│             │  3. Exchange code for tokens│              │                      │             │
└─────────────┘                              └─────────────┘                      └─────────────┘
       │                                            ▲                                      ▲
       │ Bearer access_token                        │                                      │
       └────────────────────────────────────────────┼──────────────────────────────────────┘
                                                    │
       ┌────────────────────────────────────────────┘
       │ Client Credentials (service account)
       ▼
┌─────────────┐     Admin REST API (users, roles, groups)
│   Vima      │ ───────────────────────────────────────► Keycloak
│   Backend   │     Authorization: Bearer <service-token>
│ KeycloakUtil│
└─────────────┘
```

---

### 9. Keycloak Client Settings Checklist (per flow)

| Client | Standard flow | Direct grant | Service account | PKCE | Redirect URIs | Post-logout URIs |
|--------|---------------|--------------|-----------------|------|----------------|------------------|
| vima-webapp | Yes | No | No | Optional | App origin + path | App origin |
| vima-backend | No | No | Yes | N/A | N/A | N/A |
| vima-mobile (future) | Yes | No | No | **Yes** | Deep link (e.g. `vima://callback`) | Deep link |

| Realm setting | Value |
|---------------|--------|
| Access token lifespan | 15 min |
| Refresh token lifespan | 7 days |
| SSO session idle | e.g. 30 min |
| SSO session max | e.g. 10 hours |

---

## Other Keycloak Features to Discuss

The following Keycloak features are worth discussing as part of the migration or for a future roadmap. Use this section in planning and stakeholder reviews.

### Identity Brokering & Social Login

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Identity brokering** | Use Keycloak as a broker to external IdPs (Google, Microsoft, etc.). User logs in with “Login with Google” and Keycloak issues your app’s tokens. | Already using Google OAuth in app config; consider moving “Login with Google” to Keycloak so one place manages social login. |
| **First-broker login flow** | Automatically link first-time social login to a new or existing Keycloak user (by email). | Reduces duplicate accounts; decide if you want auto-link by email or manual admin link. |
| **Multiple IdPs** | Add more providers (e.g. Microsoft Entra, Apple) per realm. | Future: if more “Login with X” options are needed. |

### User Federation & Directory Sync

| Feature | Description | Discuss |
|---------|-------------|---------|
| **LDAP / Active Directory** | Sync users from corporate directory; authenticate against AD/LDAP. | You said “maybe future compatible”; document as post-migration option. |
| **Kerberos** | SSO in corporate networks (Windows auth). | Only if internal/corporate SSO is a requirement. |
| **Custom user storage SPI** | Implement a custom connector (e.g. read users from Vima DB or external API). | If you need “users live in Vima, Keycloak only for auth” without full LDAP. |

### Account Console & Self-Service

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Account console** | Keycloak-hosted UI for profile, password change, sessions, linked accounts. | Use Keycloak’s account console, or build same features in your app and disable Keycloak account UI. |
| **Account API** | REST API for users to update profile, change password, list sessions. | Useful if your app will offer “My account” without redirecting to Keycloak. |
| **Delete account** | User-initiated account deletion (GDPR). | Decide if needed and who implements it (Keycloak + backend cleanup). |

### Security & Hardening

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Brute force detection** | Lock account after N failed logins; configurable wait time. | Recommended: enable and tune (e.g. 5 attempts, 30 min lockout). |
| **Password policy** | Min length, complexity, history, expiration (or no expiration per NIST). | Already decided “industry standard”; configure in realm (e.g. 12 char, complexity, no expiry). |
| **WebAuthn / Passkeys** | Passwordless (e.g. FIDO2 keys, device biometrics). | Future: consider for high-privilege roles or as optional second factor. |
| **OAuth 2.0 Device Authorization** | Flow for TVs, CLI, IoT (user authorizes on another device). | Only if you add non-browser clients. |
| **Certificate-based auth** | Client certificates for machine-to-machine or high-security users. | Niche; skip unless there’s a concrete requirement. |

### Sessions & Tokens

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Offline tokens** | Long-lived refresh tokens for background sync (e.g. mobile). | Use if you need “refresh in background” without re-login; otherwise standard refresh is enough. |
| **Token exchange** | Exchange one token for another (e.g. impersonation, service call). | Advanced; only if you need token exchange or delegation. |
| **Revocation** | Revoke refresh token or all sessions for a user. | Important for “Logout everywhere” or admin “disable user”; ensure backend or admin can revoke. |
| **Backchannel logout** | Keycloak notifies your app when user logs out so you can clear app session. | Optional; improves “single logout” across apps. |

### Authorization (Beyond Roles)

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Fine-grained authorization** | Resource-based permissions (e.g. “can edit endorsement X”). | Keycloak can hold policies; today you do org/role checks in backend—decide if you want to move any of that into Keycloak. |
| **Client scopes & optional claims** | Different clients get different JWT claims (e.g. backend gets `organization_ids`, web gets fewer). | Use to limit token size or exposure per client. |
| **Permissions in token** | Put fine-grained permissions in JWT (e.g. `permissions: ["endorsement:read", "endorsement:write"]`). | Alternative to “role only”; discuss if you need permission-level control. |

### Events, Audit & Compliance

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Event listeners** | Keycloak fires events (login, logout, register, password change, etc.). | Already planned: custom listener → S3 for audit. |
| **Admin events** | Events for admin actions (user created, role assigned, etc.). | Enable and send to same S3/audit pipeline for compliance. |
| **Event types** | Filter which events to store (e.g. only logins and failures). | Reduces noise and storage; define retention. |
| **User session details** | Log IP, user agent, last access per session. | Useful for security review and support. |

### Customization & Extensibility

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Custom authenticators** | Add a step to login (e.g. custom OTP, risk check). | Future: if you need non-standard MFA or custom checks. |
| **Authentication flows** | Reorder or add steps (e.g. terms acceptance, custom form). | For “I accept terms” or extra fields on login/register. |
| **JavaScript policies** | Write policy logic in JS (for fine-grained auth). | Advanced; only if you use Keycloak authorization and need dynamic rules. |
| **SPI extensions** | Custom providers (user storage, event listener, etc.). | Already using for audit → S3; consider for other integrations. |

### Multi-Tenancy & Scale

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Multiple realms** | You already have vima-dev and vima-prod. | Confirmed. |
| **Realm templates** | Clone a realm (e.g. new client gets a copy of “template” realm). | If you ever do B2B and need one realm per tenant. |
| **Keycloak clustering** | Multiple Keycloak nodes + shared DB for HA. | For future HA; document as next step after single instance. |
| **Cross-datacenter** | Replicate or federate across regions. | Only for global low-latency or DR. |

### Admin & Operations

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Realm export / import** | JSON export/import of realm (roles, clients, users, etc.). | Use for backup, dev→prod promotion, or disaster recovery. |
| **Admin CLI** | Script realm and user management (e.g. kcadm.sh). | Useful for automation and CI/CD. |
| **Health & metrics** | Keycloak health endpoint and metrics (Prometheus). | Enable for monitoring and alerting. |
| **Database failover** | Keycloak supports failover with PostgreSQL. | For HA; configure when you move to multi-node. |

### Localization & Accessibility

| Feature | Description | Discuss |
|---------|-------------|---------|
| **Realm localization** | Multiple languages for login, errors, emails. | You said “future” for multi-language; configure locale and add languages when needed. |
| **Theme per locale** | Different theme or copy per language. | When you add more locales. |
| **Accessibility** | Keycloak themes can be made WCAG-compliant. | Ensure custom login/account theme is accessible. |

### Future: React Native App

A **React Native (mobile) app** is planned. Keycloak will support it with minimal extra configuration; the same backend (JWT validation) works for web and mobile.

**Keycloak setup for mobile:**

| Item | Recommendation |
|------|-----------------|
| **Client** | Create a separate client `vima-mobile` (public, no client secret). |
| **Flow** | Authorization Code with **PKCE** (Proof Key for Code Exchange) – recommended for native/mobile apps (no secret in the app). |
| **Redirect** | Use a custom URI scheme or App Link (e.g. `com.vimainsurance.app://callback` or `vima://callback`) so Keycloak redirects back into the app after login. |
| **Tokens** | Same as web: access token (JWT) + refresh token. Backend validates the same JWT; no change to `issuer-uri` or resource server config. |
| **Storage** | Store tokens in secure storage (e.g. React Native Keychain / secure store), not plain AsyncStorage. |

**What to configure when the app is built:**

1. **Realm → Clients → Create `vima-mobile`:**
   - Access type: Public
   - Standard flow + PKCE: enabled
   - Valid redirect URIs: e.g. `com.vimainsurance.app://callback`, `vima://callback` (and dev/debug variants if needed)
   - No client secret

2. **React Native app:**
   - Use a library that supports OIDC + PKCE (e.g. `react-native-app-auth`, `expo-auth-session`, or Keycloak’s JS adapter with PKCE).
   - Open Keycloak login in in-app browser or system browser; handle redirect back to app via deep link.
   - Send received access_token in `Authorization: Bearer <token>` to the same Vima backend API.

3. **Backend:**
   - No change required: same `issuer-uri`, same JWT validation. Tokens from `vima-webapp` and `vima-mobile` are both issued by the same realm and accepted by the same resource server.

**Optional (later):**

- **Offline tokens:** If the app needs long-lived background refresh, enable “Offline access” for `vima-mobile` and request `scope=offline_access` to get a refresh token with longer lifetime.
- **Biometric / device auth:** After login, optionally protect the app with device PIN/biometric; Keycloak still handles only login and tokens.

**Summary:** Planning a React Native app is noted; Keycloak will support it via a public client `vima-mobile` with PKCE and deep-link redirect. Same realm, same backend JWT validation; add the client and redirect URIs when the mobile app is ready.

### Summary: Priority for Discussion

| Priority | Topics |
|----------|--------|
| **Migration / short-term** | Brute force detection, password policy, admin events to S3, account console vs in-app, revocation and backchannel logout. |
| **Post-migration** | Identity brokering (Google in Keycloak), LDAP/federation, account API, WebAuthn/passkeys. |
| **Future / optional** | **React Native app** (client `vima-mobile`, PKCE, deep link); fine-grained authorization, offline tokens, custom authenticators, realm templates, clustering. |

---

## Keycloak: Disadvantages and Current Vulnerabilities

This section summarizes **disadvantages** of Keycloak (for balanced decision-making) and **current or recent vulnerabilities** so the team can plan mitigations and stay updated.

### Keycloak Disadvantages

| Area | Disadvantage | Mitigation / note for Vima |
|------|--------------|----------------------------|
| **Total cost of ownership (TCO)** | Despite free licensing, production use often needs significant engineering time for setup, integration, and ongoing maintenance. Third-party TCO studies cite high ongoing maintenance (e.g. 3+ hours/week) compared to some commercial IdPs. | Plan for initial setup and documentation; use standard OAuth2/OIDC (no custom adapters) to limit long-term maintenance. |
| **Operational complexity** | Feature-rich and flexible, which means a steeper learning curve and more configuration (realms, clients, mappers, themes). Admin UI and concepts (realm, client, flow) take time to master. | Invest in runbooks and one or two trained admins; start with a minimal config (two realms, two clients). |
| **Resource usage** | Default Argon2 password hashing uses ~7 MB memory per hash request; under heavy login load this can add up. Keycloak itself typically needs 2–2.5 GB heap for production. | Size EC2 appropriately (see EC2 plan); consider tuning Argon2 iterations if needed; monitor memory. |
| **Adapter deprecation** | Keycloak 25+ removed many legacy Java adapters (Tomcat, WildFly, Spring Boot adapter, etc.). Applications must use standard OAuth2/OIDC libraries (e.g. Spring Security Resource Server) instead. | Vima already uses Spring Security OAuth2 Resource Server and JWT validation—no Keycloak-specific adapter; we are aligned with current approach. |
| **Upgrade cadence** | Keycloak releases frequently; upgrades can require config or theme changes. Skipping versions can make catching up harder. | Plan to stay on a supported branch (e.g. latest 26.x or 27.x), apply security patches, and test upgrades in dev first. |
| **Default security posture** | Some defaults (e.g. debug endpoints, bind addresses) have historically been too permissive; security hardening is required for production. | Disable debug in production; restrict admin access; follow Keycloak security hardening guide. |
| **Documentation sprawl** | Documentation spans many versions and both Red Hat and upstream; finding the right doc for your version can be tedious. | Pin to one major version and use that version’s docs; prefer official keycloak.org and Red Hat docs. |
| **Support** | Community support only unless you purchase Red Hat SSO or a third-party support contract. | Acceptable for Vima (self-hosted); if critical, consider Red Hat SSO or a support vendor later. |

**Summary:** Keycloak is powerful but operationally non-trivial. For Vima, using standard OAuth2/OIDC (no deprecated adapters), sizing infrastructure, and planning for upgrades and hardening keeps disadvantages manageable.

---

### Current and Recent Vulnerabilities

The following are **known or recent Keycloak vulnerabilities**. Always check official sources (Keycloak security advisories, CVE databases) before deployment and after upgrades.

**Where to check for updates:**
- **Keycloak Security Advisories:** https://github.com/keycloak/keycloak/security/advisories  
- **Keycloak GitHub Security:** https://github.com/keycloak/keycloak/security  
- **NVD / CVE Details:** Search for “Keycloak” and filter by date.

---

#### High priority (patch or avoid)

| CVE / issue | Description | CVSS | Mitigation |
|-------------|-------------|------|------------|
| **CVE-2024-3656** | Unguarded Admin REST API: some admin endpoints could be accessed by low-privilege users, enabling unauthorized admin actions. | 8.1 (High) | Upgrade to a fixed version (check advisory); restrict admin API to trusted networks and service accounts only; use least-privilege roles for `vima-backend` service account. |
| **CVE-2024-4540** | Sensitive information exposed in Pushed Authorization Requests (PAR): client parameters in cookies in plain text. Affected versions before 24.0.5. | — | Upgrade to 24.0.5 or later; if not using PAR, risk is lower but still upgrade. |
| **TLS renegotiation DoS** | TLS client-initiated renegotiation can be abused for denial of service. | High | Use a fixed Keycloak version; configure TLS on reverse proxy (Nginx) with renegotiation disabled or restricted. |

#### Medium priority (plan to patch)

| CVE / issue | Description | CVSS | Mitigation |
|-------------|-------------|------|------------|
| **CVE-2025-0604** | LDAP federation: authentication bypass after password reset due to missing LDAP bind validation (expired/disabled AD accounts could gain access). | 5.4 (Medium) | Upgrade to patched version; if using LDAP federation, prioritize. If not using LDAP, lower immediate risk. |
| **CVE-2025-2559** | JWT token cache DoS: very long token expiration (e.g. 24–48 hours) can cause cache to grow and lead to OutOfMemoryError. Affects Keycloak ≤ 26.1.4. | 4.9 (Moderate) | Upgrade to 26.1.5+ or limit access token lifespan to 15–60 minutes (Vima already uses 15 min). |
| **PAR / cookie info disclosure** | Related to CVE-2024-4540; ensure no sensitive data in PAR cookies. | Moderate | Upgrade; avoid storing sensitive data in client-side cookies for auth flows. |
| **Error description injection** | Error messages could be manipulated to support phishing or confusion. | Moderate | Upgrade to version with fix; use custom error pages to avoid exposing raw Keycloak errors to end users. |
| **SMTP injection** | SMTP configuration or user-controlled input could allow injection in email flow. | Moderate | Upgrade; restrict SMTP config and validate any user input used in emails. |
| **Debug default bind** | Debug interface bound to a too-permissive address. | Moderate | Disable debug in production; bind debug to localhost only if ever enabled. |
| **Deserialization (LDAP)** | Deserialization of untrusted data in LDAP user federation. | Moderate | Upgrade; use LDAP federation only if required and with trusted directory. |

#### Lower priority / operational

| Topic | Description | Mitigation |
|-------|-------------|------------|
| **Default credentials** | Default admin account if not changed. | Change default admin password immediately; use strong passwords and restrict admin console access. |
| **Admin console exposure** | Admin UI exposed on same host/port as public endpoints. | Put admin console behind VPN or IP allowlist; or use a separate admin URL and firewall. |

---

### Recommendations for Vima

1. **Before go-live:** Deploy a Keycloak version that includes fixes for CVE-2024-3656, CVE-2024-4540, and CVE-2025-2559 (or confirm not affected). Check https://github.com/keycloak/keycloak/security/advisories for the exact versions.
2. **Token lifespan:** Keep access token at 15 minutes (already planned); avoid very long-lived access tokens to reduce JWT cache DoS risk.
3. **Admin API:** Use a dedicated service account for `vima-backend` with minimum required roles (e.g. manage-users, manage-groups only); do not use realm-admin unless necessary.
4. **Network:** Restrict Keycloak admin console and Admin API to trusted IPs or VPN where possible.
5. **Ongoing:** Subscribe to Keycloak security advisories (GitHub watch or RSS) and plan quarterly upgrades for security patches; run vulnerability scans (e.g. Snyk, Dependabot) on the Keycloak image or package.

---

### Compliance and Regulatory Alignment

Keycloak supports alignment with **IRDAI**, **SOC 2**, and other regulatory and industry frameworks commonly required for insurance and financial services. Using Keycloak as the identity provider can help Vima demonstrate control over access, authentication, and auditability.

#### IRDAI (Insurance Regulatory and Development Authority of India)

| Requirement area | How Keycloak helps |
|------------------|---------------------|
| **Access control & identity** | Centralized user and role management; strong authentication (password policy, MFA); principle of least privilege via roles and groups. |
| **Audit trail** | Event listeners and admin events (login, logout, password change, user/role changes); export to S3 or SIEM for retention and audit. |
| **Data security** | Passwords hashed (e.g. Argon2); no storage of plain-text credentials; TLS for all connections. |
| **Segregation / multi-tenancy** | Realms and groups support separation by organization (e.g. vima-dev / vima-prod; ORG_* for tenant isolation). |

*Note:* IRDAI guidelines do not prescribe a specific IdP. Keycloak’s controls (access, audit, encryption, tenant isolation) can be referenced in compliance documentation and internal controls.

#### SOC 2 (Service Organization Control 2)

| Trust principle | Keycloak alignment |
|-----------------|---------------------|
| **Security** | Access control, MFA, password policies, session management, secure token handling; security advisories and patching. |
| **Availability** | Self-hosted deployment; can be run in HA (clustering) for availability commitments. |
| **Processing integrity** | Consistent authentication and token issuance; configurable flows and validation. |
| **Confidentiality** | Encryption in transit (TLS); hashed credentials; optional encryption at rest (DB/volume). |
| **Privacy** | User attributes and consent; ability to support data minimization via client scopes and optional claims; audit of access to user data. |

Keycloak’s **event and admin event logging**, **access controls**, and **encryption** support SOC 2 control narratives and evidence (e.g. CC6.1, CC6.2, CC6.3). Exporting events to S3 (as planned) supports log retention and review.

#### Other regulations and frameworks

| Regulation / framework | Relevance |
|------------------------|-----------|
| **GDPR** | User data (profile, consent); right to erasure (delete user in Keycloak + backend); audit of access; data minimization via scopes. |
| **RBI / financial sector** | Strong authentication, audit logs, and access control align with common RBI guidance for regulated entities. |
| **ISO 27001** | Identity and access management (A.9); logging and monitoring (A.12.4); cryptographic controls (A.10.1). Keycloak supports these control areas. |
| **NIST / OAuth 2.0 / OIDC** | Keycloak implements standard OAuth 2.0 and OIDC; aligns with NIST guidelines on digital identity and federation. |

#### Summary for Vima

- **IRDAI:** Keycloak supports access control, auditability, and tenant isolation relevant to insurance regulations.
- **SOC 2:** Keycloak’s security, logging, and encryption capabilities support SOC 2 trust principles and evidence.
- **Others:** Keycloak can be used as part of controls for GDPR, RBI, ISO 27001, and NIST-aligned identity practices.

Compliance remains the **organization’s responsibility**. Keycloak provides technical controls; Vima must configure them appropriately, retain and review logs, and document how Keycloak is used within your IRDAI, SOC 2, and other compliance frameworks.

---

## Audit-Ready Compliance Implementation Plan

> **📄 For detailed security, compliance, and audit implementation guidance, see:**  
> **[KEYCLOAK_SECURITY_COMPLIANCE_AUDIT_PLAN.md](./KEYCLOAK_SECURITY_COMPLIANCE_AUDIT_PLAN.md)**

This separate document provides a comprehensive, audit-ready implementation plan covering:

### Frameworks Covered
- **IRDAI (ISNP)** - Information Systems & Network Security Policy for insurance companies in India
- **SOC 2 Type I & II** - Trust Service Criteria for service organizations  
- **ISO 27001:2022** - Information Security Management System (ISMS)

### What's Included
1. **Control Mappings** - How Keycloak features align with regulatory requirements (IRDAI AC-1 to AC-4, SOC 2 CC6.x/CC7.x, ISO 27001 Annex A.5.15-A.12.4)
2. **Implementation Checklists** - 12+ tasks for IRDAI, 9 for SOC 2, 12 for ISO 27001 with owners, timelines, and status tracking
3. **Evidence Packages** - Complete list of artifacts needed for audits (password policy docs, access review logs, MFA reports, audit logs, TLS certificates, etc.)
4. **Continuous Monitoring** - Quarterly access reviews, monthly log reviews, security patching procedures
5. **Cross-Framework Controls** - 8 common controls that satisfy multiple regulations simultaneously
6. **Audit Preparation Timeline** - Week-by-week roadmap from documentation to go-live
7. **Pre-Audit Checklist** - Internal verification before engaging auditors
8. **Compliance Roadmap** - Post-migration quarterly activities

### Key Highlights
- **Single implementation satisfies multiple compliance frameworks** (password policy → IRDAI AC-1 + SOC 2 CC6.3 + ISO 27001 A.5.17)
- **Audit-ready from day one** with systematic evidence collection
- **Clear ownership** for each control (DevOps, Security, Backend, HR)
- **Reduced compliance overhead** through Keycloak automation

**See the full document for detailed tables, checklists, and implementation guidance.**

---

## Data Migration Strategy
| **AC-4: Privileged access** | Extra controls for admin users (MFA, monitoring, approval). | MFA for SUPER_ADMIN and VIMA_ADMIN roles (enforced in realm auth flow); admin actions logged in Keycloak admin events. | MFA enrollment for admins; admin event logs (S3); restricted admin console access (VPN/IP). |
| **AU-1: Audit logging** | Log authentication, authorization, admin changes; retain for min. period (e.g. 1 year). | Keycloak event listener → S3 (login, logout, password change, user creation, role assignment). | Event logs in S3; retention policy doc; quarterly log review. |
| **IA-1: Identification & authentication** | Only authorized individuals can access; MFA for sensitive access. | All API requests require JWT; admin console requires username/password (+ MFA for admins). | Auth failure logs; successful login logs; MFA config. |
| **SC-1: Transmission security** | All auth traffic encrypted (TLS 1.2+). | Keycloak behind Nginx with TLS 1.2+; no plain HTTP for auth. | SSL certificate; Nginx TLS config; SSL Labs test result. |

#### B. IRDAI ISNP Implementation Checklist (for Keycloak)

| # | Control | Implementation step | Owner | Due | Status |
|---|---------|---------------------|--------|-----|--------|
| 1 | **Password policy** | Configure realm password policy: 12 char min, complexity (upper/lower/digit/special), no expiry, history 5. | DevOps/Security | Week 1 | Pending |
| 2 | **MFA for admins** | Enable OTP (TOTP) in realm; set as required for SUPER_ADMIN, VIMA_ADMIN roles (via conditional auth flow). | Security | Week 2 | Pending |
| 3 | **Unique user IDs** | Verify realm enforces unique usernames and emails. | Backend | Week 1 | Pending |
| 4 | **RBAC roles** | Create realm roles matching Vima roles; document role→permission mapping. | Backend/Security | Week 1-2 | Pending |
| 5 | **Least privilege** | Service account `vima-backend` has only manage-users, manage-groups; no realm-admin unless needed. | DevOps | Week 2 | Pending |
| 6 | **Segregation of duties** | No user has both admin and dev roles; separation enforced in realm role assignments. | Security | Week 4 | Pending |
| 7 | **Access review process** | Quarterly access review (script to list users, last login, roles; manager approval; disable inactive). | Security/Ops | Pre-prod | Pending |
| 8 | **Audit logging (events)** | Event listener → S3 (login, logout, password change); admin events (user create, role assign). | Backend/DevOps | Week 2-3 | Pending |
| 9 | **Log retention** | S3 lifecycle policy: 1 year minimum retention for audit logs. | DevOps | Week 3 | Pending |
| 10 | **TLS 1.2+ enforcement** | Nginx TLS config; no SSLv3, TLS 1.0, TLS 1.1; only TLS 1.2 and 1.3. | DevOps | Week 1 | Pending |
| 11 | **Admin console restriction** | Admin console behind VPN or IP allowlist; or separate admin endpoint with firewall. | DevOps/Network | Week 2 | Pending |
| 12 | **Secure credential storage** | Keycloak client secret, DB password in secrets manager (e.g. AWS Secrets Manager) or encrypted config. | DevOps | Week 1 | Pending |

#### C. IRDAI ISNP Audit Evidence Package

| Document/artifact | Purpose | Where/how to generate |
|-------------------|---------|----------------------|
| **Password policy doc** | Shows strong password requirement. | Export realm password policy config; screenshot or JSON. |
| **User access matrix** | User → role mapping. | Keycloak Admin API script; CSV export (username, role, org, last login). |
| **Access review logs** | Quarterly review; approvals. | Review report with sign-offs; disabled user list. |
| **Audit logs** | Authentication, admin events. | S3 bucket with event logs; sample download; retention config. |
| **MFA enrollment report** | Which admins have MFA enabled. | Keycloak Admin API query; user list with MFA status. |
| **TLS certificate** | Encryption in transit. | SSL certificate (PEM); SSL Labs test result. |
| **Segregation of duties** | No conflicting roles. | User→role report; policy doc stating "no admin-dev overlap". |

---

### 2. SOC 2 Type I & II - Trust Service Criteria

**Applicability:** SOC 2 report for Vima's systems (if offering insurance as a service to customers or partners). Type I = design of controls; Type II = operating effectiveness over a period (typically 6-12 months).

#### A. SOC 2 Control Mapping (TSC - Trust Service Criteria)

**Common Criteria (CC) - Keycloak supports:**

| TSC | Control | Keycloak implementation | Evidence for audit |
|-----|---------|------------------------|-------------------|
| **CC6.1** | Logical access - restrict to authorized | JWT-based auth; @PreAuthorize on backend; Keycloak enforces authentication. | Keycloak realm config; Spring Security config; role mappings. |
| **CC6.2** | Register and authorize new users | Admin-created users (no self-registration currently); approval in Vima app before Keycloak user created. | User creation logs (admin events); approval workflow doc. |
| **CC6.3** | Password requirements | Strong password policy (12 char, complexity); no expiration per NIST. | Password policy config; evidence of enforcement (test login with weak password = rejected). |
| **CC6.6** | Remove access when no longer needed | Disable users in Keycloak when employee/contractor leaves; quarterly access reviews. | Offboarding checklist; disabled user list; quarterly review logs. |
| **CC6.7** | Restrict access to privileged functions | Admin roles (SUPER_ADMIN, VIMA_ADMIN) separate from regular; admin console IP-restricted; service account least privilege. | Role→permission mapping; admin console firewall rule; service account roles doc. |
| **CC6.8** | Restrict access to sensitive data | Tenant isolation via ORG_* groups; backend enforces org filtering; JWT contains only user's orgs. | Tenant filter code; org-scoped queries; JWT sample showing only user's org. |
| **CC7.2** | Detect and respond to security events | Keycloak events (failed logins, account lockout) → S3 → monitoring/alerting. | Event logs; alerting rules (e.g. 10+ failed logins); incident response plan. |
| **CC7.3** | Evaluate security events | Review logs quarterly (or monthly); investigate anomalies. | Log review report; anomaly investigation notes. |
| **A1.2** | Availability monitoring | Monitor Keycloak health; uptime; restart on failure. | Health endpoint checks; uptime logs; auto-restart config (e.g. Docker restart policy). |

#### B. SOC 2 Type II - Operating Effectiveness (6-12 months)

For **Type II**, auditor samples evidence **over time** (e.g. 3–12 months). After Keycloak go-live:

| Period | What to collect | Frequency |
|--------|-----------------|-----------|
| **Monthly** | Access review (if monthly) or log review; failed login report; MFA enrollment status. | Every 30 days |
| **Quarterly** | Formal access review (user list → manager approval); disable inactive users; security patch status. | Every 90 days |
| **On event** | User onboarding/offboarding; role change; admin actions; incident response for suspicious login. | As it occurs |
| **Continuous** | Event logs (login, logout, password change, admin events) in S3; uptime/availability monitoring. | Real-time |

Auditor will **sample** (e.g. 25–40 items) from each control population over the report period. Ensure logs and approvals are retained and accessible.

#### C. SOC 2 Implementation Checklist (for Keycloak)

| # | Control / TSC | Implementation | Owner | Evidence |
|---|---------------|----------------|--------|----------|
| 1 | **CC6.1 - Authentication** | Keycloak JWT validation on all /api/** endpoints; no unauthenticated access. | Backend | SecurityConfig; test case showing 401 without token. |
| 2 | **CC6.2 - User registration** | Admin-only user creation; admin approves before user created in Keycloak. | Backend | AdminUserServiceImpl code; approval log. |
| 3 | **CC6.3 - Password policy** | Realm password policy enforced (12 char, complexity, history 5). | DevOps | Password policy config; test: weak password rejected. |
| 4 | **CC6.6 - Access removal** | Offboarding checklist; disable Keycloak user; quarterly access review. | HR/Security | Offboarding tickets; disabled user report; quarterly review. |
| 5 | **CC6.7 - Privileged access** | Admin console IP-restricted; service account least privilege; admin role assignments documented. | DevOps/Security | Firewall rule; service account config; role mapping doc. |
| 6 | **CC6.8 - Data access restriction** | Tenant isolation via ORG_* groups; backend filters by org from JWT. | Backend | TenantFilter code; org-scoped query example; JWT sample. |
| 7 | **CC7.2 - Security monitoring** | Keycloak events → S3; alerting for failed logins (e.g. Slack/CloudWatch). | DevOps | Event log sample; alerting config and test alert. |
| 8 | **CC7.3 - Log review** | Quarterly (or monthly) review of Keycloak logs; investigate anomalies. | Security | Log review report with findings and actions. |
| 9 | **A1.2 - Availability** | Keycloak health monitoring; uptime target (e.g. 99.5%); auto-restart on failure. | DevOps/Ops | Health check config; uptime report; restart policy. |

#### D. SOC 2 Narrative and Policy Requirements

For SOC 2 report, prepare the following documents (with Keycloak sections):

1. **System description** – Include Keycloak role, architecture (realms, clients), and how it integrates with Vima backend.
2. **Access control policy** – Define roles, approval for new users, MFA for admins, quarterly reviews, offboarding.
3. **Password policy** – Minimum 12 char, complexity, no expiration, history 5; enforced by Keycloak.
4. **Logging and monitoring policy** – Events logged to S3, retention 1 year, quarterly review, alerting.
5. **Change management policy** – Keycloak upgrades tested in dev, approved before prod; document changes.
6. **Incident response plan** – Process for security events (e.g. brute force, compromise); Keycloak logs used for investigation.

---

### 3. ISO 27001:2022 - Information Security Management System

**Applicability:** ISO 27001 certification (if Vima is pursuing or already certified). Keycloak supports multiple Annex A controls (access, logging, crypto).

#### A. ISO 27001 Annex A Control Mapping (Identity & Access)

| Annex A control | Control objective | Keycloak implementation | Evidence for audit |
|-----------------|-------------------|------------------------|-------------------|
| **A.5.15 - Access control** | Access to information and systems is restricted. | JWT authentication; @PreAuthorize on endpoints; Keycloak realm roles and groups. | Access control policy; SecurityConfig; role mappings; test of unauthorized access = denied. |
| **A.5.16 - Identity management** | Unique identities for users; lifecycle (create, modify, delete). | Keycloak user management; unique username/email; KeycloakUtil for CRUD; admin events logged. | User list; admin events (user created/disabled); KeycloakUtil code. |
| **A.5.17 - Authentication information** | Secure storage/transmission of auth info; no plain-text passwords. | Passwords hashed (Argon2); TLS for transmission; no plain-text in DB or logs. | Password policy; TLS config; DB schema (password hash only); test logs (no passwords). |
| **A.5.18 - Access rights** | Access granted per role and need-to-know; reviewed periodically. | Realm roles; quarterly access reviews; disable on separation. | Access review logs (quarterly); role→permission mapping; offboarding records. |
| **A.8.2 - Privileged access rights** | Extra control for privileged users (admins). | MFA for SUPER_ADMIN, VIMA_ADMIN; admin console IP-restricted; admin actions logged. | MFA config; IP allowlist; admin event logs. |
| **A.8.3 - Information access restriction** | Access limited per business need and tenant. | ORG_* groups; TenantFilter; JWT contains only user's orgs; backend enforces org-scoped queries. | TenantFilter code; JWT sample; org-scoped query example. |
| **A.8.5 - Secure authentication** | Multi-factor for remote or high-risk access. | Optional MFA; can be enforced per role or conditionally. | MFA config; conditional flow (if implemented). |
| **A.8.10 - Deletion of information** | Remove user data when no longer needed (GDPR, right to erasure). | Delete user in Keycloak + cascade delete in Vima DB (or anonymize); document retention period. | Deletion procedure; evidence of user deletion (logs, DB before/after). |
| **A.8.12 - Data leakage prevention** | Prevent unauthorized data exfiltration. | JWT contains only necessary claims (no excessive data); token expiry; session revocation. | JWT sample (minimal claims); token lifespan config; revocation test. |
| **A.8.13 - Information backup** | Backup critical data (user DB, config). | Keycloak DB (PostgreSQL) backed up via RDS snapshots; realm export for config backup. | RDS backup schedule; realm export file (JSON); restore test. |
| **A.12.4.1 - Event logging** | Log security events; timestamp; user; action; outcome. | Keycloak event logs (timestamp, user, IP, action, success/failure) → S3. | Event log sample; fields: timestamp, user, action, IP, result. |
| **A.12.4.2 - Log protection** | Logs protected from tampering or unauthorized access. | S3 with IAM (only Security role can read); no delete; versioning enabled. | S3 bucket policy; IAM role doc; S3 versioning enabled. |
| **A.12.4.3 - Admin/operator logs** | Admin actions logged. | Keycloak admin events (user created, role assigned, config change) → S3. | Admin event logs; sample entries. |
| **A.12.4.4 - Clock sync** | Accurate timestamps on logs. | NTP on Keycloak server; UTC timestamps in event logs. | NTP config (`chrony` or `systemd-timesyncd`); timestamp validation. |
| **A.5.19 - Remote access** | Secure remote access (e.g. admin console). | Admin console over TLS; VPN or IP allowlist; no public admin without MFA. | VPN config or security group rule; admin console URL restricted. |

#### B. ISO 27001 Implementation Checklist (for Keycloak)

| # | Control | Implementation step | Owner | Evidence |
|---|---------|---------------------|--------|----------|
| 1 | **A.5.15 - Access control** | Document access control policy; configure @PreAuthorize; test unauthorized = denied. | Backend/Security | Policy doc; SecurityConfig; test result. |
| 2 | **A.5.16 - Identity mgmt** | KeycloakUtil user CRUD; unique IDs; admin events logged. | Backend | KeycloakUtil code; user list; admin event log. |
| 3 | **A.5.17 - Auth info** | Password hashing (Argon2); TLS; no plain-text in logs. | DevOps | Hashing config; TLS cert; log sample (no passwords). |
| 4 | **A.5.18 - Access review** | Quarterly access review process; document; execute; sign off. | Security/HR | Access review SOP; quarterly review logs (3+ samples for Type II). |
| 5 | **A.8.2 - Privileged access** | MFA for admins; IP restriction; admin events logged. | Security/DevOps | MFA config; IP rule; admin event sample. |
| 6 | **A.8.3 - Info restriction** | Tenant isolation via groups and TenantFilter; test cross-org = denied. | Backend | TenantFilter code; test: user A cannot access org B data. |
| 7 | **A.8.5 - Secure auth** | MFA optional now; can be enforced per role. | Security | MFA config; optional or conditional flow. |
| 8 | **A.8.10 - Data deletion** | User deletion procedure (Keycloak + Vima DB); retention period documented. | Backend/Legal | Deletion SOP; GDPR policy (if applicable); deletion log. |
| 9 | **A.8.12 - Data leakage** | JWT minimal claims; token expiry; revocation on logout. | Backend | JWT sample; token lifespan config; revocation test. |
| 10 | **A.8.13 - Backup** | Keycloak DB backup (RDS); realm export; restore test. | DevOps | RDS backup schedule; realm export JSON; restore test log. |
| 11 | **A.12.4.1-4 - Logging** | Event and admin event logging; S3 protected; NTP sync. | DevOps/Backend | Event log sample; S3 IAM policy; NTP config. |
| 12 | **A.5.19 - Remote access** | Admin console VPN/IP restricted. | DevOps/Network | VPN config or security group rule; test: public IP blocked. |

#### C. ISO 27001 Audit Evidence Package

| Document/artifact | Purpose | Annex A controls supported |
|-------------------|---------|----------------------------|
| **ISMS policy with Keycloak section** | Describe Keycloak's role in ISMS. | A.5.x (organizational) |
| **Access control policy** | Roles, approval, review, MFA. | A.5.15, A.5.18, A.8.2 |
| **Authentication policy** | Password, MFA, credential protection. | A.5.17, A.8.5 |
| **Logging policy** | What's logged, retention, protection, review. | A.12.4.1–4 |
| **User lifecycle procedure** | Onboarding, access review, offboarding. | A.5.16, A.5.18, A.8.2 |
| **Statement of Applicability (SoA)** | Controls implemented/excluded; Keycloak supports A.5.15–19, A.8.x, A.12.4. | All |
| **Risk register** | Risks: credential theft, brute force, data breach; mitigation via Keycloak (MFA, brute force protection, logging). | A.5.7 (risk assessment) |
| **Keycloak configuration baseline** | Realm export JSON; password policy; role structure; client settings; protocol mappers. | A.8.9 (configuration management) |
| **Access review evidence** | Quarterly reviews (3–4 samples over audit period). | A.5.18 |
| **Change log for Keycloak** | Upgrades, config changes, approvals. | A.8.32 (change management) |
| **Audit logs from S3** | 6–12 months of Keycloak events and admin events. | A.12.4.x |
| **Incident response test** | Scenario: compromised user; response: disable in Keycloak, investigate logs. | A.5.24–27 (incident mgmt) |

---

### 4. Cross-Framework Control Summary

Some controls are common across IRDAI, SOC 2, and ISO 27001; implement once and use for multiple audits.

| Control (common) | IRDAI | SOC 2 | ISO 27001 | Implementation |
|------------------|-------|-------|-----------|----------------|
| **Strong password policy** | AC-1 | CC6.3 | A.5.17 | Realm password policy: 12 char, complexity, history 5. |
| **MFA for admins** | AC-4 | CC6.7 | A.8.2, A.8.5 | Optional MFA (TOTP); enforce for SUPER_ADMIN, VIMA_ADMIN. |
| **Role-based access** | AC-2 | CC6.1, CC6.7 | A.5.15, A.8.2 | Realm roles; @PreAuthorize; role→permission mapping. |
| **Quarterly access review** | AC-3 | CC6.6, CC6.7 | A.5.18 | Script + manager approval; disable inactive; log. |
| **Audit logging** | AU-1 | CC7.2, CC7.3 | A.12.4.1–4 | Keycloak events → S3; retention 1 year; quarterly review. |
| **TLS encryption** | SC-1 | CC6.7 | A.8.24 | Nginx TLS 1.2+; no plain HTTP for auth. |
| **Admin access restriction** | AC-4 | CC6.7 | A.8.2 | IP allowlist or VPN for admin console. |
| **User offboarding** | AC-3 | CC6.6 | A.5.18 | Disable user in Keycloak; document process. |

**Benefit:** Single implementation (e.g. realm password policy) satisfies multiple frameworks; reduces audit prep overhead.

---

### 5. Audit Preparation Timeline

| Week | Activity | Deliverable | Framework |
|------|----------|-------------|-----------|
| **Week 1** | Document access control and password policies referencing Keycloak. | Policy docs (access, password, logging). | All |
| **Week 2** | Configure Keycloak per checklists (password, MFA, logging, IP restriction). | Keycloak realm config complete; event listener → S3 deployed. | All |
| **Week 3** | Implement quarterly access review process and document. | Access review SOP; sample review (test run). | All |
| **Week 4–6** | Deploy Keycloak, migrate users, and begin logging (start of SOC 2 Type II observation). | Keycloak production; event logs flowing to S3. | SOC 2 Type II |
| **Ongoing** | Collect evidence: quarterly access reviews, monthly log reviews, admin actions, onboarding/offboarding. | Logs in S3; review reports; approval tickets. | SOC 2 Type II, ISO 27001 |
| **Pre-audit** | Prepare evidence package (see below); run internal audit; fix gaps. | Evidence folders; internal audit report. | All |

---

### 6. Audit Evidence Package (Deliverables for Auditor)

When auditors request evidence, provide the following for Keycloak:

| Evidence item | File/data | Frameworks |
|---------------|-----------|------------|
| **System description** | Keycloak architecture (realms, clients, flows); integration with Vima backend. | All |
| **Access control policy** | Roles, approval, review, MFA, offboarding. | All |
| **Password policy doc** | 12 char, complexity, history; enforced in Keycloak. | All |
| **Keycloak realm config** | Realm export JSON; password policy screenshot; role list. | All |
| **User access matrix** | CSV: username, email, role(s), org(s), created date, last login. | All |
| **Quarterly access reviews** | 3+ reviews with manager approvals and disabled user actions (for SOC 2 Type II). | SOC 2, ISO 27001 |
| **Event logs (S3)** | Sample logs: login, logout, failed login, password change; show timestamp, user, action, IP. | All |
| **Admin event logs** | User created, role assigned, config changed; show who, when, what. | All |
| **MFA enrollment report** | Admin users with MFA enabled; percentage; enforcement config. | All |
| **TLS certificate** | SSL/TLS cert for api.vimainsurance.com; SSL Labs report. | All |
| **Firewall/IP restriction** | Security group rule or VPN config for admin console. | All |
| **Service account config** | vima-backend client roles (manage-users, manage-groups only); no realm-admin unless justified. | SOC 2, ISO 27001 |
| **Offboarding evidence** | 5+ samples: employee leaves, ticket created, Keycloak user disabled, org access removed. | All |
| **Incident response test** | Scenario: compromised user; response: disable user, investigate logs, notify; evidence: ticket, logs, resolution. | SOC 2, ISO 27001 |
| **Change log** | Keycloak version upgrades, config changes, approvals. | ISO 27001 A.8.32 |
| **Backup and restore test** | RDS backup schedule; realm export; restore test (date, result). | ISO 27001 A.8.13 |

---

### 7. Continuous Compliance Monitoring

**After Keycloak go-live:**

| Task | Frequency | Owner | Tool/method |
|------|-----------|--------|-------------|
| **Access review** | Quarterly | Security/HR | Keycloak Admin API script → CSV; manager approval; disable inactive. |
| **Log review** | Monthly or quarterly | Security | Query S3 logs for anomalies (e.g. failed logins, unusual IP, role changes); investigate. |
| **Security patching** | Quarterly (or as CVE announced) | DevOps | Check Keycloak advisories; test upgrade in dev; deploy to prod. |
| **MFA enrollment check** | Quarterly | Security | List admin users; verify all have MFA enabled; enforce if not. |
| **User offboarding audit** | On each separation | HR/Security | Checklist: Keycloak user disabled within 24 hours; org access removed; verify in next access review. |
| **Event log retention** | Continuous | DevOps | S3 lifecycle policy; verify 1-year retention; no premature deletion. |
| **Backup validation** | Monthly | DevOps | RDS snapshot exists; realm export generated; test restore (quarterly). |
| **Policy review** | Annually (or on major change) | Security/Legal | Review and update access, password, logging policies for accuracy. |

---

### 8. Pre-Audit Internal Checklist

**Before engaging auditor (SOC 2 or ISO 27001), verify:**

- [ ] All policies reference Keycloak (access, password, logging, incident).
- [ ] Keycloak configured per checklists (password policy, MFA, logging, IP restriction).
- [ ] At least 3 quarterly access reviews completed (for SOC 2 Type II) with approvals and actions.
- [ ] Event logs in S3 for the entire audit period (e.g. 6-12 months); no gaps.
- [ ] Admin events logged and available (user creation, role assignment, config changes).
- [ ] TLS certificate valid; SSL Labs grade A or A+.
- [ ] Offboarding evidence (5+ samples) with Keycloak user disabled.
- [ ] Incident response plan references Keycloak logs; at least one test or real incident with resolution documented.
- [ ] Backup/restore tested; evidence of successful restore.
- [ ] Service account roles documented and least-privilege.
- [ ] Keycloak version documented; no unpatched high-severity CVEs.
- [ ] Internal audit completed; gaps identified and remediated or accepted with risk sign-off.

**Run a mock audit:** Have an internal team (or third-party consultant) request evidence and verify completeness before the formal audit.

---

### 9. Compliance Roadmap (Post–Migration)

After Keycloak is migrated and operational:

| Quarter | Compliance activity |
|---------|---------------------|
| **Q1** | First quarterly access review; refine process; collect logs for SOC 2 Type II (if pursuing). |
| **Q2** | Second quarterly review; log review; policy updates if needed; internal audit. |
| **Q3** | Third quarterly review; pre-audit prep (collect evidence, gap analysis); engage auditor if ready. |
| **Q4** | SOC 2 or ISO 27001 audit (if scheduled); address findings; obtain report. |

**IRDAI:** Ongoing; ISNP compliance is part of regular insurance audits (IRDAI inspection or internal audit). Evidence prepared for each inspection cycle.

---

### 10. Key Policies and Procedures to Document

**Must have (for any audit):**

1. **Access Control Policy** (covers Keycloak roles, approval, review, MFA, offboarding) – owned by Security.
2. **Password Policy** (12 char, complexity, history, enforced by Keycloak) – owned by Security.
3. **Logging and Monitoring Policy** (events logged, retention, review, alerting) – owned by Security/Ops.
4. **User Lifecycle Procedure** (onboarding, access review, offboarding) – owned by HR/Security.
5. **Incident Response Plan** (how to respond to auth-related incidents; use Keycloak logs) – owned by Security/Ops.
6. **Change Management Policy** (Keycloak upgrades, config changes, testing, approval) – owned by DevOps/Security.
7. **Backup and Recovery Procedure** (Keycloak DB backup, realm export, restore testing) – owned by DevOps.

Each policy must **reference Keycloak** where applicable (e.g. "Password policy enforced by Keycloak realm settings; minimum 12 characters...").

---

### Summary: Audit-Ready Implementation

- **IRDAI (ISNP):** 12 implementation steps; 7 evidence items; focus on access control, audit logging, TLS, MFA for admins.
- **SOC 2 Type I & II:** 9 controls (CC6.x, CC7.x, A1.x); narrative and policies; continuous evidence collection over 6-12 months for Type II.
- **ISO 27001:** 14+ Annex A controls; evidence package; internal checklist; pre-audit mock.
- **Cross-framework:** 8 common controls implemented once; satisfy multiple audits.
- **Continuous compliance:** Quarterly access reviews, monthly log reviews, security patching, MFA checks, offboarding audits.
- **Policies needed:** 7 key policies/procedures referencing Keycloak.

Use this plan as a roadmap to configure Keycloak in an audit-ready manner and to prepare evidence for IRDAI, SOC 2, and ISO 27001 audits.

---

## Data Migration Strategy

### 1. User Migration

#### Step 1: Export Users from Authentik

**Option A: Via Authentik Admin API**
```java
// Use existing AuthentikUtil.getAllUsers()
List<AdminUserResponseDto> authentikUsers = authentikUtil.getAllUsers();
```

**Option B: Export from Database**
```sql
-- If Authentik uses PostgreSQL, export from Authentik DB
SELECT uid, username, email, name, is_active, date_joined, last_login
FROM authentik_core_user;
```

#### Step 2: Import Users to Keycloak

**Script: KeycloakUserMigration.java**

```java
@Service
public class KeycloakUserMigration {
    
    @Autowired
    private AuthentikUtil authentikUtil;
    
    @Autowired
    private KeycloakUtil keycloakUtil;
    
    @Autowired
    private IAdminUserRepository adminUserRepository;
    
    public void migrateUsers() {
        List<AdminUserResponseDto> authentikUsers = authentikUtil.getAllUsers();
        
        for (AdminUserResponseDto authentikUser : authentikUsers) {
            try {
                // Create user in Keycloak
                String tempPassword = PasswordGenerator.generateRandomPassword(12);
                String keycloakUserId = keycloakUtil.createUser(
                    authentikUser.getFullName(),
                    authentikUser.getUsername(),
                    authentikUser.getEmail(),
                    authentikUser.getRoles().get(0), // Primary role
                    authentikUser.getOrganizations(),
                    authentikUser.getIsActive(),
                    tempPassword,
                    authentikUser.getOauthProviderId() // user_id attribute
                );
                
                // Update admin_users table with Keycloak user ID
                Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(authentikUser.getUsername());
                if (adminUserOpt.isPresent()) {
                    AdminUser adminUser = adminUserOpt.get();
                    adminUser.setOauthProviderId(keycloakUserId);
                    adminUser.setOauthProvider("keycloak");
                    adminUserRepository.save(adminUser);
                }
                
                // Send password reset email via Keycloak
                keycloakUtil.sendPasswordResetEmail(keycloakUserId);
                
                logger.info("Migrated user: {}", authentikUser.getUsername());
            } catch (Exception e) {
                logger.error("Failed to migrate user: {}", authentikUser.getUsername(), e);
            }
        }
    }
}
```

**Considerations:**
- **Password Migration:** Passwords cannot be migrated (hashed differently). Users must reset passwords.
- **Temporary Passwords:** Generate temporary passwords and force password change on first login.
- **Email Verification:** Users can verify email via Keycloak reset link.
- **Attributes:** Migrate custom attributes (`user_id`, `company_id`) to Keycloak user attributes.

#### Step 3: Migrate Groups and Roles

**Roles:** Create realm roles manually (one-time setup)

**Groups:**
```java
public void migrateGroups() {
    // Get all ORG_* groups from Authentik
    List<OrganizationDto> authentikOrgs = authentikUtil.getOrganizations();
    
    for (OrganizationDto org : authentikOrgs) {
        keycloakUtil.createGroup(org.getName(), org.getId());
    }
}
```

### 2. Database Schema Updates

**Update admin_users table:**
```sql
-- Add column for Keycloak migration tracking
ALTER TABLE admin.admin_users ADD COLUMN IF NOT EXISTS keycloak_migrated BOOLEAN DEFAULT FALSE;
ALTER TABLE admin.admin_users ADD COLUMN IF NOT EXISTS keycloak_user_id VARCHAR(255);

-- Update oauth_provider field
UPDATE admin.admin_users SET oauth_provider = 'keycloak' WHERE keycloak_migrated = TRUE;
```

### 3. Dual-Mode Migration

**Strategy:** Run both Authentik and Keycloak in parallel for a transition period.

**Steps:**
1. Keep Authentik running
2. Deploy Keycloak
3. Migrate users to Keycloak
4. Backend supports both providers via `auth.provider` flag
5. Test Keycloak integration thoroughly
6. Switch `auth.provider=keycloak` in production
7. Monitor for 1-2 weeks
8. Decommission Authentik

---

## Migration Users Plan to Keycloak

This section provides a step-by-step plan to migrate users from Authentik to Keycloak, including roles, groups, and the Vima `admin_users` table.

### Prerequisites

Before migrating users, ensure:

| # | Prerequisite | Owner | Verification |
|---|--------------|--------|--------------|
| 1 | Keycloak server is running and reachable | DevOps | `GET {keycloak-url}/health` returns 200 |
| 2 | Target realm exists (`vima-dev` or `vima-prod`) | DevOps | Realm visible in Admin Console |
| 3 | All realm roles exist (SUPER_ADMIN, VIMA_ADMIN, HR_ADMIN, etc.) | DevOps | Roles list matches `UserRole` enum |
| 4 | All ORG_* groups exist in Keycloak (or migration script creates them) | Backend/DevOps | Groups match Authentik group names |
| 5 | Keycloak Admin API credentials (client secret or admin user) | DevOps | Backend can obtain access token |
| 6 | Vima backend has KeycloakUtil (or equivalent) implemented | Backend | Unit tests pass |
| 7 | Database migration applied (e.g. `keycloak_user_id`, `oauth_provider` columns) | Backend | Flyway migration applied |
| 8 | Authentik API is still available for read-only export | DevOps | `AuthentikUtil.getUsers()` works |

### Migration Order (What First)

Migrate in this order to avoid foreign references and missing roles/groups:

1. **Realms and realm roles** – Create realm, then create all realm roles.
2. **Groups** – Create all ORG_* groups in Keycloak (names can match Authentik; IDs will differ).
3. **Users** – Create users, then assign realm roles and group memberships.
4. **Vima `admin_users`** – Update `oauth_provider_id` and `oauth_provider` after each user is created in Keycloak.

### Phase 1: Migrate Groups to Keycloak

Groups in Authentik (ORG_*) must exist in Keycloak before users can be assigned.

**Input:** List of organizations from Authentik (`AuthentikUtil.getOrganizations()`).

**Steps:**

1. Call Authentik API to get all groups whose names start with `ORG_`.
2. For each group:
   - Create group in Keycloak with the **same name** (e.g. `ORG_MAIN`, `ORG_ACME`).
   - Keycloak will assign a new UUID; no need to preserve Authentik’s group ID in Keycloak.
3. Optionally maintain a mapping file: `authentik_group_id -> keycloak_group_id` if needed for auditing; for Vima, matching by **name** is enough (same name in different realm = different group, as per Keycloak behavior).

**Idempotency:** Before creating a group, check if a group with that name already exists in the realm; skip or update as needed.

**Script outline:**

```java
public void migrateGroupsToKeycloak(String realm) {
    List<OrganizationDto> authentikOrgs = authentikUtil.getOrganizations();
    for (OrganizationDto org : authentikOrgs) {
        if (keycloakUtil.groupExistsByName(realm, org.getName())) {
            logger.info("Group already exists: {}", org.getName());
            continue;
        }
        keycloakUtil.createGroup(realm, org.getName());
    }
}
```

Run this once per realm (`vima-dev`, then `vima-prod`).

---

### Phase 2: Export Users from Authentik

**Goal:** Get a complete, paginated list of users from Authentik without overloading the API.

**Method:** Use existing `AuthentikUtil.getUsers(page, pageSize)` or `getUsersWithFilters(...)` and page until no more results.

**Steps:**

1. Set page size (e.g. 50–100).
2. Page through all users:
   - Call Authentik API for each page.
   - Collect into a list (or process in batches in Phase 3/4).
3. Handle errors:
   - Retry with backoff on 5xx or rate limit.
   - Log and skip or abort on 4xx (e.g. auth failure).
4. Optional: Export to a JSON/CSV file as a snapshot for audit or replay.

**Data to capture per user (from Authentik / AdminUserResponseDto):**

- `username`
- `email`
- `name` (full name)
- `is_active`
- `groups_obj` → roles (ROLE_*) and organizations (ORG_*)
- `uid` (Authentik user ID – for mapping to `admin_users.oauth_provider_id` before migration)
- `last_login`, `date_joined` (optional, for reporting)

**Duplicate handling:** Authentik usernames/emails are unique; Keycloak will enforce the same. If re-running migration, use “create if not exists” or check by username before creating.

---

### Phase 3: Transform and Validate User Data

Before sending users to Keycloak, normalize and validate.

**Transform:**

- **Roles:** From Authentik `groups_obj`, take entries starting with `ROLE_` (e.g. `ROLE_VIMA_ADMIN` → realm role `VIMA_ADMIN` in Keycloak). If your backend expects `ROLE_` prefix in JWT, Keycloak mappers can add it.
- **Organizations:** From `groups_obj`, take entries starting with `ORG_`. These must match group names created in Phase 1.
- **Custom attributes:** Map `user_id` (Vima `admin_users.id`) if you have it; set as Keycloak user attribute for JWT.

**Validation:**

- Required: `username`, `email`, `name`.
- Email format valid.
- At least one role (realm role) per user.
- All referenced ORG_* groups exist in Keycloak (from Phase 1).
- Optional: flag users with no role or no org for manual review.

**Output:** A list of “migration records” (e.g. DTOs) ready for Keycloak create + role/group assignment.

---

### Phase 4: Import Users into Keycloak

**Goal:** Create each user in Keycloak with profile and attributes, then assign roles and groups.

**Steps per user:**

1. **Create user** via Keycloak Admin API:
   - `username`, `email`, `firstName`, `lastName` (split from `name` or put all in firstName).
   - `enabled` = `is_active`.
   - Set custom attributes: e.g. `user_id` (Vima admin user UUID), `company_id` if used.
2. **Set temporary password** (required for first login):
   - Set password via Admin API.
   - Set required action `UPDATE_PASSWORD` so user must change on first login (recommended).
3. **Assign realm roles:** Add each realm role (e.g. `VIMA_ADMIN`) to the user.
4. **Assign groups:** Add user to each ORG_* group by Keycloak group name (or ID if you stored mapping).

**Idempotency:**

- Check if user already exists (e.g. by `username` in realm). If exists, optionally update attributes and role/group membership only (no duplicate user).
- Use a migration log table or file: `authentik_user_id | keycloak_user_id | status (created/updated/skipped/failed)`.

**Error handling:**

- **Duplicate username/email:** Skip or update (depending on strategy).
- **Keycloak 4xx (e.g. invalid data):** Log, mark as failed, continue with next user.
- **5xx / network:** Retry with backoff; after N failures, stop and alert.

**Batch size:** Process in batches (e.g. 50–100 users) with a short pause between batches to avoid overloading Keycloak.

**Example flow:**

```java
for (AuthentikUserRecord record : userRecords) {
    try {
        String keycloakId = keycloakUtil.createUser(realm, record);
        keycloakUtil.setTemporaryPassword(realm, keycloakId, tempPassword);
        keycloakUtil.addRequiredAction(realm, keycloakId, "UPDATE_PASSWORD");
        keycloakUtil.assignRealmRoles(realm, keycloakId, record.getRoles());
        keycloakUtil.assignGroups(realm, keycloakId, record.getOrganizationGroups());
        updateVimaAdminUser(record.getAuthentikUid(), keycloakId);
        migrationLog.success(record.getUsername(), keycloakId);
    } catch (Exception e) {
        migrationLog.failed(record.getUsername(), e);
    }
}
```

---

### Phase 5: Update Vima `admin_users` Table

**Goal:** Link each Vima admin user to their Keycloak identity so the backend can resolve users by JWT and optionally by `oauth_provider_id`.

**Steps:**

1. For each successfully created Keycloak user, find the corresponding row in `admin.admin_users` (e.g. by `username` or by current `oauth_provider_id` = Authentik user ID).
2. Update:
   - `oauth_provider_id` = Keycloak user ID (UUID).
   - `oauth_provider` = `'keycloak'`.
   - Optionally: `keycloak_migrated` = `true`, `keycloak_user_id` = Keycloak user ID (if you added these columns).
3. Do not change `id`, `username`, `email`, or other business fields unless you have a separate data fix.

**SQL example (per user, or batch):**

```sql
UPDATE admin.admin_users
SET oauth_provider_id = :keycloakUserId,
    oauth_provider = 'keycloak',
    keycloak_migrated = true,
    updated_at = NOW()
WHERE username = :username;
-- Or: WHERE oauth_provider_id = :authentikUserId;
```

Run this only for users that were successfully created in Keycloak (use migration log).

---

### Phase 6: Passwords and First-Login Experience

**Passwords:** Authentik and Keycloak use different hashing; passwords cannot be copied. Two common approaches:

- **Option A (recommended):** Set a random temporary password in Keycloak and set required action `UPDATE_PASSWORD`. User receives “password reset” or “welcome” email (from Keycloak or your app) and sets a new password on first login.
- **Option B:** Do not set a password; send Keycloak “reset password” link to each user so they set their own password before first login.

**Emails:**

- Configure Keycloak SMTP (Realm → Email) so that “Forgot password” and “Verify email” (if used) are sent by Keycloak.
- If you use custom emails (e.g. “Welcome to Vima, please set your password”), send them from your backend using existing templates and link to Keycloak’s reset or login page.

**Required actions (optional but recommended):**

- `UPDATE_PASSWORD` – Force password change on first login.
- `VERIFY_EMAIL` – If you want verified emails in Keycloak.

---

### Phase 7: Per-Realm Execution (Dev vs Prod)

- **vima-dev:** Run full migration (groups → export users → transform → import → update `admin_users`). Use dev Authentik and dev Keycloak realm. Validate login and JWT in dev.
- **vima-prod:** Repeat the same steps for production:
  - Export users from production Authentik (or prod snapshot).
  - Create groups in `vima-prod` realm.
  - Import users into `vima-prod`.
  - Update production `admin_users` only after each user is successfully created in Keycloak.

Do not mix dev and prod: use separate Authentik sources and separate Keycloak realms.

---

### Phase 8: Verification and Rollback

**Verification:**

- Count users in Authentik vs Keycloak (per realm); investigate gaps.
- Spot-check: log in as a few users (each role and org) and confirm JWT contains expected `realm_access.roles` and `groups`.
- Run backend integration tests that rely on JWT (roles, orgs, tenant isolation).
- Confirm `admin_users.oauth_provider_id` and `oauth_provider` are set correctly for migrated users.

**Rollback (if migration is reverted):**

- Backend switches back to Authentik (`auth.provider=authentik`, issuer-uri points to Authentik).
- Restore `admin_users`: set `oauth_provider_id` back to Authentik user ID and `oauth_provider` back to `authentik` for migrated users (keep a pre-migration backup or use the migration log).
- Keycloak users can remain; they are simply not used until you switch back to Keycloak.

---

### Summary: Migration Users Checklist

| Phase | Action | Owner |
|-------|--------|--------|
| 1 | Create all ORG_* groups in Keycloak realm | Backend/DevOps |
| 2 | Export all users from Authentik (paginated) | Backend |
| 3 | Transform and validate (roles, groups, attributes) | Backend |
| 4 | Import users to Keycloak (create + roles + groups + temp password) | Backend |
| 5 | Update `admin_users.oauth_provider_id` and `oauth_provider` | Backend |
| 6 | Configure password reset / welcome emails and required actions | DevOps/Backend |
| 7 | Run for vima-dev, then vima-prod | Backend/DevOps |
| 8 | Verify counts, JWT, and rollback plan | QA/Backend |

---

## Testing Strategy

### 1. Unit Tests

**Test Coverage:**
- KeycloakUtil methods (mock Keycloak Admin Client)
- JwtAuthenticationConverter with Keycloak JWT structure
- JwtUserExtractor with Keycloak claims

**Example Test:**
```java
@Test
public void testKeycloakJwtExtraction() {
    // Mock JWT with Keycloak claims
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .claim("sub", "user-uuid")
        .claim("email", "test@vimainsurance.com")
        .claim("preferred_username", "testuser")
        .claim("realm_access", Map.of("roles", List.of("VIMA_ADMIN")))
        .claim("groups", List.of("ORG_MAIN"))
        .build();
    
    JwtUserExtractor extractor = new JwtUserExtractor();
    String email = extractor.getEmail(jwt);
    List<String> roles = extractor.getRoles(jwt);
    
    assertEquals("test@vimainsurance.com", email);
    assertTrue(roles.contains("VIMA_ADMIN"));
}
```

### 2. Integration Tests

**Test Scenarios:**
1. **JWT Validation:** Verify Keycloak-issued JWT is validated correctly
2. **User Creation:** Create user via KeycloakUtil, verify in Keycloak
3. **Role Assignment:** Assign roles, verify in JWT claims
4. **Group Membership:** Add user to ORG_* group, verify in JWT
5. **Password Reset:** Trigger password reset, verify email sent
6. **Token Refresh:** Test access token refresh flow

**Environment:**
- Spin up Keycloak testcontainer
- Configure test realm
- Run integration tests against test Keycloak instance

### 3. E2E Tests

**Test Flows:**
1. Login → Get JWT → Call protected API → Verify authorization
2. Admin creates user → User receives email → User resets password → User logs in
3. HR Admin creates user with ORG_X group → User logs in → Verify tenant isolation
4. Vima Admin assigns role → Verify role appears in JWT → Verify access control

**Tools:**
- Postman/Newman for API testing
- Selenium/Playwright for frontend login flow testing

### 4. Load Testing

**Scenarios:**
- 100 concurrent logins
- 1000 JWT validations/second
- User creation at scale (100 users/minute)

**Tools:**
- JMeter or Gatling
- Monitor Keycloak performance metrics

---

## Rollout Plan

### Phase 1: Pre-Migration (Week 1-2)

**Tasks:**
- [ ] Set up Keycloak server (dev environment)
- [ ] Create `vima-dev` realm
- [ ] Configure clients, roles, groups
- [ ] Create custom email and login themes
- [ ] Deploy themes to Keycloak
- [ ] Test Keycloak admin console access

**Deliverables:**
- Keycloak dev instance running
- Custom themes deployed
- Documentation for Keycloak setup

### Phase 2: Backend Development (Week 2-4)

**Tasks:**
- [ ] Implement KeycloakUtil.java
- [ ] Create/update DTOs
- [ ] Update SecurityConfig for dual provider support
- [ ] Implement UnifiedJwtAuthenticationConverter
- [ ] Update AdminUserServiceImpl for provider switching
- [ ] Add feature flag `auth.provider`
- [ ] Write unit tests for Keycloak integration

**Deliverables:**
- KeycloakUtil fully implemented
- Backend supports both Authentik and Keycloak
- Unit tests passing (>80% coverage)

### Phase 3: Data Migration (Week 4-5)

**Tasks:**
- [ ] Export users from Authentik
- [ ] Create user migration script
- [ ] Migrate users to Keycloak (dev realm)
- [ ] Verify user data in Keycloak
- [ ] Test login with migrated users
- [ ] Send password reset emails to all users

**Deliverables:**
- All dev users migrated to Keycloak
- Migration script documented and versioned

### Phase 4: UAT Testing (Week 5-6)

**Tasks:**
- [ ] Deploy backend with Keycloak integration to UAT
- [ ] Run integration tests
- [ ] Perform manual testing (login, user management, role assignment)
- [ ] Test email flows (password reset, welcome email)
- [ ] Verify JWT claims and authorization
- [ ] Load testing

**Deliverables:**
- UAT environment fully tested
- Bug fixes implemented
- Sign-off from QA team

### Phase 5: Production Migration (Week 7)

**Tasks:**
- [ ] Set up Keycloak production instance
- [ ] Create `vima-prod` realm
- [ ] Configure production clients, roles, groups
- [ ] Deploy custom themes
- [ ] Migrate production users (off-peak hours)
- [ ] Update backend configuration: `auth.provider=keycloak`
- [ ] Deploy backend to production
- [ ] Monitor logs and metrics

**Deliverables:**
- Production Keycloak instance live
- All users migrated
- Backend switched to Keycloak

### Phase 6: Post-Migration (Week 7-8)

**Tasks:**
- [ ] Monitor Keycloak performance and logs
- [ ] Address any user-reported issues
- [ ] Send user communication (password reset, new login URL if applicable)
- [ ] Monitor JWT validation success rate
- [ ] Verify no authentication errors
- [ ] Keep Authentik running as backup (do not decommission yet)

**Deliverables:**
- 7 days of stable operation
- Incident log (if any)
- User feedback collected

### Phase 7: Decommission Authentik (Week 9-10)

**Tasks:**
- [ ] Verify Keycloak fully operational for 2 weeks
- [ ] Remove `auth.provider` flag (hardcode Keycloak)
- [ ] Remove AuthentikUtil and related code
- [ ] Update documentation
- [ ] Decommission Authentik server
- [ ] Remove Authentik configuration from properties files

**Deliverables:**
- Authentik fully removed
- Codebase cleaned up
- Documentation updated

---

## Rollback Strategy

### Rollback Trigger Conditions

- Critical authentication failures (>5% login failure rate)
- JWT validation errors (>1% of requests)
- Keycloak server downtime (>5 minutes)
- Data loss or corruption
- Unable to create users via Keycloak API

### Rollback Procedure

**Step 1: Switch Auth Provider**
```properties
# Revert application-prod.properties
auth.provider=authentik
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://api.vimainsurance.com/application/o/vima/
```

**Step 2: Redeploy Backend**
- Deploy previous version with Authentik integration
- Or hot-reload configuration if using Spring Cloud Config

**Step 3: Verify Authentik Operational**
- Test login with Authentik
- Verify JWT validation working
- Monitor logs for errors

**Step 4: Notify Users**
- If login URL changed, notify users of rollback
- No action needed if backend URL unchanged

**Step 5: Investigate Keycloak Issues**
- Review Keycloak logs
- Check database connections
- Verify configuration

**Rollback Time:** < 15 minutes (configuration change + redeploy)

---

## Open Questions - ANSWERED

### Critical Questions (Need Answers Before Migration)

1. **Keycloak Hosting:** ✅ **ANSWERED**
   - [x] **Where:** AWS EC2 t3.medium with 4GB RAM
   - [x] **URL:** `api.vimainsurance.com` (⚠️ **NEEDS CLARIFICATION** - see Follow-up Questions below)
   - [x] **High Availability:** Not required now, but plan for future scalability

2. **User Communication:** ✅ **ANSWERED**
   - [x] **Notification method:** Email and phone
   - [x] **Login URL change:** No change
   - [x] **Password reset strategy:** ⚠️ **NEEDS DECISION** - see recommendations below

3. **Password Migration:** ✅ **ANSWERED**
   - [x] **Strategy:** Force password reset on first login
   - [x] **Password hash migration:** Not implemented (different hashing between Authentik and Keycloak)
   - [x] **Handling:** Users prompted to reset on login; temporary password with required action `UPDATE_PASSWORD`

4. **Migration Downtime:** ✅ **ANSWERED**
   - [x] **Downtime tolerance:** 1-2 hours acceptable
   - [x] **Preferred window:** Weekend migration
   - [x] **Approach:** Direct migration with downtime (simpler than dual-mode)

5. **Keycloak Version:** ✅ **ANSWERED**
   - [x] **Version:** Latest stable without known CVEs (26.0.0 or latest patch at migration time)
   - [x] **Deployment:** Self-hosted
   - [x] **Action:** Check CVE database before deployment

6. **Multi-Realm Strategy:** ✅ **CONFIRMED**
   - [x] **Architecture:** Two separate realms (`vima-dev`, `vima-prod`)
   - [x] **Isolation:** Complete separation between dev and production

7. **Email Sending:** ✅ **ANSWERED**
   - [x] **Keycloak SMTP:** Yes, configure SMTP in Keycloak for auth-related emails
   - [x] **Templates:** Use Keycloak customizable FreeMarker templates
   - [x] **Email types:** Password reset, email verification, welcome emails sent by Keycloak
   - [x] **Custom business emails:** Continue using backend Spring Mail (e.g., enrollment invitations, quotes)

8. **Custom Attributes Mapping:** ✅ **CONFIRMED - ALL REQUIRED**
   - [x] `user_id` - Yes (from `admin.admin_users.id`) → JWT claim
   - [x] `company_id` - Yes → JWT claim
   - [x] `agent_id` - Yes → JWT claim
   - [x] `organization_ids` - Yes (derived from ORG_* groups) → JWT claim

9. **MFA/2FA:** ✅ **ANSWERED**
   - [x] **Current:** Not enabled
   - [x] **Future:** Plan for future MFA support (Keycloak supports TOTP, SMS, Email OTP)
   - [x] **Recommendation:** Configure but keep optional; can be enforced later per role

10. **Audit Logging:** ✅ **ANSWERED**
    - [x] **Integration:** Yes, integrate Keycloak audit logs with Vima backend audit system
    - [x] **Storage:** AWS S3
    - [x] **Implementation:** Keycloak Event Listener SPI → S3 (via Lambda or direct SDK)

### Technical Clarifications

11. **JWT Signing Algorithm:** ✅ **ANSWERED - INDUSTRY STANDARD**
    - [x] **Algorithm:** RS256 (RSA with SHA-256)
    - [x] **Rationale:** Industry standard, widely supported, Keycloak default
    - [x] **Alternative:** ES256 offers better performance but RS256 is proven and compatible

12. **Token Lifetime:** ✅ **CONFIRMED - INDUSTRY STANDARD**
    - [x] **Access token:** 15 minutes (900 seconds) ✓
    - [x] **Refresh token:** 7 days (604800 seconds) ✓
    - [x] **ID token:** 15 minutes (matches access token)
    - [x] **SSO Session Idle:** 30 minutes (industry standard)
    - [x] **SSO Session Max:** 10 hours (industry standard)

13. **Client Authentication:** ✅ **ANSWERED - INDUSTRY STANDARD**
    - [x] **Method:** Client secret with basic auth (standard for confidential clients)
    - [x] **Alternative:** JWT assertion is more secure but adds complexity; start with client secret

14. **User Federation:** ✅ **ANSWERED**
    - [x] **Current:** All users managed directly in Keycloak
    - [x] **Future:** Plan for LDAP/AD/Google Workspace integration (Keycloak supports)
    - [x] **Architecture:** Design with federation in mind (e.g., username as primary key, not email)

15. **Session Management:** ✅ **ANSWERED**
    - [x] **Sessions:** Keycloak maintains sessions (for SSO and refresh token revocation)
    - [x] **SSO:** Yes, enable single sign-on across multiple Vima applications
    - [x] **Token type:** Stateless JWT for API authorization + session for SSO state

16. **API Rate Limiting:** ✅ **ANSWERED**
    - [x] **Keycloak Admin API:** Yes, implement rate limiting
    - [x] **Method:** ⚠️ **NEEDS CLARIFICATION** - Keycloak built-in or continue with Bucket4j? See recommendations below

### UI/UX Questions

17. **Login Page Branding:** ✅ **ANSWERED**
    - [x] **Logo:** Yes, Vima logo required
    - [x] **Brand colors:** Use Vima brand palette (⚠️ **NEEDS SPECIFIC HEX CODES** - see Follow-up)
    - [x] **Design:** Customized Keycloak theme with Vima branding

18. **User Registration:** ✅ **ANSWERED**
    - [x] **Current:** Admin-only user creation
    - [x] **Future:** Self-registration planned
    - [x] **Email verification:** Yes, required when self-registration is enabled

19. **Password Policy:** ✅ **ANSWERED - INDUSTRY STANDARD**
    - [x] **Minimum length:** 12 characters (NIST recommendation; current is 10)
    - [x] **Complexity:** At least 3 of 4 (uppercase, lowercase, numbers, special chars)
    - [x] **Password expiration:** No expiration (NIST 2024 guidelines; forced expiry reduces security)
    - [x] **Password history:** Prevent reuse of last 5 passwords
    - [x] **Breach detection:** Optional - integrate with HaveIBeenPwned API

20. **Locale/Language:** ✅ **ANSWERED**
    - [x] **Current:** English (US/UK)
    - [x] **Future:** Multi-language support planned
    - [x] **Keycloak:** Configure with en-US, prepare for localization later

---

## Follow-up Questions & Clarifications Needed

Based on your answers, I need clarification on the following:

### 🔴 CRITICAL - Infrastructure

**1. Keycloak URL at `api.vimainsurance.com` - Conflict with Authentik**

Currently, Authentik is running at `api.vimainsurance.com`. You mentioned Keycloak will also be at `api.vimainsurance.com`.

**Options:**

**A. Replace Authentik at same URL (recommended for "no login URL change"):**
- Keycloak at: `https://api.vimainsurance.com` (same as Authentik now)
- Frontend login redirect: `https://api.vimainsurance.com/realms/vima-prod/protocol/openid-connect/auth`
- Backend issuer: `https://api.vimainsurance.com/realms/vima-prod`
- **Pros:** Login URL doesn't change (users don't notice), clean migration
- **Cons:** Authentik must be stopped before Keycloak starts (requires downtime window)

**B. Different subdomain:**
- Keycloak at: `https://keycloak.vimainsurance.com` or `https://auth.vimainsurance.com`
- **Pros:** Can run both in parallel during testing
- **Cons:** Login URL changes (contradicts your "no change" requirement)

**C. Different path on same domain:**
- Authentik: `https://api.vimainsurance.com/application/o/vima/` (current)
- Keycloak: `https://api.vimainsurance.com/auth/realms/vima-prod/` (new)
- **Pros:** Both can coexist temporarily
- **Cons:** Reverse proxy complexity, frontend URL changes

**❓ QUESTION:** Which option do you prefer? **Option A is recommended** given your "no login URL change" requirement.

**If Option A (replace at same URL):**
- Keycloak will be installed on the **same EC2 instance** or different one?
- Current Authentik port/setup? (Need to shut down Authentik before starting Keycloak on same domain)

---

### 🟡 IMPORTANT - Password Reset Strategy

You asked "which is better and fastest" for password migration. Here's the recommendation:

**Recommended Strategy: Temporary Password + Required Action**

1. During migration, create each user in Keycloak with:
   - Random temporary password (12+ chars, generated by backend)
   - Required action: `UPDATE_PASSWORD` (forces change on first login)
2. Send email (via Keycloak or backend):
   - **Option A:** Keycloak "Reset password" link (user sets own password via link)
   - **Option B:** Email with temporary password + "You must change your password on first login"
3. User logs in:
   - Enters username + temporary password (if Option B)
   - OR clicks reset link and sets password (if Option A)
   - Keycloak forces password change page
   - User sets new password
   - User is redirected to app

**Fastest for users:** **Option A (reset link)** - no need to type temp password, just click link and set new one.

**Fallback:** Users who don't reset within 7 days can use "Forgot password" on login page (Keycloak will send new reset link).

**❓ QUESTION:** Confirm **Option A (reset link email)** is acceptable?

---

### 🟡 IMPORTANT - Rate Limiting Strategy

You said "better performance" for rate limiting. Here's the comparison:

| Feature | Keycloak Built-in | Bucket4j (current) |
|---------|------------------|-------------------|
| **Location** | Keycloak server | Vima backend |
| **What it limits** | Login attempts, token requests (at Keycloak) | Admin API calls from backend to Keycloak |
| **Performance** | Native, lower overhead | Requires backend interceptor |
| **Granularity** | Per realm/client/user | Custom (per service, per user, etc.) |

**Recommendation:**
- **Use both:**
  1. **Keycloak built-in:** Protect login endpoint (brute-force protection: 5 failed logins = 30 min lockout)
  2. **Bucket4j in backend:** Rate limit Admin API calls from KeycloakUtil (e.g., 100 user creations/min)

**❓ QUESTION:** Confirm using **both** Keycloak rate limiting (for login) and Bucket4j (for Admin API)? Or just Keycloak's?

---

### 🟡 IMPORTANT - Branding Details

You mentioned "own palette" for brand colors. I need:

1. **Vima logo:**
   - File path or URL (e.g., S3 URL, or provide image file)
   - Format: PNG or SVG (SVG preferred for scaling)
   - Preferred size for login page (e.g., 200px wide)

2. **Brand colors (hex codes):**
   - Primary color: `#______` (currently using `#2563eb` blue in doc - correct?)
   - Secondary color: `#______`
   - Background color: `#______`
   - Text color: `#______`
   - Button colors (normal, hover, active): `#______`, `#______`, `#______`

3. **Font:**
   - Primary font family (e.g., "Inter", "Roboto", "Arial") or custom font URL

**❓ QUESTION:** Please provide:
- Logo file or URL
- Hex color codes for primary, secondary, background, text, buttons
- Font family name

---

### 🟢 CLARIFICATION - EC2 Setup

**EC2 Instance Details:**
- Instance type: t3.medium (2 vCPU, 4 GB RAM) ✓
- OS: **What Linux distribution?** (Amazon Linux 2023, Ubuntu 22.04, etc.)
- Keycloak deployment: **Docker or direct installation?** (Docker recommended)
- Database: **Where is Keycloak's database?**
  - Same RDS PostgreSQL instance as Vima backend? (different database: `keycloak`)
  - Separate RDS instance?
  - Embedded H2? (NOT recommended for production)

**Recommended Setup:**
```
┌─────────────────────────────────────────────────────────────┐
│  EC2 t3.medium (4GB RAM)                                    │
│  - Docker + Keycloak container (2GB allocated)              │
│  - Nginx reverse proxy (SSL termination)                    │
│  - Keycloak DB: PostgreSQL on existing RDS                  │
│    (separate database: keycloak)                            │
└─────────────────────────────────────────────────────────────┘
```

**Resource check for t3.medium (4GB RAM):**
- Keycloak: 2-2.5 GB heap
- System + Docker: 1 GB
- Nginx: 256 MB
- **Total:** ~3.5 GB (leaves 500 MB buffer) ✓ Should be sufficient for < 1000 active users

**For future HA:** Add a second t3.medium + load balancer (ALB).

**❓ QUESTION:**
- Confirm OS for EC2 (recommend Ubuntu 22.04 LTS or Amazon Linux 2023)
- Confirm Docker deployment (recommended)
- Confirm Keycloak DB location (recommend: existing RDS, new database `keycloak`)

---

### 🟢 CLARIFICATION - Audit Logs to S3

**Keycloak Event Listener → S3 Architecture:**

**Option A: Custom Event Listener SPI (recommended):**
1. Develop custom Keycloak Event Listener SPI (Java)
2. Listener captures events (login, logout, user created, password reset, etc.)
3. Writes events to S3 via AWS SDK (batched, every 1 min or 100 events)
4. Deploy SPI JAR to Keycloak `providers/` directory

**Option B: Event Listener → SQS → Lambda → S3:**
1. Keycloak Event Listener → AWS SQS
2. Lambda triggered by SQS
3. Lambda writes to S3 (batched)
4. More complex but decoupled

**❓ QUESTION:** Are you OK with **Option A (custom SPI)** or prefer **Option B (SQS + Lambda)**?
- Option A: Faster, simpler, 1-2 days dev
- Option B: More scalable, decoupled, 3-4 days dev

---

### 🟢 CLARIFICATION - Migration Timeline

Given your answers, here's the **revised timeline**:

| Phase | Duration | Tasks |
|-------|----------|-------|
| **Pre-migration** | Week 1-2 | Set up EC2, install Keycloak, configure realms/roles/groups, custom themes |
| **Backend dev** | Week 2-4 | Implement KeycloakUtil, update SecurityConfig, DTOs, unit tests |
| **Data migration** | Week 4-5 | Migrate groups, export/import users, update admin_users table |
| **UAT testing** | Week 5-6 | Integration tests, manual testing, load testing |
| **Prod migration** | Week 7 (weekend) | 2-hour downtime window: stop Authentik, start Keycloak, migrate prod users, switch backend |
| **Monitoring** | Week 7-8 | Monitor, address issues, keep Authentik backup for 2 weeks |
| **Cleanup** | Week 9-10 | Remove Authentik, remove dual-mode code, update docs |

**Total:** ~10 weeks (2.5 months)

**❓ QUESTION:** Is this timeline acceptable? Any hard deadlines or target go-live date?

---

### Summary of Decisions Needed

| # | Question | Priority |
|---|----------|----------|
| 1 | Keycloak URL strategy (Option A: replace at api.vimainsurance.com?) | 🔴 CRITICAL |
| 2 | Password reset: send reset link (Option A) or temp password (Option B)? | 🟡 Important |
| 3 | Rate limiting: both Keycloak + Bucket4j, or just Keycloak? | 🟡 Important |
| 4 | Logo file/URL and brand color hex codes | 🟡 Important |
| 5 | EC2 OS, Docker deployment, Keycloak DB location | 🟢 Clarification |
| 6 | Audit logs: Custom SPI (A) or SQS+Lambda (B)? | 🟢 Clarification |
| 7 | Timeline acceptable? Any hard deadlines? | 🟢 Clarification |

---

## Next Steps

### Immediate Actions (This Week)

1. ✅ ~~**Answer Open Questions**~~ - COMPLETED
2. 🔴 **URGENT: Answer Follow-up Questions** (see section above) - especially:
   - Keycloak URL strategy (critical for infrastructure setup)
   - Provide logo and brand colors for theme development
   - Confirm EC2 setup details (OS, Docker, database)
3. **Finalize Infrastructure Plan:**
   - Provision EC2 t3.medium instance
   - Set up RDS PostgreSQL database for Keycloak (if not using existing RDS)
   - Configure security groups, SSL certificates for `api.vimainsurance.com`

### Week 1-2: Setup & Configuration

4. **Deploy Keycloak (Dev Environment):**
   - Install Docker on EC2
   - Deploy Keycloak 26.0.0 (latest stable, CVE-checked)
   - Configure reverse proxy (Nginx) with SSL
   - Create `vima-dev` realm
   - Create clients: `vima-webapp`, `vima-backend`
   - Create realm roles and ORG_* groups
5. **Develop Custom Themes:**
   - Login theme with Vima branding (logo, colors, fonts)
   - Email templates (password reset, email verification, welcome)
6. **Configure Keycloak:**
   - SMTP settings for emails
   - Token lifespans (15 min access, 7 day refresh)
   - Password policy (12 char min, complexity, breach detection)
   - Rate limiting for login (brute force protection)

### Week 2-4: Backend Development

7. **Implement KeycloakUtil:**
   - Admin Client library integration
   - User management (create, update, delete, search, pagination)
   - Role and group assignment
   - Password reset functionality
   - Unit tests (>80% coverage)
8. **Update Spring Security Configuration:**
   - Update `issuer-uri` to point to Keycloak
   - Implement/update JWT authentication converter
   - Add feature flag `auth.provider` for dual-mode support (optional)
9. **Update DTOs and Mappers:**
   - Create KeycloakUserDto, KeycloakPaginatedResponse, etc.
   - Update AdminUserServiceImpl to use KeycloakUtil
10. **Implement Event Listener SPI (if Option A chosen):**
    - Custom Keycloak event listener for audit logs → S3

### Week 4-5: Data Migration

11. **Migrate Groups and Roles:**
    - Create all ORG_* groups in Keycloak dev realm
    - Verify realm roles match UserRole enum
12. **User Migration (Dev):**
    - Export users from Authentik dev
    - Transform data (roles, groups, attributes)
    - Import to Keycloak vima-dev realm
    - Update `admin_users.oauth_provider_id` with Keycloak user IDs
    - Send password reset emails to test users
13. **Verification:**
    - Test login with migrated users
    - Verify JWT claims (roles, groups, custom attributes)
    - Verify backend authorization and tenant isolation

### Week 5-6: UAT Testing

14. **Integration Testing:**
    - Run all backend integration tests against Keycloak
    - Test user creation, role assignment, password reset flows
    - Test token refresh, logout flows
15. **Load Testing:**
    - 100 concurrent logins
    - 1000 JWT validations/second
    - User creation at scale
16. **Manual Testing:**
    - End-to-end testing of all user flows
    - Cross-browser testing
    - Mobile responsive testing

### Week 7: Production Migration (Weekend)

17. **Pre-migration Checklist:**
    - Backup Authentik database and users
    - Backup Vima `admin_users` table
    - Deploy Keycloak production instance
    - Create `vima-prod` realm with same config as dev
    - Test Keycloak prod instance accessibility
18. **Migration Window (Saturday, 2 hours):**
    - **Hour 1:**
      - Stop Authentik service
      - Export all production users from Authentik
      - Migrate groups to Keycloak prod
      - Import users to Keycloak prod (batched)
      - Update `admin_users` table with Keycloak user IDs
    - **Hour 2:**
      - Deploy backend with Keycloak configuration
      - Smoke test: login, API calls, JWT validation
      - Send password reset emails to all users
      - Monitor logs for errors
19. **Post-Migration:**
    - Send user notification email (migration complete, reset password)
    - Monitor for 24 hours
    - Keep Authentik instance as backup (do not delete)

### Week 7-8: Monitoring & Stabilization

20. **Monitor:**
    - Keycloak performance metrics (CPU, memory, response time)
    - Backend JWT validation success rate
    - User-reported issues (login failures, password reset)
    - Audit logs in S3
21. **Address Issues:**
    - Fix any bugs or configuration issues
    - Adjust rate limits if needed
    - Tune token lifespans if needed

### Week 9-10: Cleanup

22. **Decommission Authentik:**
    - Verify 2 weeks of stable Keycloak operation
    - Remove AuthentikUtil and related code
    - Update documentation
    - Stop Authentik service
    - Archive Authentik data for compliance
23. **Documentation:**
    - Update README with Keycloak setup instructions
    - Document KeycloakUtil API
    - Update runbooks for common tasks (add user, reset password, etc.)

### Target Completion

**Estimated go-live:** Week 7 (7 weeks from start, ~2 months)  
**Post-migration stabilization:** Week 7-8  
**Full completion (cleanup):** Week 10

---

## Appendices

### Appendix A: Keycloak Admin API Reference

**Base URL:** `https://keycloak.vimainsurance.com/admin/realms/vima-dev`

**Common Endpoints:**
- `GET /users` - List users (with pagination, search)
- `POST /users` - Create user
- `GET /users/{id}` - Get user by ID
- `PUT /users/{id}` - Update user
- `DELETE /users/{id}` - Delete user
- `PUT /users/{id}/reset-password` - Reset password
- `GET /roles` - List realm roles
- `POST /roles` - Create realm role
- `GET /groups` - List groups
- `POST /groups` - Create group
- `PUT /users/{id}/groups/{groupId}` - Add user to group

**Authentication:**
- Client credentials grant using `vima-backend` service account
- Bearer token in Authorization header

### Appendix B: Keycloak vs Authentik Feature Comparison

| Feature | Authentik | Keycloak | Notes |
|---------|-----------|----------|-------|
| OAuth2/OIDC | ✅ | ✅ | Both support standard protocols |
| SAML 2.0 | ✅ | ✅ | Not needed for Vima |
| User Management API | ✅ | ✅ | Similar functionality |
| Group Management | ✅ | ✅ | Keycloak has more hierarchical group support |
| Custom Attributes | ✅ | ✅ | Both support custom user attributes |
| Email Templates | ✅ | ✅ | Keycloak uses FreeMarker templates |
| UI Themes | ✅ | ✅ | Keycloak more mature theme system |
| MFA/2FA | ✅ | ✅ | Keycloak has better built-in MFA support |
| User Federation | ✅ | ✅ | Keycloak has more providers (LDAP, AD, custom) |
| Admin UI | ✅ | ✅ | Keycloak admin UI more feature-rich |
| REST Admin API | ✅ | ✅ | Both have comprehensive APIs |
| Events/Audit Logs | ✅ | ✅ | Keycloak has better event listener system |
| Docker Deployment | ✅ | ✅ | Both container-ready |
| Community Support | Good | Excellent | Keycloak has larger community |
| Commercial Support | Limited | Red Hat SSO | Keycloak backed by Red Hat |

### Appendix C: Useful Keycloak Resources

**Official Documentation:**
- Keycloak Server Administration Guide: https://www.keycloak.org/docs/latest/server_admin/
- Keycloak REST API Docs: https://www.keycloak.org/docs-api/latest/rest-api/
- Securing Spring Boot Apps: https://www.keycloak.org/docs/latest/securing_apps/#_spring_boot_adapter

**Community Resources:**
- Keycloak Discourse: https://keycloak.discourse.group/
- GitHub Repository: https://github.com/keycloak/keycloak
- Docker Hub: https://hub.docker.com/r/keycloak/keycloak

**Example Implementations:**
- Spring Boot + Keycloak: https://github.com/keycloak/keycloak-quickstarts
- Custom Themes: https://github.com/keycloak/keycloak-community

---

**Document Version:** 2.0  
**Last Updated:** February 13, 2026  
**Author:** Vima Engineering Team  
**Status:** Questions Answered - Awaiting Critical Clarifications (see Follow-up Questions section)

**Change Log:**
- **v1.0 (Feb 13, 2026):** Initial draft with open questions
- **v2.0 (Feb 13, 2026):** All open questions answered; added Keycloak Flows Plan, Migration Users Plan, and Follow-up Questions for final clarifications
