package com.vimainsurance.vimaadmin.scheduler;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.audit.ActionSource;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;

/**
 * Scheduler for marking enrollment windows as expired.
 * Runs daily (configurable): finds enrollment windows with status SCHEDULED
 * where end_date has passed, and updates their status to EXPIRED.
 */
@Component
@ConditionalOnProperty(
    prefix = "enrollment.window-expiry-scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class EnrollmentWindowExpiryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentWindowExpiryScheduler.class);

    @Autowired
    private IEnrollmentWindowsRepository enrollmentWindowsRepository;

    /**
     * Runs daily at midnight (cron: 0 0 0 * * *). Override with enrollment.window-expiry-scheduler.cron-expression.
     * Marks SCHEDULED enrollment windows as EXPIRED if their end_date has passed.
     */
    @Scheduled(cron = "${enrollment.window-expiry-scheduler.cron-expression:0 0 0 * * *}", zone = "UTC")
    @Transactional
    public void markExpiredEnrollmentWindows() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            AuditContextSupplier.setActionSource(ActionSource.SYSTEM);
            logger.info("[correlationId:{}] Starting scheduled enrollment window expiry job", correlationId);

            LocalDate today = LocalDate.now();
            int updatedCount = enrollmentWindowsRepository.markExpiredWindows(today);

            logger.info("[correlationId:{}] Completed enrollment window expiry job. Marked {} windows as expired",
                    correlationId, updatedCount);
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error in scheduled enrollment window expiry: {}",
                    correlationId, e.getMessage(), e);
        } finally {
            AuditContextSupplier.clearActionSource();
            MDC.clear();
        }
    }
}
