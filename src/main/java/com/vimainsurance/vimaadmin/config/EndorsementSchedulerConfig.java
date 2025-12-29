package com.vimainsurance.vimaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "endorsement.scheduler")
public class EndorsementSchedulerConfig {
    
    /**
     * Enable or disable the endorsement schedule confirmation scheduler
     */
    private boolean enabled = true;
    
    /**
     * Cron expression for scheduling (default: every minute)
     * Examples:
     * - "0 * * * * ?" - every minute
     * - "0 0 * * * ?" - every hour
     * - "0 0 0 * * ?" - every day at midnight
     * - "0 0 0 * * MON" - every Monday at midnight
     */
    private String cronExpression = "0 * * * * ?";
    
    /**
     * Fixed delay in milliseconds (alternative to cron)
     * If set, this will be used instead of cronExpression
     */
    private Long fixedDelayMs = null;
    
    /**
     * Initial delay in milliseconds before first execution
     */
    private Long initialDelayMs = 60000L; // 1 minute default
}

