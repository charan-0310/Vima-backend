package com.vimainsurance.vimaadmin.notification.dto;

import java.util.Map;
import java.util.UUID;

import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.notification.enums.NotificationSeverity;

public record CreateNotificationCommand(
        UUID recipientId,
        UUID companyId,
        NotificationEventType eventType,
        NotificationCategory category,
        NotificationSeverity severity,
        String title,
        String body,
        String deepLinkUrl,
        String dedupKey,
        String emailSubject,
        String emailTemplateName,
        Map<String, Object> templateVariables,
        /**
         * When {@code null} or {@code true}, a SLACK {@code notification_deliveries} row is created.
         * When {@code false}, only in-app + email deliveries are created (avoids duplicate Slack when the same
         * logical event is fanned out to multiple recipients).
         */
        Boolean slackDeliveryEnabled
) {
}
