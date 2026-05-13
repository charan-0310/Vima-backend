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
    private String slackChannelId = "";

    private int retryMaxAttempts = 5;

    private long retryInitialDelayMs = 60_000L;

    private long retryMaxDelayMs = 3_600_000L;

    private int dispatchBatchSize = 50;

    /**
     * Ignored for recipient resolution: org-scoped notifications only target HR users with
     * {@code admin_users.organization_id} matching the event organization (multi-tenant safe).
     * Retained for backward-compatible YAML keys only.
     *
     * @deprecated No longer applied; ensure HR admins have {@code organization_id} set in provisioning/Keycloak sync.
     */
    @Deprecated
    private boolean includeUnscopedHrAdmins = false;

    /**
     * When true and no HR admins are linked to the event organization, recipient resolution falls back to
     * all active {@code HR_ADMIN} users (legacy, cross-tenant). Default {@code false}; do not enable in production.
     */
    private boolean allowGlobalHrRecipientFallback = false;
}
