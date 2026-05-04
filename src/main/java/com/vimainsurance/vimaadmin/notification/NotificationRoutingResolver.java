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

@Component
@RequiredArgsConstructor
public class NotificationRoutingResolver {

    private static final List<String> VIMA_PLATFORM_ROLES = List.of("SUPER_ADMIN", "ADMIN", "VIMA_ADMIN");

    private final IAdminUserRepository adminUserRepository;

    public List<AdminUser> resolveRecipients(NotificationEventType eventType, UUID organizationId) {
        return switch (eventType) {
            case ENDORSEMENT_UPLOADED -> findActiveByRoles(VIMA_PLATFORM_ROLES);
            case ENROLLMENT_ALL_SUBMITTED, ENDORSEMENT_COMPLETED -> findHrAdminsForOrganization(organizationId);
        };
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
        return adminUserRepository.findByOrganization_OrganizationId(organizationId).stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .filter(u -> "HR_ADMIN".equals(u.getRole()))
                .toList();
    }
}
