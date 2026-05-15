package com.vimainsurance.vimaadmin.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.notification.dto.CreateNotificationCommand;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.notification.enums.NotificationSeverity;
import com.vimainsurance.vimaadmin.notification.repository.IAdminNotificationRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private IAdminNotificationRepository notificationRepository;
    @Mock
    private IAdminUserRepository adminUserRepository;
    @Mock
    private IOrganizationRepository organizationRepository;
    @Mock
    private NotificationsFeatureGate notificationsFeatureGate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void create_skippedWhenFlagDisabled() {
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(false);
        CreateNotificationCommand cmd = sampleCommand(UUID.randomUUID());
        Optional<UUID> id = notificationService.createIfAbsent(cmd);
        assertTrue(id.isEmpty());
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_skippedWhenDedupExists() {
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.existsByDedupKey("dedup-1")).thenReturn(true);
        CreateNotificationCommand cmd = sampleCommand(UUID.randomUUID());
        Optional<UUID> id = notificationService.createIfAbsent(cmd);
        assertTrue(id.isEmpty());
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_skippedWhenRecipientMissing() {
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.existsByDedupKey("dedup-1")).thenReturn(false);
        UUID rid = UUID.randomUUID();
        when(adminUserRepository.findById(rid)).thenReturn(Optional.empty());
        CreateNotificationCommand cmd = sampleCommand(rid);
        Optional<UUID> id = notificationService.createIfAbsent(cmd);
        assertTrue(id.isEmpty());
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    private static CreateNotificationCommand sampleCommand(UUID recipientId) {
        return new CreateNotificationCommand(
                recipientId,
                null,
                NotificationEventType.ENDORSEMENT_COMPLETED,
                NotificationCategory.ENDORSEMENT,
                NotificationSeverity.INFO,
                "t",
                "b",
                "http://x",
                "dedup-1",
                "subj",
                "email/notification-endorsement-completed",
                Map.of(
                        "organizationName", "Org",
                        "recipientName", "HR",
                        "message", "An endorsement for Org has completed.",
                        "totalEmployees", 0,
                        "totalDependents", 0,
                        "uploadedByName", "Vima Admin",
                        "deepLinkUrl", "http://x",
                        "title", "Endorsement completed — Org"),
                null);
    }
}
