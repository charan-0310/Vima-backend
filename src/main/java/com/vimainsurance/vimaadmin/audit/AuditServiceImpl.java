package com.vimainsurance.vimaadmin.audit;

import java.time.Instant;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.audit.entity.AuditPending;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Async audit writer with retry and dead-letter (audit_pending) on failure.
 * Loosely coupled: depends only on AuditEventWriter, AuditPendingRepository and ObjectMapper.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements IAuditService {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    private final AuditEventWriter auditEventWriter;
    private final AuditPendingRepository auditPendingRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void writeAsync(AuditEventPayload payload) {
        if (payload == null) {
            return;
        }
        int attempt = 0;
        Exception lastException = null;
        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                auditEventWriter.save(payload);
                if (attempt > 1) {
                    log.debug("[audit] write succeeded on retry attempt {}", attempt);
                }
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("[audit] write attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("[audit] retry interrupted");
                        break;
                    }
                }
            }
        }
        saveToPending(payload, lastException);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveToPending(AuditEventPayload payload, Exception cause) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            AuditPending pending = AuditPending.builder()
                    .payload(payloadJson)
                    .status("PENDING")
                    .errorMessage(cause != null ? cause.getMessage() : null)
                    .retryCount(0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            auditPendingRepository.save(pending);
            log.error("[audit] event written to audit_pending for retry; entityType={}, entityId={}, correlationId={}",
                    payload.getEntityType(), payload.getEntityId(), payload.getCorrelationId(), cause);
        } catch (JsonProcessingException e) {
            log.error("[audit] failed to serialize payload for audit_pending", e);
        }
    }
}
