package com.vimainsurance.vimaadmin.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationRoutingResolverTest {

    @Mock
    private IAdminUserRepository adminUserRepository;

    @InjectMocks
    private NotificationRoutingResolver resolver;

    @Test
    void endorsementUploaded_targetsPlatformRoles() {
        AdminUser vima = new AdminUser();
        vima.setId(UUID.randomUUID());
        vima.setRole("VIMA_ADMIN");
        when(adminUserRepository.findByRoleInAndIsActiveTrue(List.of("SUPER_ADMIN", "ADMIN", "VIMA_ADMIN")))
                .thenReturn(List.of(vima));
        List<AdminUser> out = resolver.resolveRecipients(NotificationEventType.ENDORSEMENT_UPLOADED, null);
        assertEquals(1, out.size());
        assertEquals(vima.getId(), out.get(0).getId());
    }

    @Test
    void enrollmentAllSubmitted_targetsHrForOrg() {
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setOrganizationId(orgId);
        AdminUser hr = new AdminUser();
        hr.setId(UUID.randomUUID());
        hr.setRole("HR_ADMIN");
        hr.setIsActive(true);
        hr.setOrganization(org);
        AdminUser other = new AdminUser();
        other.setId(UUID.randomUUID());
        other.setRole("VIMA_ADMIN");
        other.setOrganization(org);
        when(adminUserRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of(hr, other));
        List<AdminUser> out = resolver.resolveRecipients(NotificationEventType.ENROLLMENT_ALL_SUBMITTED, orgId);
        assertEquals(1, out.size());
        assertTrue(out.stream().allMatch(u -> "HR_ADMIN".equals(u.getRole())));
    }
}
