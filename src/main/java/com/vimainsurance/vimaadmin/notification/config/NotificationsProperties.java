package com.vimainsurance.vimaadmin.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Behaviour switches for the unified notification system. Slack URL configuration
 * lives in {@link com.vimainsurance.vimaadmin.notification.slack.SlackChannelProperties}
 * and routing decisions go through
 * {@link com.vimainsurance.vimaadmin.notification.slack.SlackChannelRouter}; do not
 * add Slack URLs here.
 */
@Data
@ConfigurationProperties(prefix = "notifications")
public class NotificationsProperties {

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
