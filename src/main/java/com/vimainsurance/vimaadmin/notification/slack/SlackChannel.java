package com.vimainsurance.vimaadmin.notification.slack;

/**
 * Logical Slack channel labels. Callers ask for a label; the
 * {@link SlackChannelRouter} decides which physical webhook URL fulfils that
 * label in the current environment.
 *
 * <p>Project rules (mirrored in {@code CLAUDE.md} and
 * {@code docs/architecture/notifications/notification-channels.md}):
 * <ul>
 *   <li>Non-prod environments (LOCAL / DEV / STAGING) resolve every label to
 *       {@link #TEST_NOTIFICATIONS}.</li>
 *   <li>Prod resolves only to {@link #REMINDERS} or {@link #SUPPORT_CLAIMS}.</li>
 *   <li>{@link #POLICY_WINS} exists for future use; the legacy "New Policy
 *       Issued!" callers currently pass {@link #REMINDERS} instead.</li>
 * </ul>
 */
public enum SlackChannel {
    /** #test-notifications — only target for local / dev / staging. */
    TEST_NOTIFICATIONS,
    /** #reminders — prod channel for endorsements and enrollment events. */
    REMINDERS,
    /** #support-claims — prod channel for claim lifecycle events. */
    SUPPORT_CLAIMS,
    /** #policy-wins — legacy prod channel; currently unused by this project. */
    POLICY_WINS
}
