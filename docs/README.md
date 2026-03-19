# Vima Backend — Documentation

## Structure

| Folder | Purpose | When to update |
|---|---|---|
| `prd/` | Product Requirements Documents — what we're building and why | Before starting a new feature or phase |
| `architecture/` | Architecture Decision Records — how we decided to build it | When making structural, schema, or design decisions |
| `implementation/` | Implementation guides — how it was built, technical details | During or after implementing a feature |
| `release/` | Release notes — what shipped, when, and any breaking changes | On every merge to main |
| `guides/` | Functional & operational guides — how to use features | When features are user-ready |

## Documents Index

### PRD — Product Requirements

- [Employee-Policy Mapping Plan](prd/group-insurance/emp-policy-map-plan.md)
- [Plan Actual Relationship Field](prd/group-insurance/plan-actual-relationship-field.md)

### Architecture — Decision Records

- [Enrollment Engineering Architecture](architecture/enrollment/enrollment-engineering-architecture.md)
- [Database Tables Overview](architecture/database/database-tables-overview.md)
- [Tech Stack & Code Patterns](architecture/tech-stack-and-code-patterns.md)
- [ADR Template](architecture/_TEMPLATE.md)

### Implementation — Technical Guides

- [Bulk Upload & Relationship Identity](implementation/group-insurance/bulk-upload-and-relationship-identity.md)
- [Enrollment Bulk Upload: Renewal and Policy Map Rule](implementation/group-insurance/enrollment-bulk-upload-renewal-policy-map.md)
- [Data Cleanup — Fresh Group Insurance](implementation/database/data-cleanup-fresh-group-insurance.md)
- [Audit Implementation](implementation/audit/audit-implementation.md)
- [Token Security Implementation Plan](implementation/security/token-security-implementation-plan.md)

### Release Notes

- [Keycloak Migration Plan](release/keycloak-migration-plan.md)
- [Keycloak Security & Compliance Audit](release/keycloak-security-compliance-audit-plan.md)
- [Jira Ticket Changes Summary](release/jira-ticket-changes-summary.md)
- [Release Note Template](release/_TEMPLATE.md)

### Guides — Functional & Operational

- [Feature Flags Guide](guides/feature-flags.md)
- [Audit Functional Overview](guides/audit/audit-functional-overview.md)

> Product/UX guides (enrollment functional guide, wireframes) live in the [vima-web-portal repo](https://github.com/Vima-Insurance/vima-web-portal/tree/lovable/docs).

## Conventions

- **File naming**: `kebab-case.md` (e.g., `enrollment-engineering-architecture.md`)
- **ADR naming**: `ADR-XXX-short-title.md` (e.g., `ADR-001-enrollment-db-schema.md`)
- **Release naming**: `YYYY-MM-DD-release-name.md` (e.g., `2025-03-09-enrollment-phase-2.md`)
- **Templates**: Each category has a `_TEMPLATE.md` — copy it when creating new docs
