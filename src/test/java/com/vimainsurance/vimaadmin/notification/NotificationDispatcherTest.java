package com.vimainsurance.vimaadmin.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.dto.EmailResponse;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.entity.NotificationDelivery;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationChannelKind;
import com.vimainsurance.vimaadmin.notification.enums.NotificationDeliveryStatus;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.notification.repository.IAdminNotificationRepository;
import com.vimainsurance.vimaadmin.notification.repository.INotificationDeliveryRepository;
import com.vimainsurance.vimaadmin.notification.slack.SlackChannel;
import com.vimainsurance.vimaadmin.notification.slack.SlackChannelRouter;
import com.vimainsurance.vimaadmin.service.IEmailService;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    private static final String TEST_URL = "https://hooks.slack.com/services/test-only";
    private static final String CLAIMS_URL = "https://hooks.slack.com/services/claims";

    @Mock
    private IAdminNotificationRepository notificationRepository;
    @Mock
    private INotificationDeliveryRepository deliveryRepository;
    @Mock
    private NotificationsFeatureGate notificationsFeatureGate;
    @Mock
    private IEmailService emailService;
    @Mock
    private NotificationSlackWebhookClient slackWebhookClient;
    @Mock
    private SlackChannelRouter slackChannelRouter;

    private NotificationDispatcher dispatcher;
    private NotificationsProperties notificationsProperties;

    @BeforeEach
    void setUp() {
        notificationsProperties = new NotificationsProperties();
        notificationsProperties.setRetryMaxAttempts(5);
        notificationsProperties.setRetryInitialDelayMs(1000L);
        notificationsProperties.setRetryMaxDelayMs(10000L);
        notificationsProperties.setDispatchBatchSize(50);
        dispatcher = new NotificationDispatcher(
                notificationRepository,
                deliveryRepository,
                notificationsFeatureGate,
                notificationsProperties,
                emailService,
                slackWebhookClient,
                slackChannelRouter);
        // Default routing: claim events → SUPPORT_CLAIMS, everything else → REMINDERS.
        lenient().when(slackChannelRouter.channelForEvent(any(NotificationEventType.class)))
                .thenAnswer(inv -> {
                    NotificationEventType type = inv.getArgument(0);
                    return switch (type) {
                        case EMPLOYEE_CLAIM_SUBMITTED,
                                EMPLOYEE_CLAIM_QUERY_RAISED,
                                EMPLOYEE_CLAIM_QUERY_RESPONDED,
                                EMPLOYEE_CLAIM_QUERY_RESPONSE_SUBMITTED,
                                EMPLOYEE_CLAIM_APPROVED,
                                EMPLOYEE_CLAIM_REJECTED,
                                EMPLOYEE_CLAIM_SETTLED -> SlackChannel.SUPPORT_CLAIMS;
                        default -> SlackChannel.REMINDERS;
                    };
                });
    }

    @Test
    void endorsementCompleted_postsToRoutedReminderUrl() {
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_COMPLETED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackChannelRouter.resolveUrl(SlackChannel.REMINDERS)).thenReturn(Optional.of(TEST_URL));
        when(slackWebhookClient.postMessageToWebhookUrl(expectedText(notification), TEST_URL)).thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient).postMessageToWebhookUrl(expectedText(notification), TEST_URL);
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void endorsementUploaded_usesRouterAndNotEnvironmentFallbacks() {
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_UPLOADED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackChannelRouter.resolveUrl(SlackChannel.REMINDERS)).thenReturn(Optional.of(TEST_URL));
        when(slackWebhookClient.postMessageToWebhookUrl(expectedText(notification), TEST_URL)).thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient).postMessageToWebhookUrl(expectedText(notification), TEST_URL);
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void claimEvent_routesToSupportClaimsChannel() {
        AdminNotification notification = slackNotification(NotificationEventType.EMPLOYEE_CLAIM_SUBMITTED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackChannelRouter.resolveUrl(SlackChannel.SUPPORT_CLAIMS)).thenReturn(Optional.of(CLAIMS_URL));
        when(slackWebhookClient.postMessageToWebhookUrl(expectedText(notification), CLAIMS_URL)).thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient).postMessageToWebhookUrl(expectedText(notification), CLAIMS_URL);
        verify(slackChannelRouter, never()).resolveUrl(SlackChannel.REMINDERS);
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void routerEmpty_marksDeliverySkippedAndDoesNotPost() {
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_UPLOADED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackChannelRouter.resolveUrl(SlackChannel.REMINDERS)).thenReturn(Optional.empty());

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient, never()).postMessageToWebhookUrl(any(), any());
        assertEquals(NotificationDeliveryStatus.SKIPPED, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void emailDisabled_forAllRecipients_onClaimCategory() {
        // CLAIM events are Slack-only by design; no email for HR or VIMA roles.
        AdminNotification notification = emailNotification(NotificationEventType.EMPLOYEE_CLAIM_SUBMITTED, "HR_ADMIN");
        notification.setCategory(NotificationCategory.CLAIM);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(emailService, never()).sendTemplateEmail(any());
        assertEquals(NotificationDeliveryStatus.SKIPPED, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void emailDisabled_forVimaPlatformAdmin_onEnrollmentCategory_superAdmin() {
        AdminNotification notification = emailNotification(NotificationEventType.ENROLLMENT_WINDOW_CLOSED, "SUPER_ADMIN");
        notification.setCategory(NotificationCategory.ENROLLMENT);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(emailService, never()).sendTemplateEmail(any());
        assertEquals(NotificationDeliveryStatus.SKIPPED, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void emailDisabled_forVimaPlatformAdmin_onEnrollmentCategory_vimaAdmin() {
        AdminNotification notification = emailNotification(NotificationEventType.ENROLLMENT_ALL_SUBMITTED, "VIMA_ADMIN");
        notification.setCategory(NotificationCategory.ENROLLMENT);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(emailService, never()).sendTemplateEmail(any());
        assertEquals(NotificationDeliveryStatus.SKIPPED, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void emailStillEnabled_forHrAdminOnEnrollmentCategory() {
        AdminNotification notification = emailNotification(NotificationEventType.ENROLLMENT_ALL_SUBMITTED, "HR_ADMIN");
        notification.setCategory(NotificationCategory.ENROLLMENT);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(emailService.sendTemplateEmail(any())).thenReturn(EmailResponse.builder().success(true).build());

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(emailService).sendTemplateEmail(any());
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void emailStillEnabled_forVimaAdminOnEndorsementCategory() {
        // ENDORSEMENT is intentionally NOT in the platform-admin suppression set:
        // ENDORSEMENT_UPLOADED is scoped to VIMA_ADMIN, and that role must receive the email.
        AdminNotification notification = emailNotification(NotificationEventType.ENDORSEMENT_UPLOADED, "VIMA_ADMIN");
        notification.setCategory(NotificationCategory.ENDORSEMENT);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(emailService.sendTemplateEmail(any())).thenReturn(EmailResponse.builder().success(true).build());

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(emailService).sendTemplateEmail(any());
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void emailStillEnabled_forVimaAdminOnSystemCategory() {
        AdminNotification notification = emailNotification(NotificationEventType.ENDORSEMENT_COMPLETED, "VIMA_ADMIN");
        notification.setCategory(NotificationCategory.SYSTEM);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(emailService.sendTemplateEmail(any())).thenReturn(EmailResponse.builder().success(true).build());

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(emailService).sendTemplateEmail(any());
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    private static AdminNotification slackNotification(NotificationEventType type) {
        AdminUser recipient = new AdminUser();
        recipient.setId(UUID.randomUUID());
        recipient.setEmail("ops@example.com");

        AdminNotification n = new AdminNotification();
        n.setId(UUID.randomUUID());
        n.setRecipient(recipient);
        n.setEventType(type);
        n.setTitle("Endorsement completed");
        n.setBody("Batch is completed");
        n.setDeepLinkUrl("https://portal.example.com/endorsements/1");

        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotification(n);
        delivery.setChannel(NotificationChannelKind.SLACK);
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        List<NotificationDelivery> deliveries = new ArrayList<>();
        deliveries.add(delivery);
        n.setDeliveries(deliveries);
        return n;
    }

    private static AdminNotification emailNotification(NotificationEventType type, String role) {
        AdminUser recipient = new AdminUser();
        recipient.setId(UUID.randomUUID());
        recipient.setEmail("notify@example.com");
        recipient.setRole(role);

        AdminNotification n = new AdminNotification();
        n.setId(UUID.randomUUID());
        n.setRecipient(recipient);
        n.setEventType(type);
        n.setCategory(NotificationCategory.ENDORSEMENT);
        n.setTitle("Sample Notification");
        n.setBody("Sample body");
        n.setDeepLinkUrl("https://portal.example.com/notifications/1");
        n.setEmailTemplate("email/notification-endorsement-completed");

        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotification(n);
        delivery.setChannel(NotificationChannelKind.EMAIL);
        delivery.setStatus(NotificationDeliveryStatus.PENDING);
        List<NotificationDelivery> deliveries = new ArrayList<>();
        deliveries.add(delivery);
        n.setDeliveries(deliveries);
        return n;
    }

    private static String expectedText(AdminNotification n) {
        return "*" + n.getTitle() + "*\n" + n.getBody() + "\n" + n.getDeepLinkUrl();
    }
}
