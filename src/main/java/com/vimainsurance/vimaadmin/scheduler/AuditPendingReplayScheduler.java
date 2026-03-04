package com.vimainsurance.vimaadmin.scheduler;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.audit.AuditEventPayload;
import com.vimainsurance.vimaadmin.audit.AuditEventWriter;
import com.vimainsurance.vimaadmin.audit.AuditPendingRepository;
import com.vimainsurance.vimaadmin.audit.entity.AuditPending;
import com.vimainsurance.vimaadmin.config.AuditReplayConfig;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Replays failed audit events from audit_pending into audit_events.
 * Runs on a schedule; each run processes up to batchSize PENDING records with retry_count &lt; maxRetries.
 * On success: status → PROCESSED. On failure: retry_count incremented; if retry_count ≥ maxRetries, status → FAILED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.replay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditPendingReplayScheduler {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSED = "PROCESSED";
    private static final String STATUS_FAILED = "FAILED";

    private final AuditPendingRepository auditPendingRepository;
    private final AuditEventWriter auditEventWriter;
    private final AuditReplayConfig replayConfig;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("AuditPendingReplayScheduler initialized. Enabled: {}, Cron: {}, BatchSize: {}, MaxRetries: {}",
                replayConfig.isEnabled(),
                replayConfig.getCronExpression(),
                replayConfig.getBatchSize(),
                replayConfig.getMaxRetries());
    }

    @Scheduled(cron = "${audit.replay.cron-expression:0 */10 * * * *}", zone = "UTC")
    public void replayPendingAuditEvents() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            int batchSize = Math.max(1, replayConfig.getBatchSize());
            int maxRetries = Math.max(1, replayConfig.getMaxRetries());

            var page = auditPendingRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    STATUS_PENDING, maxRetries, PageRequest.of(0, batchSize));
            var pendingList = page.getContent();

            if (pendingList.isEmpty()) {
                log.trace("[correlationId:{}] No pending audit events to replay", correlationId);
                return;
            }

            log.info("[correlationId:{}] Replaying {} pending audit event(s)", correlationId, pendingList.size());
            int processed = 0;
            int failed = 0;

            for (AuditPending pending : pendingList) {
                try {
                    AuditEventPayload payload = parsePayload(pending.getPayload());
                    if (payload == null) {
                        markFailed(pending, "Failed to parse payload");
                        failed++;
                        continue;
                    }
                    auditEventWriter.save(payload);
                    pending.setStatus(STATUS_PROCESSED);
                    pending.setUpdatedAt(Instant.now());
                    auditPendingRepository.save(pending);
                    processed++;
                } catch (Exception e) {
                    int newRetryCount = pending.getRetryCount() + 1;
                    pending.setRetryCount(newRetryCount);
                    pending.setErrorMessage(e.getMessage());
                    pending.setUpdatedAt(Instant.now());
                    if (newRetryCount >= maxRetries) {
                        pending.setStatus(STATUS_FAILED);
                        log.warn("[correlationId:{}] Audit pending id={} marked FAILED after {} attempts: {}",
                                correlationId, pending.getId(), newRetryCount, e.getMessage());
                    }
                    auditPendingRepository.save(pending);
                    failed++;
                }
            }

            log.info("[correlationId:{}] Audit replay finished: {} processed, {} failed",
                    correlationId, processed, failed);
        } catch (Exception e) {
            log.error("[correlationId:{}] Audit replay job failed: {}", correlationId, e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }

    private AuditEventPayload parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, AuditEventPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("[audit] Failed to deserialize audit pending payload: {}", e.getMessage());
            return null;
        }
    }

    private void markFailed(AuditPending pending, String message) {
        pending.setStatus(STATUS_FAILED);
        pending.setErrorMessage(message);
        pending.setUpdatedAt(Instant.now());
        auditPendingRepository.save(pending);
    }
}
