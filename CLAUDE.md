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

## Environment Topology (this project)

- **No UAT environment.** `application-uat.properties` has been removed.
  Treat any "UAT" reference in older docs as stale.
- **local** and **dev** both point to the **dev database** (`vima_dev`).
  Local runs against `localhost:5433`; dev runs against the shared RDS
  instance. Schema changes for either land there.
- **test** uses the separate **test database** (`vima_test`). It exists so
  automated / integration test runs do not crowd the shared dev data. Same
  RDS host as dev unless overridden locally.
- **staging** runs against the separate **stage database** (`vima_stage`).
- **prod** runs against the **prod database** (`vima_prod`) with SSL
  enforced.
- For Slack routing all four non-prod profiles (`local`, `dev`, `test`,
  `staging`) collapse to `VimaEnvironment.DEV` / `LOCAL` and resolve every
  channel label to `TEST_NOTIFICATIONS`. Prod is the only profile that
  resolves real production webhook URLs.

## Notification Channels Policy

- Full event → Slack channel + email recipient reference (source of truth):
  [`docs/architecture/notifications/notifications-source-of-truth.md`](./docs/architecture/notifications/notifications-source-of-truth.md)
- Slack channel labels → webhook URLs:
  [`docs/architecture/notifications/notification-channels.md`](./docs/architecture/notifications/notification-channels.md).

- **local, dev, staging** — every Slack notification (legacy
  `SlackNotificationUtil` or unified `NotificationDispatcher`) must resolve to
  the `TEST_NOTIFICATIONS` channel. No production webhook should ever fire
  from these envs.
- **prod** — Slack notifications go to either `REMINDERS` or
  `SUPPORT_CLAIMS` only.
- **`POLICY_WINS` channel must not be used by this project.** Legacy
  `sendSlackMessage(..., isPolicyWin=true)` callers (currently
  `CustomerServiceImpl` and `DealsServiceImpl`) need to be rerouted or
  removed; do not add new callers with `isPolicyWin=true`.
- When asked to send to a channel by name, look the label up in
  `docs/architecture/notifications/notification-channels.md` rather than hard-coding URLs.
- Never paste a Slack webhook URL into `application-prod.properties`. Prod
  URLs are read from env vars (`NOTIFICATIONS_SLACK_WEBHOOK_URL`,
  `NOTIFICATIONS_CLAIMS_SLACK_WEBHOOK_URL`, `SLACK_WEBHOOK_URL`,
  `SLACK_REMINDER_CHANNEL_URL`).
