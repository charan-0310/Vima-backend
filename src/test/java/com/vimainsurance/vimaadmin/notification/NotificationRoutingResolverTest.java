package com.vimainsurance.vimaadmin.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationRoutingResolverTest {

    @Mock
    private IAdminUserRepository adminUserRepository;

    @Mock
    private NotificationsProperties notificationsProperties;

    @InjectMocks
    private NotificationRoutingResolver resolver;

    @Test
    void endorsementUploaded_targetsPlatformRoles() {
        AdminUser vima = new AdminUser();
        vima.setId(UUID.randomUUID());
        vima.setRole("VIMA_ADMIN");
        vima.setIsActive(true);
        when(adminUserRepository.findByIsActiveTrue()).thenReturn(List.of(vima));
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
        assertTrue(out.stream().allMatch(u -> NotificationRoutingResolver.isHrAdminRole(u.getRole())));
    }

    @Test
    void endorsementCompleted_targetsHrWithRolePrefix() {
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setOrganizationId(orgId);
        AdminUser hr = new AdminUser();
        hr.setId(UUID.randomUUID());
        hr.setRole("ROLE_HR_ADMIN");
        hr.setIsActive(true);
        hr.setOrganization(org);
        when(adminUserRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of(hr));
        List<AdminUser> out = resolver.resolveRecipients(NotificationEventType.ENDORSEMENT_COMPLETED, orgId);
        assertEquals(1, out.size());
        assertEquals(hr.getId(), out.get(0).getId());
    }

    @Test
    void endorsementCompleted_notifiesAllOrgLinkedHrAdmins() {
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setOrganizationId(orgId);
        AdminUser hr1 = new AdminUser();
        hr1.setId(UUID.randomUUID());
        hr1.setRole("HR_ADMIN");
        hr1.setIsActive(true);
        hr1.setOrganization(org);
        AdminUser hr2 = new AdminUser();
        hr2.setId(UUID.randomUUID());
        hr2.setRole("HR_ADMIN");
        hr2.setIsActive(true);
        hr2.setOrganization(org);
        when(adminUserRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of(hr1, hr2));

        List<AdminUser> out = resolver.resolveEndorsementCompletedRecipients(orgId, null);

        assertEquals(2, out.size());
        verify(adminUserRepository, never()).findWithOrganizationById(any());
    }

    @Test
    void endorsementCompleted_includesHrUploaderWhenMissingOrgLink() {
        UUID orgId = UUID.randomUUID();
        UUID uploaderId = UUID.randomUUID();
        when(adminUserRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of());
        AdminUser uploader = new AdminUser();
        uploader.setId(uploaderId);
        uploader.setRole("HR_ADMIN");
        uploader.setIsActive(true);
        uploader.setOrganization(null);
        when(adminUserRepository.findWithOrganizationById(uploaderId)).thenReturn(Optional.of(uploader));

        List<AdminUser> out = resolver.resolveEndorsementCompletedRecipients(orgId, uploaderId);

        assertEquals(1, out.size());
        assertEquals(uploaderId, out.get(0).getId());
    }

    @Test
    void endorsementCompleted_skipsHrUploaderWhenOrgMismatchesEndorsement() {
        UUID endorsementOrgId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
        UUID uploaderId = UUID.randomUUID();
        Organization otherOrg = new Organization();
        otherOrg.setOrganizationId(otherOrgId);
        when(adminUserRepository.findByOrganization_OrganizationId(endorsementOrgId)).thenReturn(List.of());
        AdminUser uploader = new AdminUser();
        uploader.setId(uploaderId);
        uploader.setRole("HR_ADMIN");
        uploader.setIsActive(true);
        uploader.setOrganization(otherOrg);
        when(adminUserRepository.findWithOrganizationById(uploaderId)).thenReturn(Optional.of(uploader));

        List<AdminUser> out = resolver.resolveEndorsementCompletedRecipients(endorsementOrgId, uploaderId);

        assertTrue(out.isEmpty());
    }

    @Test
    void endorsementCompleted_noOrgLinkedHr_returnsEmptyWithoutGlobalFallback() {
        UUID orgId = UUID.randomUUID();
        when(adminUserRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of());

        List<AdminUser> out = resolver.resolveRecipients(NotificationEventType.ENDORSEMENT_COMPLETED, orgId);

        assertTrue(out.isEmpty());
        verify(adminUserRepository, never()).findByIsActiveTrue();
    }

    @Test
    void resolveHrAndVimaPlatformRecipients_noOrgHr_stillIncludesPlatformAdmins() {
        UUID orgId = UUID.randomUUID();
        when(adminUserRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of());
        AdminUser vima = new AdminUser();
        vima.setId(UUID.randomUUID());
        vima.setRole("VIMA_ADMIN");
        vima.setIsActive(true);
        when(adminUserRepository.findByIsActiveTrue()).thenReturn(List.of(vima));

        List<AdminUser> out = resolver.resolveHrAndVimaPlatformRecipients(orgId);

        assertEquals(1, out.size());
        assertEquals(vima.getId(), out.get(0).getId());
        verify(adminUserRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void findHrAdmins_allowGlobalFallback_broadcastsAllHrWhenEnabled() {
        when(notificationsProperties.isAllowGlobalHrRecipientFallback()).thenReturn(true);
        UUID orgId = UUID.randomUUID();
        when(adminUserRepository.findByOrganization_OrganizationId(orgId)).thenReturn(List.of());
        AdminUser hrA = new AdminUser();
        hrA.setId(UUID.randomUUID());
        hrA.setRole("HR_ADMIN");
        hrA.setIsActive(true);
        AdminUser hrB = new AdminUser();
        hrB.setId(UUID.randomUUID());
        hrB.setRole("ROLE_HR_ADMIN");
        hrB.setIsActive(true);
        when(adminUserRepository.findByIsActiveTrue()).thenReturn(List.of(hrA, hrB));

        List<AdminUser> out = resolver.resolveRecipients(NotificationEventType.ENDORSEMENT_COMPLETED, orgId);

        assertEquals(2, out.size());
        verify(adminUserRepository).findByIsActiveTrue();
    }
}
