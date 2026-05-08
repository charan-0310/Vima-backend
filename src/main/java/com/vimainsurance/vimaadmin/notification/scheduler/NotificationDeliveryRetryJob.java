package com.vimainsurance.vimaadmin.notification.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.notification.NotificationDispatcher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryRetryJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryRetryJob.class);

    private final NotificationDispatcher notificationDispatcher;

    @Scheduled(cron = "${notifications.retry.cron-expression:0 */2 * * * *}", zone = "UTC")
    public void retryDueDeliveries() {
        try {
            int n = notificationDispatcher.dispatchDueBatch();
            if (n > 0) {
                log.info("[correlationId:{}] notification_retry_job processed={}", MDC.get("correlationId"), n);
            }
        } catch (Exception e) {
            log.warn("[correlationId:{}] notification_retry_job failed: {}", MDC.get("correlationId"), e.getMessage(), e);
        }
    }
}
