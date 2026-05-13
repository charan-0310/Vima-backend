package com.vimainsurance.vimaadmin.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Non-production deployments often inherit {@code NOTIFICATIONS_*}/{@code SLACK_*} secrets from
 * the host or orchestrator. Those override {@code application-*.properties} and can send Slack to
 * production webhooks. When {@code vima.slack.enforce-engineering-test-webhooks=true}, unified
 * notification Slack URLs are pinned after binding to {@link #ENGINEERING_TEST_URL_PROPERTY}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vima.slack.enforce-engineering-test-webhooks", havingValue = "true")
public class EngineeringTestSlackWebhookOverrides {

    /** Property name duplicated on {@link ConditionalOnProperty#name()}. */
    public static final String ENFORCE_PROPERTY = "vima.slack.enforce-engineering-test-webhooks";

    static final String ENGINEERING_TEST_URL_PROPERTY = "vima.slack.engineering-test-webhook-url";

    private final NotificationsProperties notificationsProperties;
    private final Environment environment;

    @PostConstruct
    public void applyEngineeringTestSlackWebhooks() {
        String url = environment.getProperty(ENGINEERING_TEST_URL_PROPERTY, "").trim();
        if (url.isBlank()) {
            log.warn(
                    "vima.slack.enforce-engineering-test-webhooks=true but {} is blank; Slack URL override skipped",
                    ENGINEERING_TEST_URL_PROPERTY);
            return;
        }
        notificationsProperties.setSlackWebhookUrl(url);
        notificationsProperties.setClaimsSlackWebhookUrl(url);
        log.info(
                "Unified Slack webhooks (notifications.slack-webhook-url / claims) pinned to engineering test channel for this profile");
    }
}
