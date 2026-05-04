package com.vimainsurance.vimaadmin.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.notification.dto.CreateNotificationCommand;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.notification.enums.NotificationSeverity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlagshipNotificationService {

    private final NotificationRoutingResolver routingResolver;
    private final NotificationService notificationService;
    private final NotificationDispatcher notificationDispatcher;
    private final AfterCommitNotificationRunner afterCommitNotificationRunner;

    @Value("${app.portal-url:}")
    private String portalUrlOverride;

    @Value("${app.base-url:http://localhost:7219}")
    private String appBaseUrl;

    public void scheduleEndorsementUploaded(Endorsement endorsement, Organization organization, AdminUser uploadedBy) {
        if (endorsement == null || organization == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(() -> emitEndorsementUploaded(endorsement, organization, uploadedBy));
    }

    public void scheduleEnrollmentAllSubmitted(UUID organizationId, UUID windowId, String organizationDisplayName) {
        if (organizationId == null || windowId == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEnrollmentAllSubmitted(organizationId, windowId, organizationDisplayName));
    }

    public void scheduleEndorsementCompleted(UUID endorsementId, Organization organization) {
        if (endorsementId == null || organization == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(() -> emitEndorsementCompleted(endorsementId, organization));
    }

    private String portalBase() {
        if (portalUrlOverride != null && !portalUrlOverride.isBlank()) {
            return portalUrlOverride.trim().replaceAll("/$", "");
        }
        return appBaseUrl != null ? appBaseUrl.trim().replaceAll("/$", "") : "";
    }

    private void emitEndorsementUploaded(Endorsement endorsement, Organization organization, AdminUser uploadedBy) {
        List<AdminUser> recipients = routingResolver.resolveRecipients(NotificationEventType.ENDORSEMENT_UPLOADED, null);
        String orgName = organization.getOrganizationName();
        String uploader = uploadedBy != null && uploadedBy.getFullName() != null ? uploadedBy.getFullName() : "HR";
        String deepLink = portalBase() + "/endorsements/" + endorsement.getEndorsementId();
        for (AdminUser admin : recipients) {
            String dedup = "ENDORSEMENT_UPLOADED:" + endorsement.getEndorsementId() + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", "Endorsement uploaded — " + orgName);
            vars.put("organizationName", orgName);
            vars.put("uploadedByName", uploader);
            vars.put("endorsementType", endorsement.getEndorsementType() != null ? endorsement.getEndorsementType().name() : "");
            vars.put("deepLinkUrl", deepLink);
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organization.getOrganizationId(),
                    NotificationEventType.ENDORSEMENT_UPLOADED,
                    NotificationCategory.ENDORSEMENT,
                    NotificationSeverity.INFO,
                    "New endorsement upload — " + orgName,
                    uploader + " uploaded an endorsement batch for " + orgName + ".",
                    deepLink,
                    dedup,
                    "Endorsement uploaded — " + orgName,
                    "email/notification-endorsement-uploaded",
                    vars);
            notificationService.createIfAbsent(cmd).ifPresentOrElse(
                    id -> {
                        notificationDispatcher.dispatchDeliveriesFor(id);
                        log.info("flagship_notification_emit event=ENDORSEMENT_UPLOADED notificationId={} dedupKey={}", id, dedup);
                    },
                    () -> log.debug("flagship_notification_dedup event=ENDORSEMENT_UPLOADED dedupKey={}", dedup));
        }
    }

    private void emitEnrollmentAllSubmitted(UUID organizationId, UUID windowId, String organizationDisplayName) {
        List<AdminUser> recipients = routingResolver.resolveRecipients(
                NotificationEventType.ENROLLMENT_ALL_SUBMITTED, organizationId);
        if (recipients.isEmpty()) {
            log.info("flagship_notification_skip event=ENROLLMENT_ALL_SUBMITTED reason=no_hr_admins orgId={}", organizationId);
            return;
        }
        String orgName = organizationDisplayName != null && !organizationDisplayName.isBlank()
                ? organizationDisplayName
                : "Your organization";
        String deepLink = portalBase() + "/group/" + organizationId + "/enrollment-windows/" + windowId;
        for (AdminUser admin : recipients) {
            String dedup = "ENROLLMENT_ALL_SUBMITTED:" + windowId + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", "Self-enrollments submitted — " + orgName);
            vars.put("organizationName", orgName);
            vars.put("deepLinkUrl", deepLink);
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organizationId,
                    NotificationEventType.ENROLLMENT_ALL_SUBMITTED,
                    NotificationCategory.ENROLLMENT,
                    NotificationSeverity.INFO,
                    "Enrollments ready for HR review — " + orgName,
                    "All employees in the current enrollment window have submitted. Please review in Vima.",
                    deepLink,
                    dedup,
                    "Self-enrollments submitted — " + orgName,
                    "email/notification-enrollment-all-submitted",
                    vars);
            notificationService.createIfAbsent(cmd).ifPresentOrElse(
                    id -> {
                        notificationDispatcher.dispatchDeliveriesFor(id);
                        log.info("flagship_notification_emit event=ENROLLMENT_ALL_SUBMITTED notificationId={} dedupKey={}", id, dedup);
                    },
                    () -> log.debug("flagship_notification_dedup event=ENROLLMENT_ALL_SUBMITTED dedupKey={}", dedup));
        }
    }

    private void emitEndorsementCompleted(UUID endorsementId, Organization organization) {
        List<AdminUser> recipients = routingResolver.resolveRecipients(
                NotificationEventType.ENDORSEMENT_COMPLETED, organization.getOrganizationId());
        String orgName = organization.getOrganizationName();
        String deepLink = portalBase() + "/endorsements/" + endorsementId;
        for (AdminUser admin : recipients) {
            String dedup = "ENDORSEMENT_COMPLETED:" + endorsementId + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", "Endorsement completed — " + orgName);
            vars.put("organizationName", orgName);
            vars.put("deepLinkUrl", deepLink);
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organization.getOrganizationId(),
                    NotificationEventType.ENDORSEMENT_COMPLETED,
                    NotificationCategory.ENDORSEMENT,
                    NotificationSeverity.INFO,
                    "Endorsement completed — " + orgName,
                    "An endorsement batch is complete and employees are active. Summary is available in Vima.",
                    deepLink,
                    dedup,
                    "Endorsement completed — " + orgName,
                    "email/notification-endorsement-completed",
                    vars);
            notificationService.createIfAbsent(cmd).ifPresentOrElse(
                    id -> {
                        notificationDispatcher.dispatchDeliveriesFor(id);
                        log.info("flagship_notification_emit event=ENDORSEMENT_COMPLETED notificationId={} dedupKey={}", id, dedup);
                    },
                    () -> log.debug("flagship_notification_dedup event=ENDORSEMENT_COMPLETED dedupKey={}", dedup));
        }
    }
}
