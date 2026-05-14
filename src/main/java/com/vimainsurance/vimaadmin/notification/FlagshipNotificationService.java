package com.vimainsurance.vimaadmin.notification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlagshipNotificationService {

    /**
     * Serializes {@link #emitEndorsementUploaded} per endorsement so two after-commit jobs (e.g. endorsement create
     * API + employee bulk upload) cannot race past {@code existsByDedupKey} and each create a SLACK delivery for the
     * same logical upload (duplicate identical Slack posts).
     */
    private final ConcurrentHashMap<UUID, Object> endorsementUploadEmitLocks = new ConcurrentHashMap<>();

    private final NotificationRoutingResolver routingResolver;
    private final NotificationService notificationService;
    private final NotificationDispatcher notificationDispatcher;
    private final AfterCommitNotificationRunner afterCommitNotificationRunner;
    private final IDealEndorsementRepository dealEndorsementRepository;
    private final IEndorsementRepository endorsementRepository;

    @Value("${app.portal-url:}")
    private String portalUrlOverride;

    @Value("${app.base-url:http://localhost:7219}")
    private String appBaseUrl;

    private record EndorsementMemberCounts(int totalEmployees, int totalDependents) {
    }

    public void scheduleEndorsementUploaded(Endorsement endorsement, Organization organization, AdminUser uploadedBy) {
        scheduleEndorsementUploaded(endorsement, organization, uploadedBy, List.of(), null, null);
    }

    public void scheduleEndorsementUploaded(
            Endorsement endorsement,
            Organization organization,
            AdminUser uploadedBy,
            List<String> selfEmployeeIds) {
        scheduleEndorsementUploaded(endorsement, organization, uploadedBy, selfEmployeeIds, null, null);
    }

    /**
     * @param uploadTotalEmployees when non-null with {@code uploadTotalDependents}, Slack/email body uses these
     *                              aggregate counts (whole CSV) instead of per-endorsement deal rows — fixes
     *                              split uploads where the linked endorsement is e.g. PARENT_GMC-only (0 self rows).
     */
    public void scheduleEndorsementUploaded(
            Endorsement endorsement,
            Organization organization,
            AdminUser uploadedBy,
            List<String> selfEmployeeIds,
            Integer uploadTotalEmployees,
            Integer uploadTotalDependents) {
        if (endorsement == null || organization == null) {
            log.info("flagship_notification_skip event=ENDORSEMENT_UPLOADED reason=missing_context endorsementNull={} orgNull={}",
                    endorsement == null, organization == null);
            return;
        }
        List<String> ids = selfEmployeeIds != null ? new ArrayList<>(selfEmployeeIds) : List.of();
        log.info("flagship_notification_schedule event=ENDORSEMENT_UPLOADED endorsementId={} orgId={} uploadedBy={} selfEmployeeIdCount={} uploadAggregateEmployees={} uploadAggregateDependents={}",
                endorsement.getEndorsementId(), organization.getOrganizationId(), uploadedBy != null ? uploadedBy.getId() : null, ids.size(),
                uploadTotalEmployees, uploadTotalDependents);
        afterCommitNotificationRunner.runAsyncAfterCommit(() -> emitEndorsementUploaded(
                endorsement, organization, uploadedBy, ids, uploadTotalEmployees, uploadTotalDependents));
    }

    public void scheduleEnrollmentAllSubmitted(UUID organizationId, UUID windowId, String organizationDisplayName) {
        if (organizationId == null || windowId == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEnrollmentAllSubmitted(organizationId, windowId, organizationDisplayName));
    }

    public void scheduleEnrollmentSubmissionApproved(
            UUID organizationId,
            UUID windowId,
            UUID submissionId,
            UUID reviewerAdminUserId,
            String organizationDisplayName,
            String reviewerName,
            String enrolleeDisplayName,
            String submissionReferenceNumber) {
        if (organizationId == null || windowId == null || submissionId == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEnrollmentSubmissionApproved(
                        organizationId,
                        windowId,
                        submissionId,
                        reviewerAdminUserId,
                        organizationDisplayName,
                        reviewerName,
                        enrolleeDisplayName,
                        submissionReferenceNumber));
    }

    public void scheduleEnrollmentWindowOpened(UUID organizationId, UUID windowId, String organizationDisplayName) {
        if (organizationId == null || windowId == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEnrollmentWindowOpened(organizationId, windowId, organizationDisplayName));
    }

    public void scheduleEnrollmentWindowClosingSoon(
            UUID organizationId,
            UUID windowId,
            String organizationDisplayName,
            LocalDate endDate) {
        if (organizationId == null || windowId == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEnrollmentWindowClosingSoon(organizationId, windowId, organizationDisplayName, endDate));
    }

    public void scheduleEnrollmentWindowClosed(
            UUID organizationId,
            UUID windowId,
            String organizationDisplayName,
            String closedByName,
            UUID endorsementId,
            Integer totalEmployees,
            Integer totalDependents) {
        if (organizationId == null || windowId == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEnrollmentWindowClosed(
                        organizationId,
                        windowId,
                        organizationDisplayName,
                        closedByName,
                        endorsementId,
                        totalEmployees,
                        totalDependents));
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
            List<String> selfEmployeeIds,
            Integer uploadTotalEmployees,
            Integer uploadTotalDependents) {
        UUID endorsementId = endorsement.getEndorsementId();
        log.info("flagship_notification_emit_start event=ENDORSEMENT_UPLOADED endorsementId={} orgId={}",
                endorsementId, organization.getOrganizationId());
        if (endorsementId == null) {
            log.warn("flagship_notification_skip event=ENDORSEMENT_UPLOADED reason=missing_endorsement_id");
            return;
        }
        Object emitLock = endorsementUploadEmitLocks.computeIfAbsent(endorsementId, k -> new Object());
        try {
            synchronized (emitLock) {
                List<AdminUser> recipients = routingResolver.resolveRecipients(NotificationEventType.ENDORSEMENT_UPLOADED, null);
                if (recipients.isEmpty()) {
                    log.info("flagship_notification_skip event=ENDORSEMENT_UPLOADED reason=no_vima_recipients endorsementId={}",
                            endorsementId);
                    return;
                }
                String orgName = organization.getOrganizationName();
                String uploader = uploadedBy != null && uploadedBy.getFullName() != null ? uploadedBy.getFullName() : "HR";
                String deepLink = portalBase() + "/endorsements/" + endorsementId;
                EndorsementMemberCounts counts = resolveUploadNotificationMemberCounts(
                        endorsementId, uploadTotalEmployees, uploadTotalDependents);
                String bodyText = buildEndorsementUploadedBody(counts, uploader, orgName);
                int slackRecipientIndex = 0;
                for (int i = 0; i < recipients.size(); i++) {
                    AdminUser admin = recipients.get(i);
                    String dedup = "ENDORSEMENT_UPLOADED:" + endorsementId + ":" + admin.getId();
                    Map<String, Object> vars = new HashMap<>();
                    vars.put("title", "Endorsement uploaded — " + orgName);
                    vars.put("organizationName", orgName);
                    vars.put("uploadedByName", uploader);
                    vars.put("creatorName", uploader);
                    vars.put("creatorRole", uploadedBy != null ? uploadedBy.getRole() : null);
                    vars.put("creatorEmail", uploadedBy != null ? uploadedBy.getEmail() : null);
                    vars.put("endorsementType", endorsement.getEndorsementType() != null ? endorsement.getEndorsementType().name() : "");
                    vars.put("deepLinkUrl", deepLink);
                    vars.put("totalEmployees", counts.totalEmployees());
                    vars.put("totalDependents", counts.totalDependents());
                    Boolean slackDeliveryEnabled = (i == slackRecipientIndex) ? null : Boolean.FALSE;
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
                            vars,
                            slackDeliveryEnabled);
                    notificationService.createIfAbsent(cmd).ifPresentOrElse(
                            id -> {
                                notificationDispatcher.dispatchDeliveriesFor(id);
                                log.info("flagship_notification_emit event=ENDORSEMENT_UPLOADED notificationId={} dedupKey={} recipientId={} slackIncluded={}",
                                        id, dedup, admin.getId(),
                                        slackDeliveryEnabled == null || Boolean.TRUE.equals(slackDeliveryEnabled));
                            },
                            () -> log.debug("flagship_notification_dedup event=ENDORSEMENT_UPLOADED dedupKey={}", dedup));
                }
            }
        } finally {
            endorsementUploadEmitLocks.remove(endorsementId, emitLock);
        }
    }

    /**
     * In-app/Slack body and email {@code bodyText} fallback for uploaded endorsement.
     */
    private String buildEndorsementUploadedBody(
            EndorsementMemberCounts counts,
            String uploadedByName,
            String organizationName) {
        String uploader = (uploadedByName != null && !uploadedByName.isBlank()) ? uploadedByName : "HR";
        String org = (organizationName != null && !organizationName.isBlank()) ? organizationName : "Organization";
        return "Total Employees: " + counts.totalEmployees()
                + "\n:family: Total Dependents: " + counts.totalDependents()
                + "\n:bust_in_silhouette: Uploaded By: " + uploader
                + "\n:office: Organization: " + org;
    }

    private void emitEnrollmentAllSubmitted(UUID organizationId, UUID windowId, String organizationDisplayName) {
        List<AdminUser> recipients = routingResolver.resolveHrAndVimaPlatformRecipients(organizationId);
        if (recipients.isEmpty()) {
            log.info("flagship_notification_skip event=ENROLLMENT_ALL_SUBMITTED reason=no_recipients orgId={}", organizationId);
            return;
        }
        String orgName = organizationDisplayName != null && !organizationDisplayName.isBlank()
                ? organizationDisplayName
                : "Your organization";
        String deepLink = portalBase() + "/group/" + organizationId + "/enrollment-windows/" + windowId;
        Map<String, Object> baseVars = new HashMap<>();
        baseVars.put("title", "Enrollment submitted for HR review — " + orgName);
        baseVars.put("organizationName", orgName);
        baseVars.put("deepLinkUrl", deepLink);

        int slackRecipientIndex = resolveEnrollmentSlackRecipientIndex(recipients);
        for (int i = 0; i < recipients.size(); i++) {
            AdminUser admin = recipients.get(i);
            String dedup = "ENROLLMENT_ALL_SUBMITTED:" + windowId + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>(baseVars);
            Boolean slackDeliveryEnabled = (i == slackRecipientIndex) ? null : Boolean.FALSE;
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organizationId,
                    NotificationEventType.ENROLLMENT_ALL_SUBMITTED,
                    NotificationCategory.ENROLLMENT,
                    NotificationSeverity.INFO,
                    "Enrollment submitted for HR review — " + orgName,
                    "Employee enrollment submission is complete and ready for HR review in Vima.",
                    deepLink,
                    dedup,
                    "Enrollment submitted for HR review — " + orgName,
                    "email/notification-enrollment-all-submitted",
                    vars,
                    slackDeliveryEnabled);
            notificationService.createIfAbsent(cmd).ifPresentOrElse(
                    id -> {
                        notificationDispatcher.dispatchDeliveriesFor(id);
                        log.info("flagship_notification_emit event=ENROLLMENT_ALL_SUBMITTED notificationId={} dedupKey={} recipientId={} slackIncluded={}",
                                id, dedup, admin.getId(),
                                slackDeliveryEnabled == null || Boolean.TRUE.equals(slackDeliveryEnabled));
                    },
                    () -> log.debug("flagship_notification_dedup event=ENROLLMENT_ALL_SUBMITTED dedupKey={}", dedup));
        }
    }

    private void emitEnrollmentSubmissionApproved(
            UUID organizationId,
            UUID windowId,
            UUID submissionId,
            UUID reviewerAdminUserId,
            String organizationDisplayName,
            String reviewerName,
            String enrolleeDisplayName,
            String submissionReferenceNumber) {
        List<AdminUser> recipients = routingResolver.resolveEnrollmentSubmissionApprovedRecipients(
                organizationId, reviewerAdminUserId);
        if (recipients.isEmpty()) {
            log.info("flagship_notification_skip event=ENROLLMENT_SUBMISSION_APPROVED reason=no_hr_admins orgId={} submissionId={}",
                    organizationId, submissionId);
            return;
        }
        String orgName = organizationDisplayName != null && !organizationDisplayName.isBlank()
                ? organizationDisplayName
                : "Your organization";
        String actor = reviewerName != null && !reviewerName.isBlank() ? reviewerName : "HR Admin";
        String enrollee = enrolleeDisplayName != null && !enrolleeDisplayName.isBlank() ? enrolleeDisplayName.trim() : "An employee";
        String deepLink = portalBase() + "/group/" + organizationId + "/enrollment-windows/" + windowId;
        String bodyText = "Employee: " + enrollee
                + "\nOrganization: " + orgName
                + "\nApproved by: " + actor;
        int slackRecipientIndex = resolveEnrollmentSlackRecipientIndex(recipients);
        for (int i = 0; i < recipients.size(); i++) {
            AdminUser admin = recipients.get(i);
            String dedup = "ENROLLMENT_SUBMISSION_APPROVED:" + submissionId + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", "Enrollment submission approved — " + orgName);
            vars.put("organizationName", orgName);
            vars.put("enrolleeName", enrollee);
            vars.put("deepLinkUrl", deepLink);
            vars.put("actedByName", actor);
            vars.put("creatorName", actor);
            vars.put("creatorRole", "HR_ADMIN");
            String recipientLabel = admin.getFullName() != null && !admin.getFullName().isBlank()
                    ? admin.getFullName().trim()
                    : (admin.getUsername() != null && !admin.getUsername().isBlank() ? admin.getUsername().trim() : "there");
            vars.put("recipientName", recipientLabel);
            vars.put("message",
                    "A self-enrollment submission for " + enrollee + " at " + orgName + " has been approved by " + actor + ".");
            vars.put("referenceNumber",
                    submissionReferenceNumber != null && !submissionReferenceNumber.isBlank()
                            ? submissionReferenceNumber.trim()
                            : "");
            Boolean slackDeliveryEnabled = (i == slackRecipientIndex) ? null : Boolean.FALSE;
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organizationId,
                    NotificationEventType.ENROLLMENT_SUBMISSION_APPROVED,
                    NotificationCategory.ENROLLMENT,
                    NotificationSeverity.INFO,
                    "Enrollment submission approved — " + orgName,
                    bodyText,
                    deepLink,
                    dedup,
                    "Enrollment submission approved — " + orgName,
                    "email/notification-enrollment-submission-approved",
                    vars,
                    slackDeliveryEnabled);
            notificationService.createIfAbsent(cmd).ifPresentOrElse(
                    id -> {
                        notificationDispatcher.dispatchDeliveriesFor(id);
                        log.info("flagship_notification_emit event=ENROLLMENT_SUBMISSION_APPROVED notificationId={} dedupKey={} recipientId={} slackIncluded={}",
                                id, dedup, admin.getId(),
                                slackDeliveryEnabled == null || Boolean.TRUE.equals(slackDeliveryEnabled));
                    },
                    () -> log.debug("flagship_notification_dedup event=ENROLLMENT_SUBMISSION_APPROVED dedupKey={}", dedup));
        }
    }

    /**
     * Single Slack post is attached to the first org HR admin in the merged list (not necessarily list order index 0
     * if a VIMA user were ever ordered first). Falls back to the first recipient when no HR role is present.
     */
    private static int resolveEnrollmentSlackRecipientIndex(List<AdminUser> recipients) {
        for (int i = 0; i < recipients.size(); i++) {
            AdminUser u = recipients.get(i);
            if (u != null && NotificationRoutingResolver.isHrAdminRole(u.getRole())) {
                return i;
            }
        }
        return 0;
    }

    private void emitEnrollmentWindowOpened(UUID organizationId, UUID windowId, String organizationDisplayName) {
        List<AdminUser> recipients = routingResolver.resolveRecipients(
                NotificationEventType.ENROLLMENT_WINDOW_OPENED, organizationId);
        if (recipients.isEmpty()) {
            log.info("flagship_notification_skip event=ENROLLMENT_WINDOW_OPENED reason=no_hr_admins orgId={}", organizationId);
            return;
        }
        AdminUser primaryRecipient = recipients.get(0);
        String orgName = organizationDisplayName != null && !organizationDisplayName.isBlank()
                ? organizationDisplayName
                : "Your organization";
        String deepLink = portalBase() + "/group/" + organizationId + "/enrollment-windows/" + windowId;
        String dedup = "ENROLLMENT_WINDOW_OPENED:" + windowId;
        Map<String, Object> vars = new HashMap<>();
        vars.put("title", "Enrollment window opened — " + orgName);
        vars.put("organizationName", orgName);
        vars.put("deepLinkUrl", deepLink);
        CreateNotificationCommand cmd = new CreateNotificationCommand(
                primaryRecipient.getId(),
                organizationId,
                NotificationEventType.ENROLLMENT_WINDOW_OPENED,
                NotificationCategory.ENROLLMENT,
                NotificationSeverity.INFO,
                "Enrollment window opened — " + orgName,
                "Enrollment window is now open.",
                deepLink,
                dedup,
                "Enrollment window opened — " + orgName,
                "email/notification-enrollment-window-opened",
                vars,
                null);
        notificationService.createIfAbsent(cmd).ifPresentOrElse(
                id -> {
                    notificationDispatcher.dispatchDeliveriesFor(id);
                    log.info("flagship_notification_emit event=ENROLLMENT_WINDOW_OPENED notificationId={} dedupKey={} recipientId={} suppressedRecipients={}",
                            id, dedup, primaryRecipient.getId(), Math.max(0, recipients.size() - 1));
                },
                () -> log.debug("flagship_notification_dedup event=ENROLLMENT_WINDOW_OPENED dedupKey={}", dedup));
    }

    private void emitEnrollmentWindowClosingSoon(
            UUID organizationId,
            UUID windowId,
            String organizationDisplayName,
            LocalDate endDate) {
        List<AdminUser> recipients = routingResolver.resolveRecipients(
                NotificationEventType.ENROLLMENT_WINDOW_CLOSING_SOON, organizationId);
        if (recipients.isEmpty()) {
            log.info("flagship_notification_skip event=ENROLLMENT_WINDOW_CLOSING_SOON reason=no_hr_admins orgId={}", organizationId);
            return;
        }
        AdminUser primaryRecipient = recipients.get(0);
        String orgName = organizationDisplayName != null && !organizationDisplayName.isBlank()
                ? organizationDisplayName
                : "Your organization";
        String deepLink = portalBase() + "/group/" + organizationId + "/enrollment-windows/" + windowId;
        String endDateText = endDate != null ? endDate.toString() : "";
        String dedup = "ENROLLMENT_WINDOW_CLOSING_SOON:" + windowId;
        Map<String, Object> vars = new HashMap<>();
        vars.put("title", "Enrollment window closing soon — " + orgName);
        vars.put("organizationName", orgName);
        vars.put("deepLinkUrl", deepLink);
        vars.put("endDate", endDateText);
        String body = endDate != null
                ? "Enrollment window will close soon (on " + endDateText + ")."
                : "Enrollment window will close soon.";
        CreateNotificationCommand cmd = new CreateNotificationCommand(
                primaryRecipient.getId(),
                organizationId,
                NotificationEventType.ENROLLMENT_WINDOW_CLOSING_SOON,
                NotificationCategory.ENROLLMENT,
                NotificationSeverity.INFO,
                "Enrollment window closing soon — " + orgName,
                body,
                deepLink,
                dedup,
                "Enrollment window closing soon — " + orgName,
                "email/notification-enrollment-window-closing-soon",
                vars,
                null);
        notificationService.createIfAbsent(cmd).ifPresentOrElse(
                id -> {
                    notificationDispatcher.dispatchDeliveriesFor(id);
                    log.info("flagship_notification_emit event=ENROLLMENT_WINDOW_CLOSING_SOON notificationId={} dedupKey={} recipientId={} suppressedRecipients={}",
                            id, dedup, primaryRecipient.getId(), Math.max(0, recipients.size() - 1));
                },
                () -> log.debug("flagship_notification_dedup event=ENROLLMENT_WINDOW_CLOSING_SOON dedupKey={}", dedup));
    }

    private void emitEnrollmentWindowClosed(
            UUID organizationId,
            UUID windowId,
            String organizationDisplayName,
            String closedByName,
            UUID endorsementId,
            Integer totalEmployees,
            Integer totalDependents) {
        List<AdminUser> recipients = routingResolver.resolveHrAndVimaPlatformRecipients(organizationId);
        if (recipients.isEmpty()) {
            log.info("flagship_notification_skip event=ENROLLMENT_WINDOW_CLOSED reason=no_recipients orgId={}", organizationId);
            return;
        }
        String orgName = organizationDisplayName != null && !organizationDisplayName.isBlank()
                ? organizationDisplayName
                : "Your organization";
        String actor = closedByName != null && !closedByName.isBlank() ? closedByName : "HR Admin";
        int employees = totalEmployees != null ? Math.max(0, totalEmployees) : 0;
        int dependents = totalDependents != null ? Math.max(0, totalDependents) : 0;
        String deepLink = endorsementId != null
                ? portalBase() + "/endorsements/" + endorsementId
                : portalBase() + "/group/" + organizationId + "/enrollment-windows/" + windowId;
        String body = "Total Employees: " + employees
                + "\n:family: Total Dependents: " + dependents
                + "\n:bust_in_silhouette: Closed By: " + actor
                + "\n:office: Organization: " + orgName;
        int slackRecipientIndex = resolveEnrollmentSlackRecipientIndex(recipients);
        for (int i = 0; i < recipients.size(); i++) {
            AdminUser admin = recipients.get(i);
            String dedup = "ENROLLMENT_WINDOW_CLOSED:" + windowId + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", "Enrollment window closed — " + orgName);
            vars.put("organizationName", orgName);
            vars.put("deepLinkUrl", deepLink);
            vars.put("actedByName", actor);
            vars.put("creatorName", actor);
            vars.put("creatorRole", "HR_ADMIN");
            vars.put("totalEmployees", employees);
            vars.put("totalDependents", dependents);
            Boolean slackDeliveryEnabled = (i == slackRecipientIndex) ? null : Boolean.FALSE;
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organizationId,
                    NotificationEventType.ENROLLMENT_WINDOW_CLOSED,
                    NotificationCategory.ENROLLMENT,
                    NotificationSeverity.INFO,
                    "Enrollment window closed — " + orgName,
                    body,
                    deepLink,
                    dedup,
                    "Enrollment window closed — " + orgName,
                    "email/notification-enrollment-window-closed",
                    vars,
                    slackDeliveryEnabled);
            notificationService.createIfAbsent(cmd).ifPresentOrElse(
                    id -> {
                        notificationDispatcher.dispatchDeliveriesFor(id);
                        log.info("flagship_notification_emit event=ENROLLMENT_WINDOW_CLOSED notificationId={} dedupKey={} recipientId={} slackIncluded={}",
                                id, dedup, admin.getId(),
                                slackDeliveryEnabled == null || Boolean.TRUE.equals(slackDeliveryEnabled));
                    },
                    () -> log.debug("flagship_notification_dedup event=ENROLLMENT_WINDOW_CLOSED dedupKey={}", dedup));
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
        String displayOrgName = (displayOrganizationName != null && !displayOrganizationName.isBlank())
                ? displayOrganizationName
                : "Vima";
        String deepLink = portalBase() + "/endorsements/" + endorsementId;
        EndorsementMemberCounts counts = resolveCompletionNotificationMemberCounts(endorsementId);
        String bodyText = buildEndorsementCompletedBody(
                counts,
                actedByName,
                displayOrgName);
        int slackRecipientIndex = resolveEnrollmentSlackRecipientIndex(recipients);
        for (int i = 0; i < recipients.size(); i++) {
            AdminUser admin = recipients.get(i);
            String dedup = "ENDORSEMENT_COMPLETED:" + endorsementId + ":" + admin.getId();
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", "Endorsement completed — " + orgName);
            vars.put("organizationName", orgName);
            vars.put("displayOrganizationName",
                    displayOrgName);
            String recipientLabel = admin.getFullName() != null && !admin.getFullName().isBlank()
                    ? admin.getFullName().trim()
                    : (admin.getUsername() != null && !admin.getUsername().isBlank() ? admin.getUsername().trim() : "there");
            vars.put("recipientName", recipientLabel);
            vars.put("message",
                    "An endorsement for " + orgName + " has completed and affected employees are now active in Vima.");
            if (actedByName != null && !actedByName.isBlank()) {
                vars.put("actedByName", actedByName);
            }
            vars.put("creatorName", actedByName != null && !actedByName.isBlank() ? actedByName : "Vima Admin");
            vars.put("creatorRole", "VIMA_ADMIN");
            vars.put("deepLinkUrl", deepLink);
            vars.put("totalEmployees", counts.totalEmployees());
            vars.put("totalDependents", counts.totalDependents());
            vars.put("uploadedByName", actedByName != null && !actedByName.isBlank() ? actedByName : "Vima Admin");
            Boolean slackDeliveryEnabled = (i == slackRecipientIndex) ? null : Boolean.FALSE;
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
                    vars,
                    slackDeliveryEnabled);
            notificationService.createIfAbsent(cmd).ifPresentOrElse(
                    id -> {
                        notificationDispatcher.dispatchDeliveriesFor(id);
                        log.info("flagship_notification_emit event=ENDORSEMENT_COMPLETED notificationId={} dedupKey={} recipientId={} slackIncluded={}",
                                id, dedup, admin.getId(),
                                slackDeliveryEnabled == null || Boolean.TRUE.equals(slackDeliveryEnabled));
                    },
                    () -> log.debug("flagship_notification_dedup event=ENDORSEMENT_COMPLETED dedupKey={}", dedup));
        }
    }

    private String buildEndorsementCompletedBody(
            EndorsementMemberCounts counts,
            String actedByName,
            String organizationName) {
        String uploadedBy = (actedByName != null && !actedByName.isBlank()) ? actedByName : "Vima Admin";
        String org = (organizationName != null && !organizationName.isBlank()) ? organizationName : "Organization";
        return "Total Employees: " + counts.totalEmployees()
                + "\n:family: Total Dependents: " + counts.totalDependents()
                + "\n:bust_in_silhouette: Approved By: " + uploadedBy
                + "\n:office: Organization: " + org;
    }

    private EndorsementMemberCounts resolveUploadNotificationMemberCounts(
            UUID endorsementId,
            Integer uploadTotalEmployees,
            Integer uploadTotalDependents) {
        if (uploadTotalEmployees != null && uploadTotalDependents != null) {
            return new EndorsementMemberCounts(
                    Math.max(0, uploadTotalEmployees),
                    Math.max(0, uploadTotalDependents));
        }
        return resolveMemberCountsForEndorsement(endorsementId);
    }

    /**
     * Completion Slack/email should match portal totals on {@link Endorsement#getTotalEmployees()} /
     * {@link Endorsement#getTotalDependents()}. Per-endorsement {@link DealEndorsement} recount misses members
     * routed only to a sibling split (e.g. parents on PARENT_GMC while the primary id is GMC/GHI).
     */
    private EndorsementMemberCounts resolveCompletionNotificationMemberCounts(UUID endorsementId) {
        if (endorsementId == null) {
            return new EndorsementMemberCounts(0, 0);
        }
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if (opt.isPresent()) {
                Endorsement e = opt.get();
                int te = e.getTotalEmployees() != null ? Math.max(0, e.getTotalEmployees()) : 0;
                int td = e.getTotalDependents() != null ? Math.max(0, e.getTotalDependents()) : 0;
                if (te > 0 || td > 0) {
                    return new EndorsementMemberCounts(te, td);
                }
            }
        } catch (Exception ex) {
            log.warn("flagship_notification_member_counts_endorsement_lookup_failed endorsementId={} error={}",
                    endorsementId, ex.getMessage(), ex);
        }
        return resolveMemberCountsForEndorsement(endorsementId);
    }

    private EndorsementMemberCounts resolveMemberCountsForEndorsement(UUID endorsementId) {
        if (endorsementId == null) {
            return new EndorsementMemberCounts(0, 0);
        }
        try {
            List<DealEndorsement> rows = dealEndorsementRepository.findByEndorsementEndorsementIdWithDeal(endorsementId);
            LinkedHashSet<UUID> employeeIds = new LinkedHashSet<>();
            LinkedHashSet<UUID> dependentIds = new LinkedHashSet<>();
            for (DealEndorsement row : rows) {
                if (row == null || row.getDeal() == null || row.getDeal().getIndividualId() == null) {
                    continue;
                }
                UUID individualId = row.getDeal().getIndividualId();
                String relationship = row.getDeal().getRelationship();
                if (EmployeeToDeals.isPrimaryMemberRelationship(relationship)) {
                    employeeIds.add(individualId);
                } else {
                    dependentIds.add(individualId);
                }
            }
            return new EndorsementMemberCounts(employeeIds.size(), dependentIds.size());
        } catch (Exception ex) {
            log.warn("flagship_notification_member_counts_lookup_failed endorsementId={} error={}",
                    endorsementId, ex.getMessage(), ex);
            return new EndorsementMemberCounts(0, 0);
        }
    }
}
