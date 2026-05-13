package com.vimainsurance.vimaadmin.notification;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimQuery;
import com.vimainsurance.vimaadmin.notification.dto.CreateNotificationCommand;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.notification.enums.NotificationSeverity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimsNotificationEmitterService {

    private final NotificationRoutingResolver routingResolver;
    private final NotificationService notificationService;
    private final NotificationDispatcher notificationDispatcher;
    private final AfterCommitNotificationRunner afterCommitNotificationRunner;

    @Value("${app.portal-url:}")
    private String portalUrlOverride;

    @Value("${app.base-url:http://localhost:7219}")
    private String appBaseUrl;

    public void scheduleClaimSubmitted(Claim claim, String fromName, String actorOrganizationName, String actorRole) {
        scheduleClaimEvent(claim, NotificationEventType.EMPLOYEE_CLAIM_SUBMITTED, fromName, actorOrganizationName, actorRole);
    }

    public void scheduleClaimQueryRaised(Claim claim, ClaimQuery query, String fromName, String actorOrganizationName, String actorRole) {
        if (claim == null || query == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitClaimQueryRaised(claim, query, fromName, actorOrganizationName, actorRole));
    }

    public void scheduleClaimQueryResponded(Claim claim, ClaimQuery query, String fromName, String actorOrganizationName, String actorRole) {
        if (claim == null || query == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitClaimQueryResponded(claim, query, fromName, actorOrganizationName, actorRole));
    }

    public void scheduleEmployeeClaimQueryResponseSubmitted(
            Claim claim,
            ClaimQuery query,
            String fromName,
            String actorOrganizationName,
            String actorRole) {
        if (claim == null || query == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitEmployeeClaimQueryResponseSubmitted(claim, query, fromName, actorOrganizationName, actorRole));
    }

    public void scheduleClaimApproved(Claim claim, String fromName, String actorOrganizationName, String actorRole) {
        scheduleClaimEvent(claim, NotificationEventType.EMPLOYEE_CLAIM_APPROVED, fromName, actorOrganizationName, actorRole);
    }

    public void scheduleClaimRejected(Claim claim, String fromName, String actorOrganizationName, String actorRole) {
        scheduleClaimEvent(claim, NotificationEventType.EMPLOYEE_CLAIM_REJECTED, fromName, actorOrganizationName, actorRole);
    }

    public void scheduleClaimSettled(Claim claim, String fromName, String actorOrganizationName, String actorRole) {
        scheduleClaimEvent(claim, NotificationEventType.EMPLOYEE_CLAIM_SETTLED, fromName, actorOrganizationName, actorRole);
    }

    private void scheduleClaimEvent(
            Claim claim,
            NotificationEventType eventType,
            String fromName,
            String actorOrganizationName,
            String actorRole) {
        if (claim == null || claim.getId() == null || claim.getOrganization() == null) {
            return;
        }
        afterCommitNotificationRunner.runAsyncAfterCommit(
                () -> emitClaimEvent(claim, eventType, fromName, actorOrganizationName, actorRole));
    }

    private void emitClaimEvent(
            Claim claim,
            NotificationEventType eventType,
            String fromName,
            String actorOrganizationName,
            String actorRole) {
        String orgName = resolveOrganizationName(claim, actorOrganizationName, actorRole);
        String from = resolveFromName(claim, fromName);
        String claimNumber = claim.getClaimNumber() != null ? claim.getClaimNumber() : String.valueOf(claim.getId());
        String deepLink = portalBase() + "/hr/claims/" + claim.getId();
        String title;
        String body;
        String template;
        switch (eventType) {
            case EMPLOYEE_CLAIM_SUBMITTED -> {
                title = "Employee claim submitted — " + orgName;
                body = "Claim " + claimNumber + " has been submitted and is pending review.";
                template = "email/notification-employee-claim-submitted";
            }
            case EMPLOYEE_CLAIM_APPROVED -> {
                title = "Employee claim approved — " + orgName;
                body = "Claim " + claimNumber + " has been approved.";
                template = "email/notification-employee-claim-approved";
            }
            case EMPLOYEE_CLAIM_REJECTED -> {
                title = "Employee claim rejected — " + orgName;
                body = "Claim " + claimNumber + " has been rejected.";
                template = "email/notification-employee-claim-rejected";
            }
            case EMPLOYEE_CLAIM_SETTLED -> {
                title = "Employee claim settled — " + orgName;
                body = "Claim " + claimNumber + " has been settled.";
                template = "email/notification-employee-claim-settled";
            }
            default -> {
                return;
            }
        }
        Map<String, Object> vars = baseVars(claim, title, deepLink, from, orgName, actorRole);
        emitToClaimsTeam(claim, eventType, title, body, deepLink, template, vars,
                eventType.name() + ":" + claim.getId());
    }

    private void emitClaimQueryRaised(
            Claim claim,
            ClaimQuery query,
            String fromName,
            String actorOrganizationName,
            String actorRole) {
        String deepLink = portalBase() + "/hr/claims/" + claim.getId();
        String orgName = resolveOrganizationName(claim, actorOrganizationName, actorRole);
        String from = resolveFromName(claim, fromName);
        String claimNumber = claim.getClaimNumber() != null ? claim.getClaimNumber() : String.valueOf(claim.getId());
        String title = "Claim query raised — " + orgName;
        String body = "A query was raised on claim " + claimNumber + ".";
        Map<String, Object> vars = baseVars(claim, title, deepLink, from, orgName, actorRole);
        vars.put("queryText", query.getQueryText() != null ? query.getQueryText() : "");
        emitToClaimsTeam(
                claim,
                NotificationEventType.EMPLOYEE_CLAIM_QUERY_RAISED,
                title,
                body,
                deepLink,
                "email/notification-employee-claim-query-raised",
                vars,
                "EMPLOYEE_CLAIM_QUERY_RAISED:" + claim.getId() + ":" + query.getId());
    }

    private void emitClaimQueryResponded(
            Claim claim,
            ClaimQuery query,
            String fromName,
            String actorOrganizationName,
            String actorRole) {
        String deepLink = portalBase() + "/hr/claims/" + claim.getId();
        String orgName = resolveOrganizationName(claim, actorOrganizationName, actorRole);
        String from = resolveFromName(claim, fromName);
        String claimNumber = claim.getClaimNumber() != null ? claim.getClaimNumber() : String.valueOf(claim.getId());
        String title = "Claim query responded — " + orgName;
        String body = "A query response was submitted for claim " + claimNumber + ".";
        Map<String, Object> vars = baseVars(claim, title, deepLink, from, orgName, actorRole);
        vars.put("responseText", query.getResponseText() != null ? query.getResponseText() : "");
        emitToClaimsTeam(
                claim,
                NotificationEventType.EMPLOYEE_CLAIM_QUERY_RESPONDED,
                title,
                body,
                deepLink,
                "email/notification-employee-claim-query-responded",
                vars,
                "EMPLOYEE_CLAIM_QUERY_RESPONDED:" + claim.getId() + ":" + query.getId());
    }

    private void emitEmployeeClaimQueryResponseSubmitted(
            Claim claim,
            ClaimQuery query,
            String fromName,
            String actorOrganizationName,
            String actorRole) {
        String deepLink = portalBase() + "/hr/claims/" + claim.getId();
        String orgName = resolveOrganizationName(claim, actorOrganizationName, actorRole);
        String from = resolveFromName(claim, fromName);
        String claimNumber = claim.getClaimNumber() != null ? claim.getClaimNumber() : String.valueOf(claim.getId());
        String title = "Employee query response submitted — " + orgName;
        String body = "Employee submitted a query response for claim " + claimNumber + ".";
        Map<String, Object> vars = baseVars(claim, title, deepLink, from, orgName, actorRole);
        vars.put("responseText", query.getEmployeeRemarks() != null ? query.getEmployeeRemarks() : "");
        emitToClaimsTeam(
                claim,
                NotificationEventType.EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED,
                title,
                body,
                deepLink,
                "email/notification-employee-claim-query-response-submitted",
                vars,
                "EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED:" + claim.getId() + ":" + query.getId());
    }

    private Map<String, Object> baseVars(
            Claim claim,
            String title,
            String deepLink,
            String from,
            String organization,
            String actorRole) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("title", title);
        vars.put("deepLinkUrl", deepLink);
        vars.put("claimId", claim.getId());
        vars.put("claimNumber", claim.getClaimNumber());
        vars.put("memberName", claim.getMemberName());
        vars.put("organizationName", organization);
        vars.put("organization", organization);
        vars.put("from", from);
        vars.put("actedByName", from);
        vars.put("uploadedByName", from);
        vars.put("actorRole", actorRole);
        vars.put("creatorName", from);
        vars.put("creatorRole", actorRole);
        vars.put("claimAmount", claim.getClaimAmount() != null ? claim.getClaimAmount() : BigDecimal.ZERO);
        if (claim.getClaimAmount() != null) {
            vars.put("claimAmountFormatted", claim.getClaimAmount().toPlainString());
        } else {
            vars.put("claimAmountFormatted", "");
        }
        vars.put("hospitalName", claim.getHospitalName() != null ? claim.getHospitalName() : "");
        vars.put("queryText", "");
        vars.put("responseText", "");
        return vars;
    }

    private void emitToClaimsTeam(
            Claim claim,
            NotificationEventType eventType,
            String title,
            String body,
            String deepLink,
            String emailTemplate,
            Map<String, Object> vars,
            String dedupPrefix) {
        UUID organizationId = claim.getOrganization() != null ? claim.getOrganization().getOrganizationId() : null;
        if (organizationId == null) {
            return;
        }
        List<AdminUser> recipients = routingResolver.resolveClaimsTeamRecipients(organizationId);
        if (recipients.isEmpty()) {
            log.info("claims_notification_skip event={} reason=no_recipients claimId={} orgId={}",
                    eventType, claim.getId(), organizationId);
            return;
        }
        for (int i = 0; i < recipients.size(); i++) {
            AdminUser admin = recipients.get(i);
            String dedup = dedupPrefix + ":" + admin.getId();
            Boolean slackDeliveryEnabled = i == 0 ? null : Boolean.FALSE;
            String recipientDeepLink = deepLinkForRecipient(claim, admin);
            Map<String, Object> recipientVars = new HashMap<>(vars);
            recipientVars.put("deepLinkUrl", recipientDeepLink);
            applyHrClaimRecipientEmailVars(recipientVars, admin, body, eventType);
            CreateNotificationCommand cmd = new CreateNotificationCommand(
                    admin.getId(),
                    organizationId,
                    eventType,
                    NotificationCategory.CLAIM,
                    NotificationSeverity.INFO,
                    title,
                    body,
                    recipientDeepLink,
                    dedup,
                    title,
                    emailTemplate,
                    recipientVars,
                    slackDeliveryEnabled);
            notificationService.createIfAbsent(cmd).ifPresentOrElse(
                    id -> {
                        notificationDispatcher.dispatchDeliveriesFor(id);
                        log.info("claims_notification_emit event={} notificationId={} recipientId={} slackIncluded={}",
                                eventType, id, admin.getId(),
                                slackDeliveryEnabled == null || Boolean.TRUE.equals(slackDeliveryEnabled));
                    },
                    () -> log.debug("claims_notification_dedup event={} dedupKey={}", eventType, dedup));
        }
    }

    private String deepLinkForRecipient(Claim claim, AdminUser recipient) {
        if (claim == null || claim.getId() == null) {
            return portalBase() + "/hr/claims";
        }
        String base = portalBase();
        if (isPlatformAudienceRole(recipient != null ? recipient.getRole() : null)) {
            return base + "/admin/claims/" + claim.getId();
        }
        return base + "/hr/claims/" + claim.getId();
    }

    private boolean isPlatformAudienceRole(String role) {
        String normalized = normalizeRole(role);
        return "VIMA_ADMIN".equals(normalized)
                || "ADMIN".equals(normalized)
                || "SUPER_ADMIN".equals(normalized);
    }

    private String normalizeRole(String role) {
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

    private static void applyHrClaimRecipientEmailVars(
            Map<String, Object> recipientVars,
            AdminUser admin,
            String body,
            NotificationEventType eventType) {
        recipientVars.put("message", body != null ? body : "");
        recipientVars.put("recipientName", resolveAdminRecipientLabel(admin));
        applyHrClaimStatusBadge(recipientVars, eventType);
    }

    private static String resolveAdminRecipientLabel(AdminUser admin) {
        if (admin == null) {
            return "there";
        }
        if (admin.getFullName() != null && !admin.getFullName().isBlank()) {
            return admin.getFullName().trim();
        }
        if (admin.getUsername() != null && !admin.getUsername().isBlank()) {
            return admin.getUsername().trim();
        }
        return "there";
    }

    private static void applyHrClaimStatusBadge(Map<String, Object> vars, NotificationEventType eventType) {
        if (eventType == null) {
            vars.put("statusBadge", "Update");
            vars.put("statusBadgeColor", "#e2e8f0");
            return;
        }
        switch (eventType) {
            case EMPLOYEE_CLAIM_SUBMITTED -> {
                vars.put("statusBadge", "Pending review");
                vars.put("statusBadgeColor", "#dbeafe");
            }
            case EMPLOYEE_CLAIM_APPROVED -> {
                vars.put("statusBadge", "Approved");
                vars.put("statusBadgeColor", "#dcfce7");
            }
            case EMPLOYEE_CLAIM_REJECTED -> {
                vars.put("statusBadge", "Rejected");
                vars.put("statusBadgeColor", "#fee2e2");
            }
            case EMPLOYEE_CLAIM_SETTLED -> {
                vars.put("statusBadge", "Settled");
                vars.put("statusBadgeColor", "#dcfce7");
            }
            case EMPLOYEE_CLAIM_QUERY_RAISED -> {
                vars.put("statusBadge", "Query raised");
                vars.put("statusBadgeColor", "#fef3c7");
            }
            case EMPLOYEE_CLAIM_QUERY_RESPONDED -> {
                vars.put("statusBadge", "Query responded");
                vars.put("statusBadgeColor", "#dbeafe");
            }
            case EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED -> {
                vars.put("statusBadge", "Response submitted");
                vars.put("statusBadgeColor", "#fef3c7");
            }
            default -> {
                vars.put("statusBadge", "Update");
                vars.put("statusBadgeColor", "#e2e8f0");
            }
        }
    }

    private String portalBase() {
        if (portalUrlOverride != null && !portalUrlOverride.isBlank()) {
            return portalUrlOverride.trim().replaceAll("/$", "");
        }
        return appBaseUrl != null ? appBaseUrl.trim().replaceAll("/$", "") : "";
    }

    private String resolveFromName(Claim claim, String fromName) {
        if (fromName != null && !fromName.isBlank()) {
            return fromName;
        }
        if (claim.getEmployee() != null && claim.getEmployee().getFullName() != null && !claim.getEmployee().getFullName().isBlank()) {
            return claim.getEmployee().getFullName();
        }
        return "System";
    }

    private String resolveOrganizationName(Claim claim, String actorOrganizationName, String actorRole) {
        String normalizedRole = actorRole != null ? actorRole.trim().toUpperCase() : "";
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }
        if ("VIMA_ADMIN".equals(normalizedRole)) {
            return "Vima";
        }
        if (actorOrganizationName != null && !actorOrganizationName.isBlank()) {
            return actorOrganizationName;
        }
        if (claim.getOrganization() != null && claim.getOrganization().getOrganizationName() != null
                && !claim.getOrganization().getOrganizationName().isBlank()) {
            return claim.getOrganization().getOrganizationName();
        }
        return "Organization";
    }
}
