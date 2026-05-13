package com.vimainsurance.vimaadmin.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.notification.dto.AdminNotificationPageResponseDto;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.repository.IAdminNotificationRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

@ExtendWith(MockitoExtension.class)
class AdminNotificationInboxServiceTest {

    @Mock
    private IAdminNotificationRepository notificationRepository;

    @Mock
    private IAdminUserRepository adminUserRepository;

    @Mock
    private JwtUserExtractor jwtUserExtractor;

    @InjectMocks
    private AdminNotificationInboxService inboxService;

    @Test
    void hrAdmin_list_scopesToRecipientAndOrganization() {
        UUID recipientId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        AdminUser hr = hrAdmin(recipientId, orgId);
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(hr));
        when(adminUserRepository.findWithOrganizationById(recipientId)).thenReturn(Optional.of(hr));

        Page<AdminNotification> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(notificationRepository.findInboxForHrAdminByOrganization(
                eq(recipientId), eq(false), isNull(), eq(orgId), any(Pageable.class)))
                .thenReturn(page);

        AdminNotificationPageResponseDto out = inboxService.list(false, null, null, 0, 20);

        assertEquals(0, out.getContent().size());
        assertEquals(0, out.getTotalElements());
        verify(notificationRepository).findInboxForHrAdminByOrganization(
                eq(recipientId), eq(false), isNull(), eq(orgId), any(Pageable.class));
        verify(notificationRepository, never()).findInboxByReceiverEmail(any(), anyBoolean(), any(), any());
    }

    @Test
    void hrAdmin_withoutOrganization_returnsEmptyList() {
        AdminUser hr = new AdminUser();
        hr.setId(UUID.randomUUID());
        hr.setRole("HR_ADMIN");
        hr.setOrganization(null);
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(hr));
        when(adminUserRepository.findWithOrganizationById(hr.getId())).thenReturn(Optional.of(hr));
        when(jwtUserExtractor.getCurrentOrganizations()).thenReturn(List.of());

        AdminNotificationPageResponseDto out = inboxService.list(false, null, null, 0, 20);

        assertTrue(out.getContent().isEmpty());
        assertEquals(0, out.getTotalElements());
        verify(notificationRepository, never()).findInboxForHrAdminByOrganization(any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void hrAdmin_usesJwtOrganizationWhenDbOrgMissing() {
        UUID recipientId = UUID.randomUUID();
        UUID jwtOrgId = UUID.randomUUID();
        AdminUser hr = new AdminUser();
        hr.setId(recipientId);
        hr.setRole("HR_ADMIN");
        hr.setOrganization(null);
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(hr));
        when(adminUserRepository.findWithOrganizationById(recipientId)).thenReturn(Optional.of(hr));
        when(jwtUserExtractor.getCurrentOrganizations()).thenReturn(List.of(jwtOrgId.toString()));

        Page<AdminNotification> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(notificationRepository.findInboxForHrAdminByOrganization(
                eq(recipientId), eq(false), isNull(), eq(jwtOrgId), any(Pageable.class)))
                .thenReturn(page);

        AdminNotificationPageResponseDto out = inboxService.list(false, null, null, 0, 20);

        assertEquals(0, out.getContent().size());
        verify(notificationRepository).findInboxForHrAdminByOrganization(
                eq(recipientId), eq(false), isNull(), eq(jwtOrgId), any(Pageable.class));
    }

    @Test
    void hrAdmin_unreadCount_usesOrganizationScope() {
        UUID recipientId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        AdminUser hr = hrAdmin(recipientId, orgId);
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(hr));
        when(adminUserRepository.findWithOrganizationById(recipientId)).thenReturn(Optional.of(hr));
        when(notificationRepository.countUnreadForHrAdminByOrganization(recipientId, orgId)).thenReturn(3L);

        assertEquals(3L, inboxService.unreadCount(null));
        verify(notificationRepository).countUnreadForHrAdminByOrganization(recipientId, orgId);
        verify(notificationRepository, never()).countUnreadByReceiverEmail(any());
    }

    @Test
    void hrAdmin_markAllRead_scopesToOrganization() {
        UUID recipientId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        AdminUser hr = hrAdmin(recipientId, orgId);
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(hr));
        when(adminUserRepository.findWithOrganizationById(recipientId)).thenReturn(Optional.of(hr));
        when(notificationRepository.markAllReadForHrAdminByOrganization(
                        eq(recipientId), eq(orgId), any(LocalDateTime.class)))
                .thenReturn(2);

        assertEquals(2, inboxService.markAllRead(null));
        verify(notificationRepository).markAllReadForHrAdminByOrganization(
                eq(recipientId), eq(orgId), any(LocalDateTime.class));
        verify(notificationRepository, never()).markAllReadByReceiverEmail(any(), any());
    }

    @Test
    void vimaAudience_jwtOrganizationIds_scopesInboxToThoseOrganizations() {
        UUID org1 = UUID.randomUUID();
        UUID org2 = UUID.randomUUID();
        UUID vimaId = UUID.randomUUID();
        AdminUser vima = vimaAdmin(vimaId);
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(vima));
        when(jwtUserExtractor.getCurrentOrganizations()).thenReturn(List.of(org1.toString(), org2.toString()));
        when(adminUserRepository.findByIsActiveTrue()).thenReturn(List.of(vima));

        Page<AdminNotification> page = new PageImpl<>(List.of(), PageRequest.of(0, 500), 0);
        when(notificationRepository.findInboxByRecipientIdsAndOrganizationIdIn(
                        any(), eq(false), isNull(), eq(List.of(org1, org2)), any(Pageable.class)))
                .thenReturn(page);

        AdminNotificationPageResponseDto out = inboxService.list(false, null, null, 0, 20);

        assertEquals(0, out.getContent().size());
        verify(notificationRepository).findInboxByRecipientIdsAndOrganizationIdIn(
                argThat(ids -> ids != null && ids.contains(vimaId)),
                eq(false),
                isNull(),
                argThat(ids -> ids != null && ids.size() == 2 && ids.contains(org1) && ids.contains(org2)),
                any(Pageable.class));
        verify(notificationRepository, never()).findInboxByRecipientIds(any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void vimaAudience_noJwtOrganizationIds_usesLegacyUnscopedInbox() {
        UUID vimaId = UUID.randomUUID();
        AdminUser vima = vimaAdmin(vimaId);
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(vima));
        when(jwtUserExtractor.getCurrentOrganizations()).thenReturn(List.of());
        when(adminUserRepository.findByIsActiveTrue()).thenReturn(List.of(vima));

        Page<AdminNotification> page = new PageImpl<>(List.of(), PageRequest.of(0, 500), 0);
        when(notificationRepository.findInboxByRecipientIds(
                        any(), eq(false), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        inboxService.list(false, null, null, 0, 20);

        verify(notificationRepository).findInboxByRecipientIds(
                argThat(ids -> ids != null && ids.contains(vimaId)),
                eq(false),
                isNull(),
                isNull(),
                any(Pageable.class));
        verify(notificationRepository, never()).findInboxByRecipientIdsAndOrganizationIdIn(any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void vimaAudience_companyIdNotInJwt_returnsEmptyInbox() {
        UUID org1 = UUID.randomUUID();
        UUID vimaId = UUID.randomUUID();
        AdminUser vima = vimaAdmin(vimaId);
        when(jwtUserExtractor.resolveCurrentAdminUser()).thenReturn(Optional.of(vima));
        when(jwtUserExtractor.getCurrentOrganizations()).thenReturn(List.of(org1.toString()));
        when(adminUserRepository.findByIsActiveTrue()).thenReturn(List.of(vima));

        AdminNotificationPageResponseDto out = inboxService.list(false, null, UUID.randomUUID(), 0, 20);

        assertTrue(out.getContent().isEmpty());
        assertEquals(0, out.getTotalElements());
        verify(notificationRepository, never()).findInboxByRecipientIdsAndOrganizationIdIn(any(), anyBoolean(), any(), any(), any());
        verify(notificationRepository, never()).findInboxByRecipientIds(any(), anyBoolean(), any(), any(), any());
    }

    private static AdminUser vimaAdmin(UUID id) {
        AdminUser u = new AdminUser();
        u.setId(id);
        u.setRole("VIMA_ADMIN");
        return u;
    }

    private static AdminUser hrAdmin(UUID recipientId, UUID orgId) {
        Organization org = new Organization();
        org.setOrganizationId(orgId);
        AdminUser hr = new AdminUser();
        hr.setId(recipientId);
        hr.setRole("HR_ADMIN");
        hr.setOrganization(org);
        return hr;
    }
}
