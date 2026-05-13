# Cursor Team Guidance (Backend)

This file defines repo-wide Cursor behavior for `Vima-backend`.
Detailed implementation conventions live in `.cursor/rules/`.

## Source Of Truth

- Architecture and coding conventions: `.cursor/rules/backend-architecture.mdc`
- **Feature completion** (production-style code review, fix issues in-repo, `./mvnw clean verify` when feasible): `.cursor/rules/feature-completion-gate.mdc`
- Documentation triggers:
  - `.cursor/rules/docs-release.mdc`
  - `.cursor/rules/docs-implementation.mdc`
  - `.cursor/rules/docs-architecture.mdc`

## How To Work In This Repo

- Follow existing backend architecture conventions before introducing new patterns.
- Prefer minimal, scoped changes over broad refactors unless explicitly requested.
- Run relevant targeted tests during development for each substantive backend code change.
- Before calling work done, do a short **production code review** pass on the diff (security, transactions, validation, tenancy), then run `./mvnw clean verify` when feasible. Prefer fixing findings over dumping a deferred checklist.
- Treat failing tests as blocking issues (do not mark work complete while tests fail unless the user explicitly approves a temporary exception).
- If validation cannot run, state what was not validated and why.

## Safety And Collaboration Defaults

- Do not run destructive git commands (`reset --hard`, force push) unless explicitly requested.
- Do not revert unrelated local changes you did not create.
- Keep responses concise and include changed file paths plus verification steps.

## Personal Overrides

Developers can use local-only overrides via:

- `CLAUDE.local.md`
- `.cursor/settings.local.json`

These should remain uncommitted.
