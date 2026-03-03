package com.vimainsurance.vimaadmin.scheduler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.audit.ActionSource;
import com.vimainsurance.vimaadmin.service.IEnrollmentInvitation;

/**
 * Scheduler for enrollment reminder emails.
 * Runs daily at 9 AM (configurable): finds invitations with status IN (sent, opened, in_progress)
 * and expires_at &gt; now; applies window config (reminderEnabled, reminderFrequencyDays);
 * sends reminders when last_reminder_at + reminderFrequencyDays &lt;= today, and sends
 * final reminder 1 day before expiry. Updates reminder_count and last_reminder_at.
 */
@Component
@ConditionalOnProperty(
    prefix = "enrollment.reminder-scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ReminderEmailSender {

    private static final Logger logger = LoggerFactory.getLogger(ReminderEmailSender.class);

    @Autowired
    private IEnrollmentInvitation enrollmentInvitationService;

    /**
     * Runs daily at 9 AM (cron: 0 0 9 * * *). Override with enrollment.reminder-scheduler.cron-expression.
     */
    @Scheduled(cron = "${enrollment.reminder-scheduler.cron-expression:0 0 9 * * *}", zone = "UTC")
    public void sendScheduledReminders() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            AuditContextSupplier.setActionSource(ActionSource.SYSTEM);
            logger.info("[correlationId:{}] Starting scheduled enrollment reminder job", correlationId);
            enrollmentInvitationService.runScheduledReminders();
            logger.info("[correlationId:{}] Completed scheduled enrollment reminder job", correlationId);
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error in scheduled enrollment reminders: {}", correlationId, e.getMessage(), e);
        } finally {
            AuditContextSupplier.clearActionSource();
            MDC.clear();
        }
    }
}
