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

/**
 * Resolves {@link com.vimainsurance.vimaadmin.entity.AdminUser} recipients for unified notifications.
 * Organization-scoped HR targeting uses {@code admin_users.organization_id}. Most org-specific events require that
 * link; endorsement completion also targets the uploading HR admin when they are missing that link (see
 * {@link #resolveEndorsementCompletedRecipients(UUID, UUID)}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRoutingResolver {

    /** Include Keycloak-style {@code ROLE_*} values stored in some environments. */
    private static final List<String> VIMA_PLATFORM_ROLES = List.of(
            "SUPER_ADMIN", "ADMIN", "VIMA_ADMIN", "SALES_ADMIN",
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_VIMA_ADMIN", "ROLE_SALES_ADMIN");

    /** Narrower audience for endorsement upload notifications — only VIMA_ADMIN role. */
    private static final List<String> VIMA_ADMIN_ONLY_ROLES = List.of(
            "VIMA_ADMIN", "ROLE_VIMA_ADMIN");

    private final IAdminUserRepository adminUserRepository;
    private final NotificationsProperties notificationsProperties;

    public List<AdminUser> resolveRecipients(NotificationEventType eventType, UUID organizationId) {
        return switch (eventType) {
            case ENDORSEMENT_UPLOADED -> findActiveByRoles(VIMA_ADMIN_ONLY_ROLES);
            case ENDORSEMENT_COMPLETED -> resolveEndorsementCompletedRecipients(organizationId, null);
            case ENROLLMENT_ALL_SUBMITTED,
                    ENROLLMENT_WINDOW_OPENED, // event retired; no emitter — kept here only so the switch stays exhaustive
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
     * Endorsement completion: notify every active HR admin linked to the endorsement organization
     * ({@code admin_users.organization_id}), plus the HR user who uploaded the endorsement when they are not in that
     * query (e.g. {@code organization_id} not set yet). Uploader is skipped if they are linked to a different org.
     */
    public List<AdminUser> resolveEndorsementCompletedRecipients(UUID organizationId, UUID uploadedByAdminUserId) {
        LinkedHashSet<UUID> seen = new LinkedHashSet<>();
        List<AdminUser> out = new ArrayList<>();
        for (AdminUser u : findHrAdminsForOrganization(organizationId)) {
            if (u != null && u.getId() != null && seen.add(u.getId())) {
                out.add(u);
            }
        }
        if (uploadedByAdminUserId == null || seen.contains(uploadedByAdminUserId)) {
            return out;
        }
        adminUserRepository.findWithOrganizationById(uploadedByAdminUserId).ifPresent(uploader -> {
            if (!Boolean.TRUE.equals(uploader.getIsActive()) || !isHrAdminRole(uploader.getRole())) {
                return;
            }
            UUID uploaderOrgId = uploader.getOrganization() != null && uploader.getOrganization().getOrganizationId() != null
                    ? uploader.getOrganization().getOrganizationId()
                    : null;
            if (uploaderOrgId != null && !organizationId.equals(uploaderOrgId)) {
                log.warn(
                        "notification_routing_endorsement_completed_skip_uploader_org_mismatch uploaderId={} uploaderOrgId={} endorsementOrgId={}",
                        uploader.getId(), uploaderOrgId, organizationId);
                return;
            }
            if (uploader.getId() != null && seen.add(uploader.getId())) {
                out.add(uploader);
                if (uploaderOrgId == null) {
                    log.info(
                            "notification_routing_endorsement_completed_include_uploader_missing_org_link uploaderId={} endorsementOrgId={}",
                            uploader.getId(), organizationId);
                }
            }
        });
        return out;
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
        if (!out.isEmpty()) {
            return out;
        }
        if (notificationsProperties.isAllowGlobalHrRecipientFallback()) {
            List<AdminUser> fallback = findActiveByRoles(List.of("HR_ADMIN", "ROLE_HR_ADMIN"));
            if (!fallback.isEmpty()) {
                log.warn("notification_routing_hr_fallback orgId={} reason=no_org_linked_hr_admins allowGlobalHrRecipientFallback=true fallbackCount={}",
                        organizationId, fallback.size());
            }
            return fallback;
        }
        log.warn("notification_routing_no_org_hr_admins orgId={} hint=set admin_users.organization_id for HR_ADMIN users; "
                + "notifications.allow-global-hr-recipient-fallback is false",
                organizationId);
        return List.of();
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
