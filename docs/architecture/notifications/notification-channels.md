# Slack Notification Channels — Webhook URLs

Lookup table for Slack channel labels → configuration. For the wider reference
(which event posts to which channel, who receives each email) see
[`notifications-source-of-truth.md`](./notifications-source-of-truth.md) in
this folder. Pass the matching `SlackChannel` enum into `SlackNotificationUtil` /
`NotificationDispatcher`; **never commit real `hooks.slack.com` URLs** in source or docs.

## Labels → configuration

| Label                | Slack channel name   | URL source (env / property)                                                                      | Allowed envs (this project) |
|----------------------|----------------------|--------------------------------------------------------------------------------------------------|-----------------------------|
| `TEST_NOTIFICATIONS` | `#test-notifications`| `slack.channel.test-notifications-url` ← `SLACK_TEST_NOTIFICATIONS_WEBHOOK_URL` (Secrets Manager / local only) | local, dev, test, staging   |
| `REMINDERS`          | `#reminders`         | `slack.channel.reminders-url` ← `SLACK_REMINDERS_WEBHOOK_URL`                                    | prod only                   |
| `SUPPORT_CLAIMS`     | `#support-claims`    | `slack.channel.support-claims-url` ← `SLACK_SUPPORT_CLAIMS_WEBHOOK_URL`                          | prod only                   |
| `POLICY_WINS`        | `#policy-wins`       | `slack.channel.policy-wins-url` ← `SLACK_POLICY_WINS_WEBHOOK_URL`                                | reserved (currently unused) |

## Routing rules (this project)

- **local + dev + test + staging** — every Slack notification resolves to
  `TEST_NOTIFICATIONS`. `SlackChannelRouter` enforces this at the resolution
  step, regardless of which label the caller asked for.
- **prod** — Slack notifications go to `REMINDERS` or `SUPPORT_CLAIMS`. The
  router skips with a `WARN` log if `TEST_NOTIFICATIONS` is asked for in
  prod, or if a requested label has no URL configured.
- **`POLICY_WINS`** is kept in the enum and has a property key. Today the
  legacy "New Policy Issued!" callers (only `DealsServiceImpl`, after the
  `CustomerServiceImpl` removal on 2026-05-15) pass `REMINDERS` instead;
  when we're ready to use the dedicated channel, swap their constant to
  `SlackChannel.POLICY_WINS` and set `SLACK_POLICY_WINS_WEBHOOK_URL` in the
  prod deployment env.

## How URLs reach the code

Source of truth: [`SlackChannelRouter`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/slack/SlackChannelRouter.java).
URL property keys (bound to [`SlackChannelProperties`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/slack/SlackChannelProperties.java)):

| Property key                         | Set in                                                          |
|--------------------------------------|------------------------------------------------------------------|
| `slack.channel.test-notifications-url` | `application-local.properties`, `application-dev.properties`, `application-test.properties`, `application-stage.properties` |
| `slack.channel.reminders-url`        | `application-prod.properties` ← `SLACK_REMINDERS_WEBHOOK_URL`    |
| `slack.channel.support-claims-url`   | `application-prod.properties` ← `SLACK_SUPPORT_CLAIMS_WEBHOOK_URL` |
| `slack.channel.policy-wins-url`      | `application-prod.properties` ← `SLACK_POLICY_WINS_WEBHOOK_URL`  |

The router is the only piece of code that reads these. Every Slack send goes
through it:

- Unified flow: `NotificationDispatcher.sendSlack` calls
  `router.channelForEvent(eventType)` → `router.resolveUrl(channel)` →
  `NotificationSlackWebhookClient.postMessageToWebhookUrl(text, url)`.
- Legacy flow: `SlackNotificationUtil.sendSlackMessage(..., SlackChannel)`
  delegates URL resolution to the router and posts via `RestTemplate`.

## How environment is decided

[`VimaEnvironment`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/slack/VimaEnvironment.java)
is resolved exactly once at startup from `SPRING_PROFILES_ACTIVE` and exposed
as a Spring bean by `VimaEnvironmentConfig`. Mapping:

| Spring profile | `VimaEnvironment` | Database |
|----------------|-------------------|----------|
| `prod` / `production` | `PROD` | `vima_prod` |
| `stage` / `staging`   | `STAGING` | `vima_stage` |
| `dev` / `development` | `DEV` | `vima_dev` |
| `test`         | `DEV` | `vima_test` (separate, so test data does not crowd dev) |
| `uat`          | `DEV` | n/a — UAT deleted; mapping kept as a safety fallback |
| `local`        | `LOCAL` | `vima_dev` |
| unknown / unset | `LOCAL` (defaults to the safest non-prod env) | — |

For Slack routing all four non-prod profiles (`local`, `dev`, `test`,
`staging`) collapse to `TEST_NOTIFICATIONS`. Prod is the only profile that
resolves real production webhook URLs.
