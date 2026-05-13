package com.vimainsurance.vimaadmin.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.core.env.Environment;

import com.vimainsurance.vimaadmin.dto.EmailResponse;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.entity.NotificationDelivery;
import com.vimainsurance.vimaadmin.notification.enums.NotificationChannelKind;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationDeliveryStatus;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;
import com.vimainsurance.vimaadmin.notification.repository.IAdminNotificationRepository;
import com.vimainsurance.vimaadmin.notification.repository.INotificationDeliveryRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

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
    private Environment environment;

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
                environment,
                emailService,
                slackWebhookClient);
    }

    @Test
    void endorsementCompleted_usesWebhookPathLikeUploadFlow() {
        notificationsProperties.setSlackBotToken("xoxb-test-token");
        notificationsProperties.setSlackChannelId("C09PR4VC0DR");
        notificationsProperties.setSlackWebhookUrl("https://hooks.slack.com/services/test");
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_COMPLETED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackWebhookClient.postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/test"))
                .thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient, never()).postMessageToChannel(any(), any());
        verify(slackWebhookClient).postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/test");
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void endorsementCompleted_usesWebhookEvenWhenBotRouteUnavailable() {
        notificationsProperties.setSlackBotToken("");
        notificationsProperties.setSlackChannelId("C09PR4VC0DR");
        notificationsProperties.setSlackWebhookUrl("https://hooks.slack.com/services/test");
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_COMPLETED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackWebhookClient.postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/test"))
                .thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient, never()).postMessageToChannel(any(), any());
        verify(slackWebhookClient).postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/test");
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void nonEndorsementEvent_keepsWebhookPathEvenWhenBotConfigured() {
        notificationsProperties.setSlackBotToken("xoxb-test-token");
        notificationsProperties.setSlackChannelId("C09PR4VC0DR");
        notificationsProperties.setSlackWebhookUrl("https://hooks.slack.com/services/test");
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_UPLOADED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackWebhookClient.postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/test"))
                .thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient, never()).postMessageToChannel(any(), any());
        verify(slackWebhookClient).postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/test");
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void endorsementUploaded_prefersUnifiedWebhookWhenReminderEnvAlsoSet() {
        notificationsProperties.setSlackWebhookUrl("https://hooks.slack.com/services/unified-hook");
        when(environment.getProperty("slack.reminder.channel.url", "")).thenReturn("https://hooks.slack.com/services/reminder-hook");
        when(environment.getProperty("slack.webhook.url", "")).thenReturn("");
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_UPLOADED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackWebhookClient.postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/unified-hook"))
                .thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient).postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/unified-hook");
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void endorsementUploaded_fallsBackToReminderWhenUnifiedUnset() {
        notificationsProperties.setSlackWebhookUrl("");
        when(environment.getProperty("slack.reminder.channel.url", "")).thenReturn("https://hooks.slack.com/services/reminder-only");
        when(environment.getProperty("slack.webhook.url", "")).thenReturn("");
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_UPLOADED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackWebhookClient.postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/reminder-only"))
                .thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient).postMessageToWebhookUrl(expectedText(notification), "https://hooks.slack.com/services/reminder-only");
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void emailTemporarilyDisabled_forVimaAdminOnConfiguredCategories() {
        AdminNotification notification = emailNotification(NotificationEventType.ENDORSEMENT_COMPLETED, "VIMA_ADMIN");
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(emailService, never()).sendTemplateEmail(any());
        assertEquals(NotificationDeliveryStatus.SKIPPED, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void emailStillEnabled_forHrAdminOnConfiguredEvents() {
        AdminNotification notification = emailNotification(NotificationEventType.ENDORSEMENT_COMPLETED, "HR_ADMIN");
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
