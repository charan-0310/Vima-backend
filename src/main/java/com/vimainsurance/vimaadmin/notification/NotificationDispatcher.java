package com.vimainsurance.vimaadmin.notification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EmailResponse;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.notification.config.EngineeringTestSlackWebhookOverrides;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.entity.NotificationDelivery;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationChannelKind;
import com.vimainsurance.vimaadmin.notification.enums.NotificationDeliveryStatus;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.notification.repository.IAdminNotificationRepository;
import com.vimainsurance.vimaadmin.notification.repository.INotificationDeliveryRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {
    private static final Set<NotificationCategory> TEMP_EMAIL_DISABLED_FOR_VIMA_ADMIN_CATEGORIES = Set.of(
            NotificationCategory.ENDORSEMENT,
            NotificationCategory.ENROLLMENT,
            NotificationCategory.CLAIM);

    private final IAdminNotificationRepository notificationRepository;
    private final INotificationDeliveryRepository deliveryRepository;
    private final NotificationsFeatureGate notificationsFeatureGate;
    private final NotificationsProperties notificationsProperties;
    private final Environment environment;
    private final IEmailService emailService;
    private final NotificationSlackWebhookClient slackWebhookClient;

    @Transactional
    public void dispatchDeliveriesFor(UUID notificationId) {
        log.info("notification_dispatch_start notificationId={}", notificationId);
        if (!notificationsFeatureGate.isNotificationsEnabled()) {
            log.info("notification_dispatch_skip reason=flag_disabled notificationId={}", notificationId);
            return;
        }
        AdminNotification n = notificationRepository.findByIdForDispatch(notificationId).orElse(null);
        if (n == null) {
            log.info("notification_dispatch_skip reason=notification_not_found notificationId={}", notificationId);
            return;
        }
        ensureRequiredDeliveries(n);
        log.info("notification_dispatch_found notificationId={} deliveries={}", notificationId, n.getDeliveries().size());
        for (NotificationDelivery d : n.getDeliveries()) {
            if (d.getStatus() == NotificationDeliveryStatus.SENT || d.getStatus() == NotificationDeliveryStatus.SKIPPED) {
                continue;
            }
            if (d.getStatus() == NotificationDeliveryStatus.FAILED
                    && d.getAttemptCount() >= notificationsProperties.getRetryMaxAttempts()) {
                continue;
            }
            if (d.getNextRetryAt() != null && d.getNextRetryAt().isAfter(LocalDateTime.now())) {
                continue;
            }
            processDelivery(n, d);
        }
    }

    @Transactional
    public int dispatchDueBatch() {
        if (!notificationsFeatureGate.isNotificationsEnabled()) {
            return 0;
        }
        List<NotificationDelivery> due = deliveryRepository.findDueForDispatch(
                Set.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.FAILED),
                LocalDateTime.now(),
                notificationsProperties.getRetryMaxAttempts(),
                PageRequest.of(0, notificationsProperties.getDispatchBatchSize()));
        int n = 0;
        for (NotificationDelivery d : due) {
            AdminNotification notif = d.getNotification();
            if (notif == null) {
                continue;
            }
            processDelivery(notif, d);
            n++;
        }
        return n;
    }

    private void processDelivery(AdminNotification n, NotificationDelivery d) {
        d.setAttemptCount(d.getAttemptCount() + 1);
        d.setLastAttemptAt(LocalDateTime.now());
        try {
            if (d.getChannel() == NotificationChannelKind.EMAIL) {
                sendEmail(n, d);
            } else if (d.getChannel() == NotificationChannelKind.SLACK) {
                sendSlack(n, d);
            }
        } catch (Exception ex) {
            markFailed(d, ex.getMessage());
        }
        deliveryRepository.save(d);
    }

    private void ensureRequiredDeliveries(AdminNotification n) {
        List<NotificationDelivery> deliveries = n.getDeliveries();
        if (deliveries == null) {
            deliveries = new ArrayList<>();
            n.setDeliveries(deliveries);
        }
        boolean hasEmail = false;
        for (NotificationDelivery d : deliveries) {
            if (d.getChannel() == NotificationChannelKind.EMAIL) {
                hasEmail = true;
                break;
            }
        }
        if (!hasEmail) {
            NotificationDelivery email = new NotificationDelivery();
            email.setNotification(n);
            email.setChannel(NotificationChannelKind.EMAIL);
            email.setStatus(NotificationDeliveryStatus.PENDING);
            applyDeliverySnapshot(email, n);
            // Must use the same collection instance for loaded entities (Hibernate orphanRemoval bag).
            deliveries.add(email);
        }
        // Do not auto-add SLACK: NotificationServiceImpl omits it intentionally for fan-out recipients
        // (single shared webhook). Re-injecting SLACK here caused duplicate channel posts.
        if (n.getId() != null) {
            notificationRepository.save(n);
        }
    }

    private void sendEmail(AdminNotification n, NotificationDelivery d) {
        AdminUser recipient = n.getRecipient();
        if (shouldTemporarilySkipEmailForVimaAdmin(recipient, n)) {
            d.setStatus(NotificationDeliveryStatus.SKIPPED);
            d.setLastError("Email temporarily disabled for VIMA_ADMIN on this event");
            log.info("notification_delivery_skipped channel=EMAIL notificationId={} reason=vima_admin_temp_email_disabled eventType={}",
                    n.getId(), n.getEventType());
            return;
        }
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            d.setStatus(NotificationDeliveryStatus.SKIPPED);
            d.setLastError("Recipient email missing");
            log.info("notification_delivery_skipped channel=EMAIL notificationId={} reason=no_email", n.getId());
            return;
        }
        String template = n.getEmailTemplate();
        if (template == null || template.isBlank()) {
            d.setStatus(NotificationDeliveryStatus.FAILED);
            d.setLastError("email_template missing");
            return;
        }
        Map<String, Object> vars = n.getEmailTemplateVars() != null ? new HashMap<>(n.getEmailTemplateVars()) : new HashMap<>();
        vars.putIfAbsent("title", n.getTitle());
        vars.putIfAbsent("bodyText", n.getBody());
        vars.putIfAbsent("deepLinkUrl", n.getDeepLinkUrl());

        EmailRequest req = EmailRequest.builder()
                .to(recipient.getEmail().trim())
                .subject(n.getEmailSubject() != null ? n.getEmailSubject() : n.getTitle())
                .templateName(template)
                .templateVariables(vars)
                .build();
        EmailResponse resp = emailService.sendTemplateEmail(req);
        if (resp != null && resp.isSuccess()) {
            d.setStatus(NotificationDeliveryStatus.SENT);
            d.setLastError(null);
            log.info("notification_delivery_sent channel=EMAIL notificationId={} deliveryId={}", n.getId(), d.getId());
        } else {
            markFailed(d, resp != null ? resp.getError() : "email send failed");
            log.warn("notification_delivery_failed channel=EMAIL notificationId={} deliveryId={}", n.getId(), d.getId());
        }
    }

    private void sendSlack(AdminNotification n, NotificationDelivery d) {
        String text = (n.getTitle() != null ? "*" + n.getTitle() + "*\n" : "") + (n.getBody() != null ? n.getBody() : "");
        if (n.getDeepLinkUrl() != null && !n.getDeepLinkUrl().isBlank()) {
            text = text + "\n" + n.getDeepLinkUrl();
        }
        boolean ok = false;
        String route = "none";
        // Prefer notifications.* first so repo-controlled non-prod webhooks win over stray
        // SLACK_REMINDER_CHANNEL_URL / SLACK_WEBHOOK_URL from the host environment.
        // When vima.slack.enforce-engineering-test-webhooks=true, only NotificationsProperties URLs
        // are used (pinned at startup) — host env cannot override.
        String url;
        if (slackWebhooksPinnedToEngineeringTest()) {
            if (isClaimEvent(n.getEventType())) {
                url = firstNonBlank(
                        notificationsProperties.getClaimsSlackWebhookUrl(),
                        notificationsProperties.getSlackWebhookUrl());
            } else {
                url = firstNonBlank(notificationsProperties.getSlackWebhookUrl());
            }
        } else if (isClaimEvent(n.getEventType())) {
            url = firstNonBlank(
                    notificationsProperties.getClaimsSlackWebhookUrl(),
                    environment.getProperty("slack.webhook.url", ""));
        } else if (NotificationEventType.ENDORSEMENT_UPLOADED.equals(n.getEventType())) {
            url = firstNonBlank(
                    notificationsProperties.getSlackWebhookUrl(),
                    environment.getProperty("slack.reminder.channel.url", ""),
                    environment.getProperty("slack.webhook.url", ""));
        } else {
            url = firstNonBlank(
                    notificationsProperties.getSlackWebhookUrl(),
                    environment.getProperty("slack.webhook.url", ""));
        }
        if (url == null || url.isBlank()) {
            d.setStatus(NotificationDeliveryStatus.SKIPPED);
            d.setLastError("Slack Incoming Webhook not configured (notifications.slack-webhook-url / claims / slack.webhook.url)");
            log.info("notification_delivery_skipped channel=SLACK notificationId={} reason=no_webhook", n.getId());
            return;
        }
        route = "webhook";
        log.info("notification_delivery_route channel=SLACK route=webhook notificationId={} deliveryId={} eventType={}",
                n.getId(), d.getId(), n.getEventType());
        ok = slackWebhookClient.postMessageToWebhookUrl(text, url);
        if (ok) {
            d.setStatus(NotificationDeliveryStatus.SENT);
            d.setLastError(null);
            log.info("notification_delivery_sent channel=SLACK notificationId={} deliveryId={} route={}", n.getId(), d.getId(), route);
        } else {
            markFailed(d, "Slack webhook rejected or failed");
            log.warn("notification_delivery_failed channel=SLACK notificationId={} deliveryId={} route={}", n.getId(), d.getId(), route);
        }
    }

    private void markFailed(NotificationDelivery d, String message) {
        d.setStatus(NotificationDeliveryStatus.FAILED);
        d.setLastError(message != null && message.length() > 2000 ? message.substring(0, 2000) : message);
        long delayMs = Math.min(
                notificationsProperties.getRetryMaxDelayMs(),
                notificationsProperties.getRetryInitialDelayMs() * (1L << Math.min(d.getAttemptCount(), 10)));
        d.setNextRetryAt(LocalDateTime.now().plus(Duration.ofMillis(delayMs)));
    }

    private boolean slackWebhooksPinnedToEngineeringTest() {
        return Boolean.parseBoolean(
                environment.getProperty(EngineeringTestSlackWebhookOverrides.ENFORCE_PROPERTY, "false"));
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c.trim();
            }
        }
        return "";
    }

    private static boolean isClaimEvent(NotificationEventType eventType) {
        if (eventType == null) {
            return false;
        }
        return switch (eventType) {
            case EMPLOYEE_CLAIM_SUBMITTED,
                    EMPLOYEE_CLAIM_QUERY_RAISED,
                    EMPLOYEE_CLAIM_QUERY_RESPONDED,
                    EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED,
                    EMPLOYEE_CLAIM_APPROVED,
                    EMPLOYEE_CLAIM_REJECTED,
                    EMPLOYEE_CLAIM_SETTLED -> true;
            default -> false;
        };
    }

    private static boolean shouldTemporarilySkipEmailForVimaAdmin(AdminUser recipient, AdminNotification notification) {
        if (recipient == null || notification == null || notification.getCategory() == null) {
            return false;
        }
        if (!TEMP_EMAIL_DISABLED_FOR_VIMA_ADMIN_CATEGORIES.contains(notification.getCategory())) {
            return false;
        }
        String normalizedRole = normalizeRole(recipient.getRole());
        return "VIMA_ADMIN".equals(normalizedRole);
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        normalized = normalized.replace('-', '_').replace(' ', '_');
        while (normalized.contains("__")) {
            normalized = normalized.replace("__", "_");
        }
        if (normalized.endsWith("_GROUP")) {
            normalized = normalized.substring(0, normalized.length() - "_GROUP".length());
        }
        return normalized;
    }

    private static void applyDeliverySnapshot(NotificationDelivery delivery, AdminNotification notification) {
        if (delivery == null || notification == null) {
            return;
        }
        delivery.setReceiverEmail(notification.getReceiverEmail());
        delivery.setReceiverName(notification.getReceiverName());
        delivery.setReceiverRole(notification.getReceiverRole());
        delivery.setCreatorEmail(notification.getCreatorEmail());
        delivery.setCreatorName(notification.getCreatorName());
        delivery.setCreatorRole(notification.getCreatorRole());
    }
}
