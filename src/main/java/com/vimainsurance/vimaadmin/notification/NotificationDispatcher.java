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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EmailResponse;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.entity.NotificationDelivery;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationChannelKind;
import com.vimainsurance.vimaadmin.notification.enums.NotificationDeliveryStatus;
import com.vimainsurance.vimaadmin.notification.repository.IAdminNotificationRepository;
import com.vimainsurance.vimaadmin.notification.repository.INotificationDeliveryRepository;
import com.vimainsurance.vimaadmin.notification.slack.SlackChannel;
import com.vimainsurance.vimaadmin.notification.slack.SlackChannelRouter;
import com.vimainsurance.vimaadmin.service.IEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {
    /**
     * Categories where NOBODY receives email — Slack and in-app bell still fire.
     * CLAIM events are Slack-only by design: HR is notified via the support-claims channel and
     * VIMA staff get the same Slack post; we do not flood inboxes with claim emails.
     */
    private static final Set<NotificationCategory> EMAIL_DISABLED_FOR_ALL_CATEGORIES = Set.of(
            NotificationCategory.CLAIM);

    /**
     * Categories where VIMA platform admins (SUPER_ADMIN / ADMIN / VIMA_ADMIN / SALES_ADMIN)
     * should NOT receive email. HR admins still receive. Slack and in-app bell still fire for everyone.
     */
    private static final Set<NotificationCategory> EMAIL_DISABLED_FOR_VIMA_PLATFORM_CATEGORIES = Set.of(
            NotificationCategory.ENROLLMENT);

    private static final Set<String> VIMA_PLATFORM_ROLE_NAMES = Set.of(
            "SUPER_ADMIN", "ADMIN", "VIMA_ADMIN", "SALES_ADMIN");

    private final IAdminNotificationRepository notificationRepository;
    private final INotificationDeliveryRepository deliveryRepository;
    private final NotificationsFeatureGate notificationsFeatureGate;
    private final NotificationsProperties notificationsProperties;
    private final IEmailService emailService;
    private final NotificationSlackWebhookClient slackWebhookClient;
    private final SlackChannelRouter slackChannelRouter;

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
        if (n.getCategory() != null && EMAIL_DISABLED_FOR_ALL_CATEGORIES.contains(n.getCategory())) {
            d.setStatus(NotificationDeliveryStatus.SKIPPED);
            d.setLastError("Email disabled for category " + n.getCategory());
            log.info("notification_delivery_skipped channel=EMAIL notificationId={} reason=category_email_disabled category={} eventType={}",
                    n.getId(), n.getCategory(), n.getEventType());
            return;
        }
        if (shouldSkipEmailForVimaPlatformAdmin(recipient, n)) {
            d.setStatus(NotificationDeliveryStatus.SKIPPED);
            d.setLastError("Email disabled for VIMA platform admin on this category");
            log.info("notification_delivery_skipped channel=EMAIL notificationId={} reason=vima_platform_admin_email_disabled eventType={} category={}",
                    n.getId(), n.getEventType(), n.getCategory());
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

        SlackChannel channel = slackChannelRouter.channelForEvent(n.getEventType());
        var maybeUrl = slackChannelRouter.resolveUrl(channel);
        if (maybeUrl.isEmpty()) {
            d.setStatus(NotificationDeliveryStatus.SKIPPED);
            d.setLastError("Slack channel " + channel + " not configured for this environment");
            log.info("notification_delivery_skipped channel=SLACK notificationId={} reason=router_skipped slackChannel={} eventType={}",
                    n.getId(), channel, n.getEventType());
            return;
        }

        log.info("notification_delivery_route channel=SLACK route=webhook notificationId={} deliveryId={} eventType={} slackChannel={}",
                n.getId(), d.getId(), n.getEventType(), channel);
        boolean ok = slackWebhookClient.postMessageToWebhookUrl(text, maybeUrl.get());
        if (ok) {
            d.setStatus(NotificationDeliveryStatus.SENT);
            d.setLastError(null);
            log.info("notification_delivery_sent channel=SLACK notificationId={} deliveryId={} slackChannel={}",
                    n.getId(), d.getId(), channel);
        } else {
            markFailed(d, "Slack webhook rejected or failed");
            log.warn("notification_delivery_failed channel=SLACK notificationId={} deliveryId={} slackChannel={}",
                    n.getId(), d.getId(), channel);
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

    private static boolean shouldSkipEmailForVimaPlatformAdmin(AdminUser recipient, AdminNotification notification) {
        if (recipient == null || notification == null || notification.getCategory() == null) {
            return false;
        }
        if (!EMAIL_DISABLED_FOR_VIMA_PLATFORM_CATEGORIES.contains(notification.getCategory())) {
            return false;
        }
        return VIMA_PLATFORM_ROLE_NAMES.contains(normalizeRole(recipient.getRole()));
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
