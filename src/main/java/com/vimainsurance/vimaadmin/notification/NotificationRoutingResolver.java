package com.vimainsurance.vimaadmin.notification;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.entity.AdminUser;
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

    public List<AdminUser> resolveRecipients(NotificationEventType eventType, UUID organizationId) {
        return switch (eventType) {
            case ENDORSEMENT_UPLOADED -> findActiveByRoles(VIMA_PLATFORM_ROLES);
            case ENROLLMENT_ALL_SUBMITTED, ENDORSEMENT_COMPLETED -> findHrAdminsForOrganization(organizationId);
        };
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
        List<AdminUser> found = adminUserRepository.findByRoleInAndIsActiveTrue(roles);
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
        List<AdminUser> scoped = adminUserRepository.findByOrganization_OrganizationId(organizationId).stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .filter(u -> isHrAdminRole(u.getRole()))
                .toList();
        if (!scoped.isEmpty()) {
            return scoped;
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
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return "HR_ADMIN".equals(normalized);
    }
}
