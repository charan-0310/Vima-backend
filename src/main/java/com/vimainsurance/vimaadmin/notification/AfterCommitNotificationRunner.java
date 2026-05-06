package com.vimainsurance.vimaadmin.notification;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.vimainsurance.vimaadmin.config.AsyncConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AfterCommitNotificationRunner {

    private final Executor executor;

    public AfterCommitNotificationRunner(@Qualifier(AsyncConfig.ENROLLMENT_BULK_EXECUTOR) Executor executor) {
        this.executor = executor;
    }

    public void runAsyncAfterCommit(Runnable task) {
        Runnable wrapped = () -> {
            try {
                log.info("after_commit_notification_execute");
                task.run();
            } catch (Exception e) {
                log.warn("after_commit_notification_failed: {}", e.getMessage(), e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            log.info("after_commit_notification_register mode=tx_sync");
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("after_commit_notification_after_commit");
                    executor.execute(wrapped);
                }
            });
        } else {
            log.info("after_commit_notification_register mode=immediate");
            executor.execute(wrapped);
        }
    }
}
