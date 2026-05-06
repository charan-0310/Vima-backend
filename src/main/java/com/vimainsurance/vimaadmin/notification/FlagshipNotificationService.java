package com.vimainsurance.vimaadmin.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.DealEndorsement;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.mapper.EmployeeToDeals;
import com.vimainsurance.vimaadmin.notification.dto.CreateNotificationCommand;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.notification.enums.NotificationSeverity;
import com.vimainsurance.vimaadmin.repository.IDealEndorsementRepository;

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
    private final IDealEndorsementRepository dealEndorsementRepository;

    @Value("${app.portal-url:}")
    private String portalUrlOverride;

    @Value("${app.base-url:http://localhost:7219}")
    private String appBaseUrl;

    private static final int MAX_SELF_EMPLOYEE_IDS_IN_COPY = 20;

    public void scheduleEndorsementUploaded(Endorsement endorsement, Organization organization, AdminUser uploadedBy) {
        scheduleEndorsementUploaded(endorsement, organization, uploadedBy, List.of());
    }

    public void scheduleEndorsementUploaded(
            Endorsement endorsement,
            Organization organization,
            AdminUser uploadedBy,
            List<String> selfEmployeeIds) {
        if (endorsement == null || organization == null) {
            log.info("flagship_notification_skip event=ENDORSEMENT_UPLOADED reason=missing_context endorsementNull={} orgNull={}",
                    endorsement == null, organization == null);
            return;
        }
        List<String> ids = selfEmployeeIds != null ? new ArrayList<>(selfEmployeeIds) : List.of();
        log.info("flagship_notification_schedule event=ENDORSEMENT_UPLOADED endorsementId={} orgId={} uploadedBy={} selfEmployeeIdCount={}",
                endorsement.getEndorsementId(), organization.getOrganizationId(), uploadedBy != null ? uploadedBy.getId() : null, ids.size());
        afterCommitNotificationRunner.runAsyncAfterCommit(() -> emitEndorsementUploaded(endorsement, organization, uploadedBy, ids));
    }

    public void scheduleEnrollmentAllSubmitted(UUID organizationId, UUID windowId, String organizationDisplayName) {
        if (organizationId == null || windowId == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEnrollmentAllSubmitted(organizationId, windowId, organizationDisplayName));
    }

    public void scheduleEndorsementCompleted(UUID endorsementId, Organization organization, UUID uploadedByAdminUserId) {
        scheduleEndorsementCompleted(endorsementId, organization, uploadedByAdminUserId, null, null);
    }

    public void scheduleEndorsementCompleted(
            UUID endorsementId,
            Organization organization,
            UUID uploadedByAdminUserId,
            String actedByName,
            String displayOrganizationName) {
        if (endorsementId == null || organization == null) {
            log.info("flagship_notification_skip event=ENDORSEMENT_COMPLETED reason=missing_context endorsementId={} orgNull={}",
                    endorsementId, organization == null);
            return;
        }
        log.info("flagship_notification_schedule event=ENDORSEMENT_COMPLETED endorsementId={} orgId={} uploadedBy={}",
                endorsementId, organization.getOrganizationId(), uploadedByAdminUserId);
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEndorsementCompleted(endorsementId, organization, uploadedByAdminUserId, actedByName, displayOrganizationName));
    }

    private String portalBase() {
        if (portalUrlOverride != null && !portalUrlOverride.isBlank()) {
            return portalUrlOverride.trim().replaceAll("/$", "");
        }
        return appBaseUrl != null ? appBaseUrl.trim().replaceAll("/$", "") : "";
    }

    private void emitEndorsementUploaded(
            Endorsement endorsement,
            Organization organization,
            AdminUser uploadedBy,
            List<String> selfEmployeeIds) {
        log.info("flagship_notification_emit_start event=ENDORSEMENT_UPLOADED endorsementId={} orgId={}",
                endorsement.getEndorsementId(), organization.getOrganizationId());
        List<AdminUser> recipients = routingResolver.resolveRecipients(NotificationEventType.ENDORSEMENT_UPLOADED, null);
        if (recipients.isEmpty()) {
            log.info("flagship_notification_skip event=ENDORSEMENT_UPLOADED reason=no_vima_recipients endorsementId={}",
                    endorsement.getEndorsementId());
            return;
        }
        String orgName = organization.getOrganizationName();
        String uploader = uploadedBy != null && uploadedBy.getFullName() != null ? uploadedBy.getFullName() : "HR";
        String deepLink = portalBase() + "/endorsements/" + endorsement.getEndorsementId();
        String bodyText = buildEndorsementUploadedBody(selfEmployeeIds);
        int shownCount = Math.min(MAX_SELF_EMPLOYEE_IDS_IN_COPY, selfEmployeeIds.size());
        int remainingAfterCap = Math.max(0, selfEmployeeIds.size() - shownCount);
        String displayedIdsCsv = selfEmployeeIds.isEmpty()
                ? ""
                : String.join(", ", selfEmployeeIds.subList(0, shownCount));
        for (AdminUser admin : recipients) {
            String dedup = "ENDORSEMENT_UPLOADED:" + endorsement.getEndorsementId() + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", "Endorsement uploaded — " + orgName);
            vars.put("organizationName", orgName);
            vars.put("uploadedByName", uploader);
            vars.put("endorsementType", endorsement.getEndorsementType() != null ? endorsement.getEndorsementType().name() : "");
            vars.put("deepLinkUrl", deepLink);
            vars.put("hasSelfEmployeeIds", !selfEmployeeIds.isEmpty());
            vars.put("selfEmployeeIdsText", displayedIdsCsv);
            vars.put("selfEmployeeIdsRemainingCount", remainingAfterCap);
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organization.getOrganizationId(),
                    NotificationEventType.ENDORSEMENT_UPLOADED,
                    NotificationCategory.ENDORSEMENT,
                    NotificationSeverity.INFO,
                    "New endorsement upload — " + orgName,
                    bodyText,
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

    /**
     * In-app/Slack body and email {@code bodyText} fallback. Lists primary (Self) employee IDs from the upload sheet, capped for readability.
     */
    private String buildEndorsementUploadedBody(List<String> selfEmployeeIds) {
        if (selfEmployeeIds == null || selfEmployeeIds.isEmpty()) {
            return "An endorsement batch was uploaded successfully.";
        }
        int cap = Math.min(MAX_SELF_EMPLOYEE_IDS_IN_COPY, selfEmployeeIds.size());
        List<String> shown = selfEmployeeIds.subList(0, cap);
        int remaining = selfEmployeeIds.size() - cap;
        String joined = String.join(", ", shown);
        StringBuilder sb = new StringBuilder("Uploaded employees (Self): ").append(joined);
        if (remaining > 0) {
            sb.append(" (and ").append(remaining).append(" more).");
        } else {
            sb.append(".");
        }
        return sb.toString();
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

    private void emitEndorsementCompleted(
            UUID endorsementId,
            Organization organization,
            UUID uploadedByAdminUserId,
            String actedByName,
            String displayOrganizationName) {
        log.info("flagship_notification_emit_start event=ENDORSEMENT_COMPLETED endorsementId={} orgId={}",
                endorsementId, organization.getOrganizationId());
        List<AdminUser> recipients = routingResolver.resolveEndorsementCompletedRecipients(
                organization.getOrganizationId(), uploadedByAdminUserId);
        if (recipients.isEmpty()) {
            log.info("flagship_notification_skip event=ENDORSEMENT_COMPLETED reason=no_hr_admins orgId={} endorsementId={}",
                    organization.getOrganizationId(), endorsementId);
            return;
        }
        String orgName = organization.getOrganizationName();
        String deepLink = portalBase() + "/endorsements/" + endorsementId;
        List<String> selfEmployeeIds = resolveSelfEmployeeIdsForEndorsement(endorsementId);
        String bodyText = buildEndorsementCompletedBody(selfEmployeeIds);
        for (AdminUser admin : recipients) {
            String dedup = "ENDORSEMENT_COMPLETED:" + endorsementId + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", "Endorsement completed — " + orgName);
            vars.put("organizationName", orgName);
            vars.put("displayOrganizationName",
                    displayOrganizationName != null && !displayOrganizationName.isBlank() ? displayOrganizationName : "Vima Admin");
            if (actedByName != null && !actedByName.isBlank()) {
                vars.put("actedByName", actedByName);
            }
            vars.put("deepLinkUrl", deepLink);
            vars.put("hasSelfEmployeeIds", !selfEmployeeIds.isEmpty());
            vars.put("selfEmployeeIdsText", String.join(", ", selfEmployeeIds.subList(0, Math.min(selfEmployeeIds.size(), MAX_SELF_EMPLOYEE_IDS_IN_COPY))));
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organization.getOrganizationId(),
                    NotificationEventType.ENDORSEMENT_COMPLETED,
                    NotificationCategory.ENDORSEMENT,
                    NotificationSeverity.INFO,
                    "Endorsement completed — " + orgName,
                    bodyText,
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

    private List<String> resolveSelfEmployeeIdsForEndorsement(UUID endorsementId) {
        if (endorsementId == null) {
            return List.of();
        }
        try {
            List<DealEndorsement> rows = dealEndorsementRepository.findByEndorsementEndorsementIdWithDeal(endorsementId);
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            for (DealEndorsement row : rows) {
                if (row == null || row.getDeal() == null) {
                    continue;
                }
                String relationship = row.getDeal().getRelationship();
                if (!EmployeeToDeals.isPrimaryMemberRelationship(relationship)) {
                    continue;
                }
                String employeeId = row.getDeal().getEmployeeNumber();
                if (employeeId != null && !employeeId.isBlank()) {
                    ids.add(employeeId.trim());
                }
            }
            return new ArrayList<>(ids);
        } catch (Exception ex) {
            log.warn("flagship_notification_self_ids_lookup_failed endorsementId={} error={}", endorsementId, ex.getMessage(), ex);
            return List.of();
        }
    }

    private String buildEndorsementCompletedBody(List<String> selfEmployeeIds) {
        if (selfEmployeeIds == null || selfEmployeeIds.isEmpty()) {
            return "Endorsement processing is completed.";
        }
        int cap = Math.min(MAX_SELF_EMPLOYEE_IDS_IN_COPY, selfEmployeeIds.size());
        List<String> shown = selfEmployeeIds.subList(0, cap);
        int remaining = selfEmployeeIds.size() - cap;
        StringBuilder sb = new StringBuilder("Uploaded employees (Self): ")
                .append(String.join(", ", shown))
                .append(". Endorsement processing is completed");
        if (remaining > 0) {
            sb.append(" (and ").append(remaining).append(" more)");
        }
        sb.append(".");
        return sb.toString();
    }
}
