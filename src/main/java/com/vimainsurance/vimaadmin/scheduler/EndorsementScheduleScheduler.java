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
import com.vimainsurance.vimaadmin.config.EndorsementSchedulerConfig;
import com.vimainsurance.vimaadmin.service.IEndorsementService;

import jakarta.annotation.PostConstruct;

/**
 * Scheduler for automatically confirming endorsement schedules
 * Updates deals from APPROVED to ACTIVE and LEAVING to INACTIVE based on date conditions
 */
@Component
@ConditionalOnProperty(
    prefix = "endorsement.scheduler",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class EndorsementScheduleScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EndorsementScheduleScheduler.class);

    @Autowired
    private IEndorsementService endorsementService;

    @Autowired
    private EndorsementSchedulerConfig schedulerConfig;

    @PostConstruct
    public void init() {
        logger.info("EndorsementScheduleScheduler initialized. Enabled: {}, Cron: {}",
            schedulerConfig.isEnabled(),
            schedulerConfig.getCronExpression());
    }

    /**
     * Scheduled task to confirm endorsement schedules
     * Runs based on configured cron expression
     * Default: Every minute (0 * * * * ?)
     * Note: initialDelay is not supported with cron expressions in Spring
     */
    @Scheduled(cron = "${endorsement.scheduler.cron-expression:0 * * * * ?}", zone="UTC")
    public void confirmEndorsementSchedule() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            AuditContextSupplier.setActionSource(ActionSource.SYSTEM);
            logger.info("[correlationId:{}] Starting scheduled endorsement confirmation", correlationId);
            endorsementService.confirmSchedule();
            logger.info("[correlationId:{}] Completed scheduled endorsement confirmation", correlationId);
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error in scheduled endorsement confirmation: {}",
                correlationId, e.getMessage(), e);
        } finally {
            AuditContextSupplier.clearActionSource();
            MDC.clear();
        }
    }
}

