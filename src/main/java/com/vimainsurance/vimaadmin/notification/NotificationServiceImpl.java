package com.vimainsurance.vimaadmin.notification;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.notification.dto.CreateNotificationCommand;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.entity.NotificationDelivery;
import com.vimainsurance.vimaadmin.notification.enums.NotificationChannelKind;
import com.vimainsurance.vimaadmin.notification.enums.NotificationDeliveryStatus;
import com.vimainsurance.vimaadmin.notification.repository.IAdminNotificationRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final IAdminNotificationRepository notificationRepository;
    private final IAdminUserRepository adminUserRepository;
    private final IOrganizationRepository organizationRepository;
    private final NotificationsFeatureGate notificationsFeatureGate;

    @Override
    @Transactional
    public Optional<UUID> createIfAbsent(CreateNotificationCommand command) {
        log.info("notification_create_attempt event={} recipientId={} companyId={} dedupKey={}",
                command.eventType(), command.recipientId(), command.companyId(), command.dedupKey());
        if (!notificationsFeatureGate.isNotificationsEnabled()) {
            log.info("notification_create_skip reason=flag_disabled flag={}", NotificationsFeatureGate.FLAG_KEY);
            return Optional.empty();
        }
        if (notificationRepository.existsByDedupKey(command.dedupKey())) {
            log.info("notification_create_skip reason=dedup_exists dedupKey={}", command.dedupKey());
            return Optional.empty();
        }
        UUID recipientId = command.recipientId();
        AdminUser recipient = recipientId != null ? adminUserRepository.findById(recipientId).orElse(null) : null;
        if (recipient == null) {
            log.warn("notification_create_skip reason=recipient_not_found recipientId={}", command.recipientId());
            return Optional.empty();
        }
        Organization company = null;
        UUID companyId = command.companyId();
        if (companyId != null) {
            company = organizationRepository.findById(companyId).orElse(null);
            if (company == null && organizationRepository.existsById(companyId)) {
                company = organizationRepository.getReferenceById(companyId);
                log.warn("notification_create_org_reference_only companyId={} recipientId={}", companyId, recipientId);
            }
            if (company == null) {
                log.warn("notification_create_missing_org companyId={} recipientId={} event={}",
                        companyId, recipientId, command.eventType());
            }
        }
        AdminNotification n = new AdminNotification();
        n.setRecipient(recipient);
        n.setCompany(company);
        n.setEventType(command.eventType());
        n.setCategory(command.category());
        n.setSeverity(command.severity());
        n.setTitle(command.title());
        n.setBody(command.body());
        n.setEmailSubject(command.emailSubject());
        n.setEmailTemplate(command.emailTemplateName());
        n.setEmailTemplateVars(command.templateVariables());
        n.setDeepLinkUrl(command.deepLinkUrl());
        n.setDedupKey(command.dedupKey());
        n.setReceiverEmail(recipient.getEmail());
        n.setReceiverName(resolveReceiverName(recipient));
        n.setReceiverRole(recipient.getRole());
        n.setCreatorEmail(resolveCreatorEmail(command.templateVariables()));
        n.setCreatorName(resolveCreatorName(command.templateVariables()));
        n.setCreatorRole(resolveCreatorRole(command.templateVariables()));

        NotificationDelivery email = new NotificationDelivery();
        email.setNotification(n);
        email.setChannel(NotificationChannelKind.EMAIL);
        email.setStatus(NotificationDeliveryStatus.PENDING);
        applyDeliverySnapshot(email, n);
        n.getDeliveries().add(email);

        boolean includeSlack = command.slackDeliveryEnabled() == null || Boolean.TRUE.equals(command.slackDeliveryEnabled());
        if (includeSlack) {
            NotificationDelivery slack = new NotificationDelivery();
            slack.setNotification(n);
            slack.setChannel(NotificationChannelKind.SLACK);
            slack.setStatus(NotificationDeliveryStatus.PENDING);
            applyDeliverySnapshot(slack, n);
            n.getDeliveries().add(slack);
        }

        try {
            AdminNotification saved = notificationRepository.saveAndFlush(n);
            log.info("notification_create_success notificationId={} recipientId={} dedupKey={}",
                    saved.getId(), command.recipientId(), command.dedupKey());
            return Optional.of(saved.getId());
        } catch (DataIntegrityViolationException ex) {
            log.info("notification_create_skip reason=dedup_race dedupKey={} message={}", command.dedupKey(), ex.getMessage());
            return Optional.empty();
        }
    }

    private static String resolveReceiverName(AdminUser recipient) {
        if (recipient == null) {
            return null;
        }
        if (recipient.getFullName() != null && !recipient.getFullName().isBlank()) {
            return recipient.getFullName().trim();
        }
        if (recipient.getUsername() != null && !recipient.getUsername().isBlank()) {
            return recipient.getUsername().trim();
        }
        return null;
    }

    private static String resolveCreatorName(Map<String, Object> vars) {
        if (vars == null) {
            return null;
        }
        return firstNonBlank(
                vars.get("creatorName"),
                vars.get("actedByName"),
                vars.get("uploadedByName"),
                vars.get("from"));
    }

    private static String resolveCreatorEmail(Map<String, Object> vars) {
        if (vars == null) {
            return null;
        }
        return firstNonBlank(
                vars.get("creatorEmail"),
                vars.get("actedByEmail"),
                vars.get("uploadedByEmail"),
                vars.get("fromEmail"));
    }

    private static String resolveCreatorRole(Map<String, Object> vars) {
        if (vars == null) {
            return null;
        }
        return firstNonBlank(
                vars.get("creatorRole"),
                vars.get("actedByRole"),
                vars.get("uploadedByRole"),
                vars.get("actorRole"));
    }

    private static String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }
        return null;
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
