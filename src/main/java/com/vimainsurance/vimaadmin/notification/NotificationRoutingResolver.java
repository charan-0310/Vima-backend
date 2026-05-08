package com.vimainsurance.vimaadmin.notification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRoutingResolver {

    /** Include Keycloak-style {@code ROLE_*} values stored in some environments. */
    private static final List<String> VIMA_PLATFORM_ROLES = List.of(
            "SUPER_ADMIN", "ADMIN", "VIMA_ADMIN", "SALES_ADMIN",
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_VIMA_ADMIN", "ROLE_SALES_ADMIN");

    private final IAdminUserRepository adminUserRepository;
    private final NotificationsProperties notificationsProperties;

    public List<AdminUser> resolveRecipients(NotificationEventType eventType, UUID organizationId) {
        return switch (eventType) {
            case ENDORSEMENT_UPLOADED -> findActiveByRoles(VIMA_PLATFORM_ROLES);
            case ENROLLMENT_ALL_SUBMITTED,
                    ENDORSEMENT_COMPLETED,
                    ENROLLMENT_WINDOW_OPENED,
                    ENROLLMENT_WINDOW_CLOSING_SOON,
                    ENROLLMENT_WINDOW_CLOSED -> findHrAdminsForOrganization(organizationId);
            case EMPLOYEE_CLAIM_SUBMITTED,
                    EMPLOYEE_CLAIM_QUERY_RAISED,
                    EMPLOYEE_CLAIM_QUERY_RESPONDED,
                    EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED,
                    EMPLOYEE_CLAIM_APPROVED,
                    EMPLOYEE_CLAIM_REJECTED,
                    EMPLOYEE_CLAIM_SETTLED -> resolveClaimsTeamRecipients(organizationId);
            case ENROLLMENT_SUBMISSION_APPROVED -> List.of();
        };
    }

    /**
     * Enrollment approval notifications target the approving HR user plus VIMA platform admins.
     * This keeps HR context while ensuring ops visibility in the Vima admin portal.
     */
    public List<AdminUser> resolveEnrollmentSubmissionApprovedRecipients(UUID organizationId, UUID reviewerAdminUserId) {
        LinkedHashSet<UUID> seen = new LinkedHashSet<>();
        List<AdminUser> out = new ArrayList<>();
        if (reviewerAdminUserId != null) {
            AdminUser reviewer = adminUserRepository.findById(reviewerAdminUserId).orElse(null);
            boolean reviewerEligible = reviewer != null
                    && Boolean.TRUE.equals(reviewer.getIsActive())
                    && isHrAdminRole(reviewer.getRole());
            if (reviewerEligible
                    && organizationId != null
                    && reviewer.getOrganization() != null
                    && reviewer.getOrganization().getOrganizationId() != null
                    && !organizationId.equals(reviewer.getOrganization().getOrganizationId())) {
                reviewerEligible = false;
                log.warn("notification_routing_enrollment_approval_reviewer_skip reviewerId={} reason=org_mismatch reviewerOrgId={} eventOrgId={}",
                        reviewerAdminUserId, reviewer.getOrganization().getOrganizationId(), organizationId);
            }
            if (reviewerEligible && reviewer.getId() != null && seen.add(reviewer.getId())) {
                out.add(reviewer);
            } else if (!reviewerEligible) {
                log.warn("notification_routing_enrollment_approval_reviewer_skip reviewerId={} reason=missing_or_not_active_hr",
                        reviewerAdminUserId);
            }
        } else {
            log.warn("notification_routing_enrollment_approval_reviewer_skip reason=missing_reviewer orgId={}", organizationId);
        }
        for (AdminUser u : findActiveByRoles(VIMA_PLATFORM_ROLES)) {
            if (u != null && u.getId() != null && seen.add(u.getId())) {
                out.add(u);
            }
        }
        return out;
    }

    /**
     * HR admins for the org (in-app bell + email) plus VIMA platform admins, deduplicated by user id.
     * Used for enrollment-wide signals that should appear in both HR and ops portals without duplicate Slack posts.
     */
    public List<AdminUser> resolveHrAndVimaPlatformRecipients(UUID organizationId) {
        LinkedHashSet<UUID> seen = new LinkedHashSet<>();
        List<AdminUser> out = new ArrayList<>();
        for (AdminUser u : findHrAdminsForOrganization(organizationId)) {
            if (u != null && u.getId() != null && seen.add(u.getId())) {
                out.add(u);
            }
        }
        for (AdminUser u : findActiveByRoles(VIMA_PLATFORM_ROLES)) {
            if (u != null && u.getId() != null && seen.add(u.getId())) {
                out.add(u);
            }
        }
        return out;
    }

    /**
     * Claims team audience: organization HR admins + VIMA platform admins.
     */
    public List<AdminUser> resolveClaimsTeamRecipients(UUID organizationId) {
        return resolveHrAndVimaPlatformRecipients(organizationId);
    }

    /**
     * Completion notifications are creator-targeted: notify only the HR uploader when available.
     * Falls back to organization-wide HR admins only when uploader context is missing or no longer active.
     */
    public List<AdminUser> resolveEndorsementCompletedRecipients(UUID organizationId, UUID uploadedByAdminUserId) {
        if (uploadedByAdminUserId != null) {
            AdminUser uploader = adminUserRepository.findById(uploadedByAdminUserId).orElse(null);
            if (uploader != null
                    && Boolean.TRUE.equals(uploader.getIsActive())
                    && isHrAdminRole(uploader.getRole())) {
                return List.of(uploader);
            }
            log.warn("notification_routing_completion_creator_miss uploaderId={} reason=missing_or_not_active_hr", uploadedByAdminUserId);
        }
        return findHrAdminsForOrganization(organizationId);
    }

    private List<AdminUser> findActiveByRoles(List<String> roles) {
        // Role values in admin_users are not always consistent (e.g. ROLE_VIMA_ADMIN, vima_admin, extra spaces).
        // Resolve by normalized role so role-scoped notifications are shared across the full intended audience.
        Set<String> wanted = new LinkedHashSet<>();
        for (String r : roles) {
            String n = normalizeRole(r);
            if (n != null) {
                wanted.add(n);
            }
        }
        List<AdminUser> found = adminUserRepository.findByIsActiveTrue().stream()
                .filter(u -> wanted.contains(normalizeRole(u.getRole())))
                .toList();
        Set<UUID> seen = new LinkedHashSet<>();
        List<AdminUser> out = new ArrayList<>();
        for (AdminUser u : found) {
            if (u.getId() != null && seen.add(u.getId())) {
                out.add(u);
            }
        }
        return out;
    }

    private List<AdminUser> findHrAdminsForOrganization(UUID organizationId) {
        if (organizationId == null) {
            return List.of();
        }
        LinkedHashSet<UUID> seen = new LinkedHashSet<>();
        List<AdminUser> out = new ArrayList<>();
        for (AdminUser u : adminUserRepository.findByOrganization_OrganizationId(organizationId)) {
            if (Boolean.TRUE.equals(u.getIsActive())
                    && isHrAdminRole(u.getRole())
                    && u.getId() != null
                    && seen.add(u.getId())) {
                out.add(u);
            }
        }
        if (notificationsProperties.isIncludeUnscopedHrAdmins()) {
            for (AdminUser u : findActiveByRoles(List.of("HR_ADMIN", "ROLE_HR_ADMIN"))) {
                if (u == null
                        || !Boolean.TRUE.equals(u.getIsActive())
                        || !isHrAdminRole(u.getRole())
                        || u.getId() == null
                        || u.getOrganization() != null) {
                    continue;
                }
                if (seen.add(u.getId())) {
                    out.add(u);
                    log.debug("notification_routing_hr_unscoped orgId={} userId={}", organizationId, u.getId());
                }
            }
        }
        if (!out.isEmpty()) {
            return out;
        }
        // Backward compatibility: some environments do not populate admin_users.organization_id for HR users.
        List<AdminUser> fallback = findActiveByRoles(List.of("HR_ADMIN", "ROLE_HR_ADMIN"));
        if (!fallback.isEmpty()) {
            log.warn("notification_routing_hr_fallback orgId={} reason=no_org_linked_hr_admins fallbackCount={}",
                    organizationId, fallback.size());
        }
        return fallback;
    }

    static boolean isHrAdminRole(String role) {
        String normalized = normalizeRole(role);
        return "HR_ADMIN".equals(normalized);
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        // Accept common storage variants like "VIMA ADMIN" / "VIMA-ADMIN" / "VIMA__ADMIN".
        normalized = normalized.replace('-', '_').replace(' ', '_');
        while (normalized.contains("__")) {
            normalized = normalized.replace("__", "_");
        }
        if (normalized.endsWith("_GROUP")) {
            normalized = normalized.substring(0, normalized.length() - "_GROUP".length());
        }
        return normalized;
    }
}
