package com.vimainsurance.vimaadmin.notification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.entity.NotificationDelivery;
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
        // Tests (and some callers) may provide immutable lists (e.g., List.of(...)).
        // Always normalize to a mutable list before adding missing channel deliveries.
        List<NotificationDelivery> deliveries = n.getDeliveries() != null
                ? new ArrayList<>(n.getDeliveries())
                : new ArrayList<>();
        n.setDeliveries(deliveries);
        Set<NotificationChannelKind> existingChannels = new HashSet<>();
        for (NotificationDelivery d : deliveries) {
            if (d.getChannel() != null) {
                existingChannels.add(d.getChannel());
            }
        }
        if (!existingChannels.contains(NotificationChannelKind.EMAIL)) {
            NotificationDelivery email = new NotificationDelivery();
            email.setNotification(n);
            email.setChannel(NotificationChannelKind.EMAIL);
            email.setStatus(NotificationDeliveryStatus.PENDING);
            deliveries.add(email);
        }
        if (!existingChannels.contains(NotificationChannelKind.SLACK)) {
            NotificationDelivery slack = new NotificationDelivery();
            slack.setNotification(n);
            slack.setChannel(NotificationChannelKind.SLACK);
            slack.setStatus(NotificationDeliveryStatus.PENDING);
            deliveries.add(slack);
        }
        if (n.getId() != null) {
            notificationRepository.save(n);
        }
    }

    private void sendEmail(AdminNotification n, NotificationDelivery d) {
        AdminUser recipient = n.getRecipient();
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
        String url;
        if (NotificationEventType.ENDORSEMENT_UPLOADED.equals(n.getEventType())) {
            url = environment.getProperty("slack.reminder.channel.url", "");
            if (url == null || url.isBlank()) {
                url = notificationsProperties.getSlackWebhookUrl();
            }
        } else {
            url = notificationsProperties.getSlackWebhookUrl();
        }
        if (url == null || url.isBlank()) {
            url = environment.getProperty("slack.webhook.url", "");
        }
        if (url == null || url.isBlank()) {
            d.setStatus(NotificationDeliveryStatus.SKIPPED);
            d.setLastError("notifications.slack-webhook-url (or slack.webhook.url) not configured");
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
}
