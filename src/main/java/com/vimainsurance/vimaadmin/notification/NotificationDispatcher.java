package com.vimainsurance.vimaadmin.notification;

import java.time.Duration;
import java.time.LocalDateTime;
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
import com.vimainsurance.vimaadmin.notification.enums.NotificationChannelKind;
import com.vimainsurance.vimaadmin.notification.enums.NotificationDeliveryStatus;
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
        String url = notificationsProperties.getSlackWebhookUrl();
        if (url == null || url.isBlank()) {
            d.setStatus(NotificationDeliveryStatus.SKIPPED);
            d.setLastError("notifications.slack-webhook-url not configured");
            log.info("notification_delivery_skipped channel=SLACK notificationId={} reason=no_webhook", n.getId());
            return;
        }
        String text = (n.getTitle() != null ? "*" + n.getTitle() + "*\n" : "") + (n.getBody() != null ? n.getBody() : "");
        if (n.getDeepLinkUrl() != null && !n.getDeepLinkUrl().isBlank()) {
            text = text + "\n" + n.getDeepLinkUrl();
        }
        boolean ok = slackWebhookClient.postMessage(text);
        if (ok) {
            d.setStatus(NotificationDeliveryStatus.SENT);
            d.setLastError(null);
            log.info("notification_delivery_sent channel=SLACK notificationId={} deliveryId={}", n.getId(), d.getId());
        } else {
            markFailed(d, "Slack webhook rejected or failed");
            log.warn("notification_delivery_failed channel=SLACK notificationId={} deliveryId={}", n.getId(), d.getId());
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
