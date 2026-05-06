package com.vimainsurance.vimaadmin.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;
import com.vimainsurance.vimaadmin.notification.entity.AdminNotification;
import com.vimainsurance.vimaadmin.notification.entity.NotificationDelivery;
import com.vimainsurance.vimaadmin.notification.enums.NotificationChannelKind;
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
                slackWebhookClient);
    }

    @Test
    void endorsementCompleted_prefersBotTokenPathWhenConfigured() {
        notificationsProperties.setSlackBotToken("xoxb-test-token");
        notificationsProperties.setSlackChannelId("C09PR4VC0DR");
        notificationsProperties.setSlackWebhookUrl("https://hooks.slack.com/services/test");
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_COMPLETED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackWebhookClient.postMessageToChannel("C09PR4VC0DR", expectedText(notification))).thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient).postMessageToChannel("C09PR4VC0DR", expectedText(notification));
        verify(slackWebhookClient, never()).postMessage(any());
        assertEquals(NotificationDeliveryStatus.SENT, notification.getDeliveries().get(0).getStatus());
    }

    @Test
    void endorsementCompleted_fallsBackToWebhookWhenBotRouteUnavailable() {
        notificationsProperties.setSlackBotToken("");
        notificationsProperties.setSlackChannelId("C09PR4VC0DR");
        notificationsProperties.setSlackWebhookUrl("https://hooks.slack.com/services/test");
        AdminNotification notification = slackNotification(NotificationEventType.ENDORSEMENT_COMPLETED);
        when(notificationsFeatureGate.isNotificationsEnabled()).thenReturn(true);
        when(notificationRepository.findByIdForDispatch(notification.getId())).thenReturn(Optional.of(notification));
        when(slackWebhookClient.postMessage(expectedText(notification))).thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient, never()).postMessageToChannel(any(), any());
        verify(slackWebhookClient).postMessage(expectedText(notification));
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
        when(slackWebhookClient.postMessage(expectedText(notification))).thenReturn(true);

        dispatcher.dispatchDeliveriesFor(notification.getId());

        verify(slackWebhookClient, never()).postMessageToChannel(any(), any());
        verify(slackWebhookClient).postMessage(expectedText(notification));
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
        n.setDeliveries(List.of(delivery));
        return n;
    }

    private static String expectedText(AdminNotification n) {
        return "*" + n.getTitle() + "*\n" + n.getBody() + "\n" + n.getDeepLinkUrl();
    }
}
