# Keycloak Security, Compliance & Audit-Ready Implementation Plan

**For:** Vima Insurance Backend - Keycloak Migration  
**Frameworks Covered:** IRDAI (ISNP), SOC 2 Type I & II, ISO 27001:2022  
**Author Role:** Senior Security Architect and Compliance Lead  
**Date:** February 13, 2026  
**Status:** Planning Phase

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [IRDAI (ISNP) - Information Systems & Network Security Policy](#irdai-isnp---information-systems--network-security-policy)
3. [SOC 2 Type I & II - Trust Service Criteria](#soc-2-type-i--ii---trust-service-criteria)
4. [ISO 27001:2022 - Information Security Management System](#iso-270012022---information-security-management-system)
5. [Cross-Framework Control Summary](#cross-framework-control-summary)
6. [Audit Preparation Timeline](#audit-preparation-timeline)
7. [Audit Evidence Package](#audit-evidence-package)
8. [Continuous Compliance Monitoring](#continuous-compliance-monitoring)
9. [Pre-Audit Internal Checklist](#pre-audit-internal-checklist)
10. [Compliance Roadmap](#compliance-roadmap)
11. [Key Policies and Procedures to Document](#key-policies-and-procedures-to-document)

---

## Executive Summary

This document provides a **comprehensive, audit-ready implementation plan** for configuring Keycloak to support compliance with:

- **IRDAI (ISNP)** - Information Systems & Network Security Policy for insurance companies in India
- **SOC 2 Type I & II** - Trust Service Criteria for service organizations
- **ISO 27001:2022** - Information Security Management System (ISMS)

The plan includes:
- **Control mappings** showing how Keycloak features align with regulatory requirements
- **Implementation checklists** with specific tasks, owners, and timelines
- **Evidence packages** listing artifacts needed for audits
- **Continuous monitoring procedures** for ongoing compliance
- **Cross-framework controls** that satisfy multiple regulations simultaneously

**Key Benefits:**
- Single implementation satisfies multiple compliance frameworks
- Audit-ready evidence collection from day one
- Clear ownership and accountability for each control
- Reduced compliance overhead through automation and integration

---

## IRDAI (ISNP) - Information Systems & Network Security Policy

**Applicability:** Vima Insurance (insurance company regulated by IRDAI in India) must comply with IRDAI's Information and Cyber Security Guidelines (Master Circular Ref: IRDAI/IT/CIR/MISC/194/10/2023 or latest).

### A. IRDAI ISNP Control Areas Relevant to Identity and Access Management

| Control area | IRDAI requirement (summary) | Keycloak implementation | Audit evidence |
|--------------|----------------------------|------------------------|-----------------|
| **AC-1: User identification & authentication** | Unique user IDs; strong password policy; MFA for privileged users. | Realm roles with unique usernames; password policy (12 char, complexity); optional MFA (TOTP). | Password policy config; user list; MFA enrollment report; Keycloak realm settings export. |
| **AC-2: Access control** | Role-based access (RBAC); least privilege; segregation of duties. | Realm roles (SUPER_ADMIN, VIMA_ADMIN, HR_ADMIN, etc.); groups for org isolation; role assignments per user. | Role definitions and mappings; user→role report; no overlapping admin/dev users. |
| **AC-3: Access review** | Periodic review of user access; revoke inactive users. | Keycloak Admin API + script to list users, last login, roles; quarterly access review. | Access review report (quarterly); disabled users list; approval sign-off. |
| **AC-4: Privileged access** | Extra controls for admin users (MFA, monitoring, approval). | MFA for SUPER_ADMIN and VIMA_ADMIN roles (enforced in realm auth flow); admin actions logged in Keycloak admin events. | MFA enrollment for admins; admin event logs (S3); restricted admin console access (VPN/IP). |
| **AU-1: Audit logging** | Log authentication, authorization, admin changes; retain for min. period (e.g. 1 year). | Keycloak event listener → S3 (login, logout, password change, user creation, role assignment). | Event logs in S3; retention policy doc; quarterly log review. |
| **IA-1: Identification & authentication** | Only authorized individuals can access; MFA for sensitive access. | All API requests require JWT; admin console requires username/password (+ MFA for admins). | Auth failure logs; successful login logs; MFA config. |
| **SC-1: Transmission security** | All auth traffic encrypted (TLS 1.2+). | Keycloak behind Nginx with TLS 1.2+; no plain HTTP for auth. | SSL certificate; Nginx TLS config; SSL Labs test result. |

### B. IRDAI ISNP Implementation Checklist (for Keycloak)

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

### C. IRDAI ISNP Audit Evidence Package

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

## SOC 2 Type I & II - Trust Service Criteria

**Applicability:** SOC 2 report for Vima's systems (if offering insurance as a service to customers or partners). Type I = design of controls; Type II = operating effectiveness over a period (typically 6-12 months).

### A. SOC 2 Control Mapping (TSC - Trust Service Criteria)

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

### B. SOC 2 Type II - Operating Effectiveness (6-12 months)

For **Type II**, auditor samples evidence **over time** (e.g. 3–12 months). After Keycloak go-live:

| Period | What to collect | Frequency |
|--------|-----------------|-----------|
| **Monthly** | Access review (if monthly) or log review; failed login report; MFA enrollment status. | Every 30 days |
| **Quarterly** | Formal access review (user list → manager approval); disable inactive users; security patch status. | Every 90 days |
| **On event** | User onboarding/offboarding; role change; admin actions; incident response for suspicious login. | As it occurs |
| **Continuous** | Event logs (login, logout, password change, admin events) in S3; uptime/availability monitoring. | Real-time |

Auditor will **sample** (e.g. 25–40 items) from each control population over the report period. Ensure logs and approvals are retained and accessible.

### C. SOC 2 Implementation Checklist (for Keycloak)

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

### D. SOC 2 Narrative and Policy Requirements

For SOC 2 report, prepare the following documents (with Keycloak sections):

1. **System description** – Include Keycloak role, architecture (realms, clients), and how it integrates with Vima backend.
2. **Access control policy** – Define roles, approval for new users, MFA for admins, quarterly reviews, offboarding.
3. **Password policy** – Minimum 12 char, complexity, no expiration, history 5; enforced by Keycloak.
4. **Logging and monitoring policy** – Events logged to S3, retention 1 year, quarterly review, alerting.
5. **Change management policy** – Keycloak upgrades tested in dev, approved before prod; document changes.
6. **Incident response plan** – Process for security events (e.g. brute force, compromise); Keycloak logs used for investigation.

---

## ISO 27001:2022 - Information Security Management System

**Applicability:** ISO 27001 certification (if Vima is pursuing or already certified). Keycloak supports multiple Annex A controls (access, logging, crypto).

### A. ISO 27001 Annex A Control Mapping (Identity & Access)

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

### B. ISO 27001 Implementation Checklist (for Keycloak)

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

### C. ISO 27001 Audit Evidence Package

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

## Cross-Framework Control Summary

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

## Audit Preparation Timeline

| Week | Activity | Deliverable | Framework |
|------|----------|-------------|-----------|
| **Week 1** | Document access control and password policies referencing Keycloak. | Policy docs (access, password, logging). | All |
| **Week 2** | Configure Keycloak per checklists (password, MFA, logging, IP restriction). | Keycloak realm config complete; event listener → S3 deployed. | All |
| **Week 3** | Implement quarterly access review process and document. | Access review SOP; sample review (test run). | All |
| **Week 4–6** | Deploy Keycloak, migrate users, and begin logging (start of SOC 2 Type II observation). | Keycloak production; event logs flowing to S3. | SOC 2 Type II |
| **Ongoing** | Collect evidence: quarterly access reviews, monthly log reviews, admin actions, onboarding/offboarding. | Logs in S3; review reports; approval tickets. | SOC 2 Type II, ISO 27001 |
| **Pre-audit** | Prepare evidence package (see below); run internal audit; fix gaps. | Evidence folders; internal audit report. | All |

---

## Audit Evidence Package

### Deliverables for Auditor

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

## Continuous Compliance Monitoring

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

## Pre-Audit Internal Checklist

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

## Compliance Roadmap

### Post–Migration Compliance Activities

After Keycloak is migrated and operational:

| Quarter | Compliance activity |
|---------|---------------------|
| **Q1** | First quarterly access review; refine process; collect logs for SOC 2 Type II (if pursuing). |
| **Q2** | Second quarterly review; log review; policy updates if needed; internal audit. |
| **Q3** | Third quarterly review; pre-audit prep (collect evidence, gap analysis); engage auditor if ready. |
| **Q4** | SOC 2 or ISO 27001 audit (if scheduled); address findings; obtain report. |

**IRDAI:** Ongoing; ISNP compliance is part of regular insurance audits (IRDAI inspection or internal audit). Evidence prepared for each inspection cycle.

---

## Key Policies and Procedures to Document

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

## Summary

### Implementation Overview

- **IRDAI (ISNP):** 12 implementation steps; 7 evidence items; focus on access control, audit logging, TLS, MFA for admins.
- **SOC 2 Type I & II:** 9 controls (CC6.x, CC7.x, A1.x); narrative and policies; continuous evidence collection over 6-12 months for Type II.
- **ISO 27001:** 14+ Annex A controls; evidence package; internal checklist; pre-audit mock.
- **Cross-framework:** 8 common controls implemented once; satisfy multiple audits.
- **Continuous compliance:** Quarterly access reviews, monthly log reviews, security patching, MFA checks, offboarding audits.
- **Policies needed:** 7 key policies/procedures referencing Keycloak.

### Next Steps

1. Review this plan with compliance/security team
2. Assign owners for each implementation task
3. Begin documentation of required policies (Week 1)
4. Configure Keycloak according to checklists (Weeks 1-3)
5. Implement continuous monitoring (from go-live)
6. Collect evidence systematically for audit readiness

Use this plan as a roadmap to configure Keycloak in an audit-ready manner and to prepare evidence for IRDAI, SOC 2, and ISO 27001 audits.

---

**Document Version:** 1.0  
**Last Updated:** February 13, 2026  
**Next Review:** Post-Keycloak deployment
