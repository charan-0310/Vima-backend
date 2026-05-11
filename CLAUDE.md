# Cursor Team Guidance (Backend)

This file defines repo-wide Cursor behavior for `Vima-backend`.
Detailed implementation conventions live in `.cursor/rules/`.

## Source Of Truth

- Architecture and coding conventions: `.cursor/rules/backend-architecture.mdc`
- Documentation triggers:
  - `.cursor/rules/docs-release.mdc`
  - `.cursor/rules/docs-implementation.mdc`
  - `.cursor/rules/docs-architecture.mdc`

## How To Work In This Repo

- Follow existing backend architecture conventions before introducing new patterns.
- Prefer minimal, scoped changes over broad refactors unless explicitly requested.
- Run relevant targeted tests during development for each substantive backend code change.
- Before finalizing backend code changes, run full verification: `./mvnw clean verify`.
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
