package com.vimainsurance.vimaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "audit.replay")
public class AuditReplayConfig {

    /**
     * Enable or disable the audit pending replay scheduler.
     */
    private boolean enabled = true;

    /**
     * Cron expression (default: every 10 minutes).
     * Example: "0 */10 * * * *" = every 10 minutes.
    **/
    private String cronExpression = "0 */10 * * * *";

    /**
     * Max number of pending events to process per run.
     */
    private int batchSize = 50;

    /**
     * After this many failed replay attempts, mark the pending record as FAILED (no further retries).
     */
    private int maxRetries = 5;
}
