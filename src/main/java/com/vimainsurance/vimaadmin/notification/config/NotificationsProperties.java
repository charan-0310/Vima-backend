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

    private int retryMaxAttempts = 5;

    private long retryInitialDelayMs = 60_000L;

    private long retryMaxDelayMs = 3_600_000L;

    private int dispatchBatchSize = 50;
}
