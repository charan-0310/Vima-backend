package com.vimainsurance.vimaadmin.notification.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Webhook URLs for each {@link SlackChannel}, bound from {@code slack.channel.*}.
 *
 * <p>Conventions:
 * <ul>
 *   <li>{@link #testNotificationsUrl} is the only URL non-prod profiles need.
 *       It is committed to the repo (the engineering #test-notifications
 *       channel is not a secret).</li>
 *   <li>{@link #remindersUrl}, {@link #supportClaimsUrl} and
 *       {@link #policyWinsUrl} are read from environment variables in
 *       {@code application-prod.properties}; they must never be committed.</li>
 * </ul>
 *
 * <p>All routing decisions go through {@link SlackChannelRouter} — never read
 * these fields directly from outside this package.
 */
@Data
@ConfigurationProperties(prefix = "slack.channel")
public class SlackChannelProperties {
    private String testNotificationsUrl = "";
    private String remindersUrl = "";
    private String supportClaimsUrl = "";
    private String policyWinsUrl = "";
}
