package com.vimainsurance.vimaadmin.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "notifications")
public class NotificationsProperties {

    /**
     * Dedicated Slack Incoming Webhook for unified notifications (separate from legacy SlackNotificationUtil).
     */
    private String slackWebhookUrl = "";
    /**
     * Dedicated Slack Incoming Webhook for claim events.
     */
    private String claimsSlackWebhookUrl = "";
    /**
     * Optional Slack Bot token used for chat.postMessage.
     */
    private String slackBotToken = "";
    /**
     * Target Slack channel ID for endorsement-completed notifications.
     */
    private String slackChannelId = "C0B22DB8AUC";

    private int retryMaxAttempts = 5;

    private long retryInitialDelayMs = 60_000L;

    private long retryMaxDelayMs = 3_600_000L;

    private int dispatchBatchSize = 50;

    /**
     * When true, HR routing for org-scoped events also includes active {@code HR_ADMIN} users with no
     * {@code organization_id} (legacy/local accounts). Default false for multi-tenant safety; enable on localhost
     * via {@code application-local.properties} when HR rows are not org-linked.
     */
    private boolean includeUnscopedHrAdmins = false;
}
