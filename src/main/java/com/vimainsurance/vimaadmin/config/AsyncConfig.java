package com.vimainsurance.vimaadmin.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async execution config for enrollment bulk operations (e.g. 1000 invites in &lt;5 min).
 */
@Configuration
public class AsyncConfig {

    public static final String ENROLLMENT_BULK_EXECUTOR = "enrollmentBulkExecutor";

    /**
     * Executor for bulk enrollment invitation sends. Bounded pool to avoid overwhelming DB and email.
     */
    @Bean(name = ENROLLMENT_BULK_EXECUTOR)
    public Executor enrollmentBulkExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("enrollment-bulk-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);
        executor.initialize();
        return executor;
    }
}
