# Notifications — Source of Truth

| Field | Value |
|---|---|
| **Status** | Accepted |
| **Last reviewed** | 2026-05-15 |
| **Domain** | notifications (Slack + email) |
| **Owner** | Platform team |

This is the canonical reference for every Slack message and email the Vima
backend can produce. If you're adding or changing a notification, update this
file in the same PR — diverging behaviour from this document is a defect.

For Slack channel labels and webhook URLs only, see
[`notification-channels.md`](./notification-channels.md) in this folder.
This file references it instead of duplicating the URL table.

---

## 1. Architecture in one paragraph

Every Slack and email send goes through one of two paths:

- **Unified** — Code calls `FlagshipNotificationService.schedule*` (or
  similar emitter), which after the surrounding transaction commits creates
  an `AdminNotification` row plus per-channel `NotificationDelivery` rows.
  A background dispatcher (`NotificationDispatcher`) reads each delivery
  and sends. Recipients are resolved by `NotificationRoutingResolver`. Slack
  URLs are resolved by `SlackChannelRouter`.
- **Direct** — Code calls `emailService.sendTemplateEmail(...)` or
  `slackNotificationUtil.sendSlackMessage(...)` directly, bypassing the
  unified table. Used today for legacy paths (employee-facing transactional
  emails, the "New Policy Issued!" Slack post).

Environment safety:
[`VimaEnvironment`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/slack/VimaEnvironment.java)
is resolved once at boot from `SPRING_PROFILES_ACTIVE`. Non-prod profiles
(`local`, `dev`, `test`, `staging`, `uat-if-ever-set`) collapse so every
Slack send goes to `#test-notifications` regardless of which channel label
the caller asked for. Email goes to whatever the recipient resolver returns
in every env (email has no equivalent test-only sink yet — flagged in §8).

---

## 2. Unified events — Slack channel and email recipients

Single table, one row per `NotificationEventType` value. Slack column shows
the logical channel the router asks for; the actual webhook URL it resolves
to is per-env (see [`notification-channels.md`](./notification-channels.md)).

| Event | Trigger | Slack channel (router asks for) | Email recipients |
|---|---|---|---|
| `ENDORSEMENT_UPLOADED` | HR or VIMA admin uploads an endorsement | `REMINDERS` | Active users with role `VIMA_ADMIN` only — no SUPER_ADMIN / ADMIN / SALES_ADMIN, no HR admins. Email is delivered (no suppression). |
| `ENDORSEMENT_COMPLETED` | Endorsement processing completes (auto-confirm or manual) | `REMINDERS` | HR admins whose `admin_users.organization_id` equals the endorsement org **only** + the uploader if their org matches or is null. VIMA platform admins are **not** in the recipient list. Email delivered. |
| ~~`ENROLLMENT_WINDOW_OPENED`~~ | Removed 2026-05-15 | n/a | n/a — emitter, scheduler call, and template deleted. Enum value kept for SQL constraint stability. |
| `ENROLLMENT_WINDOW_CLOSING_SOON` | Fires **1 day before window end date** (`expiresAt.toLocalDate().equals(tomorrow)` check at `EnrollmentInvitationServiceImpl:464`, evaluated daily at 9:00 IST inside the reminder cron). Dedup key `ENROLLMENT_WINDOW_CLOSING_SOON:{windowId}` guarantees one notification per HR per window. | `REMINDERS` | HR admins of the org. Email delivered. |
| `ENROLLMENT_WINDOW_CLOSED` | Window closes (HR or scheduler) | `REMINDERS` | HR admins of the org → email + bell + Slack. VIMA platform admins → bell + Slack only (email suppressed by `EMAIL_DISABLED_FOR_VIMA_PLATFORM_CATEGORIES`). |
| `ENROLLMENT_ALL_SUBMITTED` | Every employee in the window has submitted (verified by recipient-side check, see §2.1) | `REMINDERS` | HR admins of the org → exactly one email per HR per window (deduped). VIMA platform admins → bell + Slack only, no email. |
| `EMPLOYEE_CLAIM_SUBMITTED` | Employee submits a claim | `SUPPORT_CLAIMS` | HR + VIMA platform admins → bell + Slack only. **No email to anyone** (entire CLAIM category is in `EMAIL_DISABLED_FOR_ALL_CATEGORIES`). |
| `EMPLOYEE_CLAIM_QUERY_RAISED` | Insurer/admin raises a query on a claim | `SUPPORT_CLAIMS` | HR + VIMA platform admins → bell + Slack only. No email. |
| `EMPLOYEE_CLAIM_QUERY_RESPONDED` | Admin responds to a query | `SUPPORT_CLAIMS` | HR + VIMA platform admins → bell + Slack only. No email. |
| `EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED` | Employee submits a response to a query | `SUPPORT_CLAIMS` | HR + VIMA platform admins → bell + Slack only. No email. |
| `EMPLOYEE_CLAIM_APPROVED` | Claim approved | `SUPPORT_CLAIMS` | HR + VIMA platform admins → bell + Slack only. No email. |
| `EMPLOYEE_CLAIM_REJECTED` | Claim rejected | `SUPPORT_CLAIMS` | HR + VIMA platform admins → bell + Slack only. No email. |
| `EMPLOYEE_CLAIM_SETTLED` | Claim settled | `SUPPORT_CLAIMS` | HR + VIMA platform admins → bell + Slack only. No email. |
| ~~`ENROLLMENT_SUBMISSION_APPROVED`~~ | Removed | n/a | n/a — emitter and resolver method deleted; enum value retained only because the SQL constraint in `V87__notifications_backfill_company_id.sql` still references it. Do not re-add without a deliberate decision. |

### Slack channel routing rule

`SlackChannelRouter.channelForEvent`:

- claim lifecycle events (the seven `EMPLOYEE_CLAIM_*` values) → `SUPPORT_CLAIMS`
- everything else → `REMINDERS`

### Email recipient resolution

`NotificationRoutingResolver`:

- `ENDORSEMENT_UPLOADED` → `findActiveByRoles(VIMA_ADMIN_ONLY_ROLES)` —
  active users with role `VIMA_ADMIN` (or `ROLE_VIMA_ADMIN`). Not org-scoped
  because this is internal-only ops visibility.
- `ENDORSEMENT_COMPLETED` → `resolveEndorsementCompletedRecipients` —
  `findByOrganization_OrganizationId(orgId)` filtered to active HR admins,
  plus the uploader if their org matches or is null.
- `ENROLLMENT_WINDOW_*` and `ENROLLMENT_ALL_SUBMITTED` →
  `resolveHrAndVimaPlatformRecipients` — org HR admins + all active VIMA
  platform admins, deduped by user id.
- `EMPLOYEE_CLAIM_*` → `resolveClaimsTeamRecipients` (same impl as
  `resolveHrAndVimaPlatformRecipients`).

### Email suppression rules

Two filters in `NotificationDispatcher.sendEmail`, checked in order:

1. **`EMAIL_DISABLED_FOR_ALL_CATEGORIES = {CLAIM}`** — no recipient receives
   an email for any event in these categories, regardless of role. Slack and
   bell still fire. Today this covers all seven `EMPLOYEE_CLAIM_*` events.
2. **`EMAIL_DISABLED_FOR_VIMA_PLATFORM_CATEGORIES = {ENROLLMENT}`** — for
   events in these categories, recipients whose role is in the VIMA platform
   set (`SUPER_ADMIN`, `ADMIN`, `VIMA_ADMIN`, `SALES_ADMIN`) get bell + Slack
   but no email. HR admins still receive email.

`ENDORSEMENT` is in neither set — both HR admins and VIMA_ADMIN receive
endorsement emails (`ENDORSEMENT_UPLOADED` is VIMA_ADMIN-only by recipient
resolution; `ENDORSEMENT_COMPLETED` is HR-only by recipient resolution).

## 2.1. Why `ENROLLMENT_ALL_SUBMITTED` delivers exactly one email per HR per window

Three guarantees stack:

1. **Trigger gate.** `EnrollmentSubmissionServiceImpl.maybeEmitEnrollmentAllSubmitted`
   runs after every employee submission, but it loops over every employee in
   the window and short-circuits unless every one of them has status
   `SUBMITTED`. Only the submission that completes the last outstanding one
   passes this gate.
2. **Dedup key.** When the emitter does fire, it builds
   `dedupKey = "ENROLLMENT_ALL_SUBMITTED:{windowId}:{adminId}"` for each HR
   admin. `NotificationServiceImpl.createIfAbsent` rejects any second insert
   with the same key — the unique constraint on `admin_notifications.dedup_key`
   catches it at the SQL level, so even concurrent submissions cannot create
   duplicates.
3. **One email per HR row.** Each HR admin gets their own
   `AdminNotification` row with their own `EMAIL` delivery row. The
   dispatcher sends that row exactly once on success.

Net effect: if a window has 3 HR admins, each receives exactly one email
the moment the last employee submits. Subsequent submissions in the same
window (e.g., a resubmission after rejection) do not produce another email.

---

## 3. Legacy Slack senders (not unified)

| Caller | Channel | What it sends |
|---|---|---|
| `DealsServiceImpl:790` | `REMINDERS` | "New Policy Issued!" when a policy is uploaded |
| ~~`CustomerServiceImpl:759`~~ | Removed (Customer→Deal Slack post deleted on 2026-05-15) | — |

Both go through `SlackNotificationUtil.sendSlackMessage(title, body, SlackChannel)` → `SlackChannelRouter.resolveUrl(channel)`. Non-prod still routes to `#test-notifications`; prod posts the `REMINDERS` URL.

---

## 4. Direct emails (not unified)

These go straight through `IEmailService` without creating an
`AdminNotification` row. They will not appear in the bell, and they bypass
any guardrails added to `NotificationDispatcher`.

### To employees / end users

| Trigger | Caller | Recipient | Template |
|---|---|---|---|
| Enrollment magic-link invite (first send) | `EnrollmentInvitationServiceImpl:209` | Employee email | `enrollment-invitation.html` |
| Enrollment invite — pending retry | `EnrollmentInvitationServiceImpl:258` | Employee email | `enrollment-invitation.html` |
| Enrollment invite — manual resend by HR | `HRApprovalServiceImpl:477` | Employee email | `enrollment-invitation.html` |
| Enrollment reminder (scheduled, daily 9:00 IST) | `EnrollmentInvitationServiceImpl:400, 493` | Employee email | `enrollment-reminder.html` |
| Enrollment submission acknowledgement | `EnrollmentSubmissionServiceImpl:323` | Employee email | `enrollment-submission.html` |
| HR rejection notice | `HRApprovalServiceImpl:1240` (now ~`HRApprovalServiceImpl:1199` after the approval-email removal) | Employee email | simple text body |
| Employee onboarding welcome | `EmployeeOnboardingPipeline:110` | Employee email | `welcome.html` |
| Employee login welcome (resend) | `OrganizationEmployeeLoginServiceImpl:304` | Employee email | `welcome.html` |
| Health-card delivery | `OrganizationEmployeeLoginServiceImpl:471` | Employee email | `health-card-email.html` |
| Broadcast email (sendToAll / list) | `OrganizationServiceImpl:675, 707` | Many employee emails | inline HTML body (no template) |
| Demo account welcome (async) | `DemoSetupEmailNotifier:30` | Demo admin user email | `demo-welcome.html` |
| Demo expiry warning (scheduled, daily 3:30 UTC) | `DemoAccountCleanupScheduler:102` | Demo admin user email | `demo-expiry-warning.html` |
| Password reset | `OrganizationEmployeeLoginServiceImpl:344` | Employee email (sent **by Keycloak**) | Keycloak-managed; not visible in Vima logs |
| ~~HR approval — cost-sharing notice to employee~~ | Removed 2026-05-15 | — | `cost-sharing-notice.html` is now orphaned on disk |

### To internal / VIMA staff

| Trigger | Caller | Recipient | Notes |
|---|---|---|---|
| Admin welcome — new admin user | `AdminUserServiceImpl:264` | Email supplied in request payload | No org filter; recipient is the new admin themselves |
| Demo cleanup summary | `DemoAccountCleanupScheduler:82` | `${demo.internal-notification-email}` (default `team@vimainsurance.com`) | Subject: `Demo Accounts Expired: N revoked` |
| Payroll deduction report (monthly, 1st @ 11:30 IST) | `PayrollReportGeneratorJob:76` | `PayrollReportSchedule.recipientEmails` per org | XLSX attached |

---

## 5. Scheduled jobs that send notifications

| Job | Cron (UTC) | Sends |
|---|---|---|
| `EnrollmentInvitationServiceImpl` daily reminder loop | `0 30 3 * * *` (9:00 IST) | Enrollment reminder emails per invitation |
| `DemoAccountCleanupScheduler.cleanupExpiredDemoAccounts` | `0 0 0 * * *` | Internal summary email to `demo.internal-notification-email` |
| `DemoAccountCleanupScheduler.sendExpiryWarnings` | `0 30 3 * * *` (9:00 IST) | Warning email to each demo user expiring within 24h |
| `PayrollReportGeneratorJob` | `0 0 6 1 * *` (1st of month, 11:30 IST) | Payroll deduction report + XLSX to schedule recipients |
| `EndorsementScheduleScheduler` | `0 0 19 * * ?` (00:30 IST next day) — disabled outside prod | Auto-confirms endorsement schedules; may emit `ENDORSEMENT_COMPLETED` (unified) when it does |
| `EnrollmentWindowExpiryScheduler` | `0 0 0 * * *` (5:30 IST) | Marks `SCHEDULED` windows `EXPIRED`; may emit `ENROLLMENT_WINDOW_CLOSED` |

---

## 6. Configuration

### Slack

| Property | Value source |
|---|---|
| `slack.channel.test-notifications-url` | Hard-coded engineering test webhook in `application-{local,dev,test,stage,uat-if-set}.properties` |
| `slack.channel.reminders-url` | `${SLACK_REMINDERS_WEBHOOK_URL:}` in `application-prod.properties` |
| `slack.channel.support-claims-url` | `${SLACK_SUPPORT_CLAIMS_WEBHOOK_URL:}` in `application-prod.properties` |
| `slack.channel.policy-wins-url` | `${SLACK_POLICY_WINS_WEBHOOK_URL:}` in `application-prod.properties` (currently unused; reserved) |

### Email

| Property | Value source |
|---|---|
| `mail.provider` | `ses` everywhere |
| `aws.ses.from-email` | `no-reply@vimainsurance.com` |
| `aws.sqs.mail-queue.listener-enabled` | `true` in prod and stage / dev, `false` for local |
| `claims.email.manual-submission-admin-notification-enabled` | `true` (default). Set `false` to suppress the admin alert when a claim must be manually submitted to the insurer. |
| `notifications.allow-global-hr-recipient-fallback` | `false` everywhere. **Do not set true in production** — would broadcast org-scoped notifications to every HR admin globally. |
| `app.base-url` | Per-env URL used to construct deep links in notifications |

---

## 7. Where it lives in the code

| Concern | File |
|---|---|
| Slack channel labels | [`SlackChannel.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/slack/SlackChannel.java) |
| Slack URL routing | [`SlackChannelRouter.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/slack/SlackChannelRouter.java) |
| Environment enum | [`VimaEnvironment.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/slack/VimaEnvironment.java) |
| Webhook URL config | [`SlackChannelProperties.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/slack/SlackChannelProperties.java) |
| Legacy Slack util | [`SlackNotificationUtil.java`](../../../src/main/java/com/vimainsurance/vimaadmin/util/SlackNotificationUtil.java) |
| Unified emitters | [`FlagshipNotificationService.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/FlagshipNotificationService.java) |
| Unified create | [`NotificationServiceImpl.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/NotificationServiceImpl.java) |
| Unified dispatch | [`NotificationDispatcher.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/NotificationDispatcher.java) |
| Recipient resolution | [`NotificationRoutingResolver.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/NotificationRoutingResolver.java) |
| Slack HTTP client | [`NotificationSlackWebhookClient.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/NotificationSlackWebhookClient.java) |
| Event enum | [`NotificationEventType.java`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/enums/NotificationEventType.java) |
| Email service | [`EmailServiceImpl.java`](../../../src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl/EmailServiceImpl.java) |
| Email templates | `src/main/resources/templates/email/` |
| Channel URL reference | [`notification-channels.md`](./notification-channels.md) |
| Env / DB topology rules | [`../../../CLAUDE.md`](../../../CLAUDE.md) |

---

## 8. Known gaps and risks

1. **Direct emails bypass the unified system.** All entries in §4 send via `IEmailService` directly. Guardrails on `NotificationDispatcher` (env suppression, audit logging, future opt-out) do not apply to them. Consider migrating high-volume employee transactional emails (`enrollment-invitation`, `enrollment-reminder`, `welcome`, `health-card-email`, broadcast) into the unified path so we have one place to govern email.
2. **No "test sink" for email in non-prod.** Slack non-prod sends are pinned to `#test-notifications`; email non-prod sends still hit real recipient addresses pulled from the DB. Dev/staging environments running against `vima_dev` / `vima_stage` will email real-looking HR addresses if those rows exist. Mitigation today: keep production-like emails out of `vima_dev` and `vima_stage`. Long-term fix: an env-aware email router that rewrites recipients to a known internal mailbox in non-prod.
3. **`AdminUserServiceImpl.sendWelcomeEmail` has no org-scope check** (`:264`). Safe today because the recipient is the new admin themselves, but the endpoint should still enforce who is allowed to create admin users in which org.
4. **`OrganizationServiceImpl.broadcastEmail` body is supplied by the API caller** (`:675, 707`). Worth confirming auth on the endpoint plus rate limits — and that we don't accept inline HTML from untrusted input.
5. **`notifications.allow-global-hr-recipient-fallback`** would, if ever turned on, cause org-scoped HR notifications to broadcast to every HR_ADMIN in the database. Default is `false` and no property file sets it, but it's still env-var settable. Consider removing the fallback branch entirely (delete the `if (notificationsProperties.isAllowGlobalHrRecipientFallback())` branch in `NotificationRoutingResolver.findHrAdminsForOrganization`).
6. **`ENROLLMENT_SUBMISSION_APPROVED` cleanup is partial.** Emitter, scheduler, resolver method and call sites are gone. The enum value, the `case ... -> List.of()` switch arm, and the `notification-enrollment-submission-approved.html` template remain — harmless but worth deleting in the next sweep along with the SQL constraint update.
7. **Orphaned templates on disk:** `cost-sharing-notice.html` (since 2026-05-15), `quote-notification.html`, `claim_status_changed.html`, `claim_submitted_to_insurer.html`, `notification-enrollment-submission-approved.html`. No callers. Delete or wire.
8. **Password reset email comes from Keycloak**, not Vima. Audit logs do not capture it. Failures show up only in Keycloak logs.

---

## 9. How to add a new event

1. Add the enum value to
   [`NotificationEventType`](../../../src/main/java/com/vimainsurance/vimaadmin/notification/enums/NotificationEventType.java).
2. Update `NotificationRoutingResolver.resolveRecipients` switch to declare
   the recipient audience (org-scoped HR? VIMA-only? both?).
3. If it's a claim-style event, add the enum value to the claim branch of
   `SlackChannelRouter.channelForEvent`. Otherwise it defaults to
   `REMINDERS`.
4. Add an emitter method on `FlagshipNotificationService`
   (`scheduleXxx` + private `emitXxx`) following the existing pattern.
   Build a `CreateNotificationCommand` with `dedupKey`, recipient,
   `companyId`, template name, vars. Use `AfterCommitNotificationRunner` so
   the emit only fires after the surrounding DB transaction commits.
5. Add an email template at
   `src/main/resources/templates/email/notification-<event-slug>.html`.
6. **Update this document.** Add a row to §2 and any direct email entries
   to §4 in the same PR.
7. Decide whether VIMA_ADMIN should receive email or only the in-app bell.
   If only bell, add the event's category to
   `NotificationDispatcher.TEMP_EMAIL_DISABLED_FOR_VIMA_ADMIN_CATEGORIES`
   (today: `ENROLLMENT`, `CLAIM`).

---

## 10. Change log

| Date | Change |
|---|---|
| 2026-05-15 | Introduced `SlackChannelRouter`, `SlackChannel`, `VimaEnvironment`. Retired `EngineeringTestSlackWebhookOverrides` and the legacy `slack.webhook.url` / `slack.reminder.channel.url` / `slack.sendmessage` / `notifications.slack-*` keys. Pinned non-prod Slack to `#test-notifications`. |
| 2026-05-15 | Deleted `application-uat.properties`. Patched `application-local.properties` and `application-test.properties` (both had real prod webhook URLs hard-coded). |
| 2026-05-15 | Removed Customer→Deal "New Policy Issued!" Slack post (`CustomerServiceImpl:759`). |
| 2026-05-15 | Removed `ENROLLMENT_SUBMISSION_APPROVED` emitter, scheduler call, and resolver method. |
| 2026-05-15 | Narrowed `ENDORSEMENT_UPLOADED` recipients to `VIMA_ADMIN` only. Removed `ENDORSEMENT` from the VIMA_ADMIN email suppression set. |
| 2026-05-15 | Removed `HRApprovalServiceImpl.sendApprovalEmail` (cost-sharing notice). |
| 2026-05-15 | Email policy revision: `CLAIM` category now in `EMAIL_DISABLED_FOR_ALL_CATEGORIES` (no email for any claim event). Renamed `TEMP_EMAIL_DISABLED_FOR_VIMA_ADMIN_CATEGORIES` → `EMAIL_DISABLED_FOR_VIMA_PLATFORM_CATEGORIES` and broadened role match to all 4 VIMA platform roles (SUPER_ADMIN / ADMIN / VIMA_ADMIN / SALES_ADMIN), suppression set narrowed to `{ENROLLMENT}`. |
| 2026-05-15 | Removed `ENROLLMENT_WINDOW_OPENED` event. Deleted both call sites (`EnrollmentWindowServiceImpl:714`, `EnrollmentInvitationServiceImpl:743`), the `scheduleEnrollmentWindowOpened` + `emitEnrollmentWindowOpened` methods, and `notification-enrollment-window-opened.html`. Enum value retained for SQL constraint stability. |
