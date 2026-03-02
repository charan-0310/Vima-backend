package com.vimainsurance.vimaadmin.audit;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AOP aspect: intercepts @AuditedOperation methods, captures context in request thread,
 * and enqueues audit event for async write. Loosely coupled to business code.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final IAuditService auditService;
    private final ObjectMapper objectMapper;

    @AfterReturning(
            pointcut = "@annotation(auditedOperation)",
            returning = "result")
    public void afterReturning(JoinPoint joinPoint, AuditedOperation auditedOperation, Object result) {
        try {
            String entityId = resolveEntityId(result);
            String newSnapshot = toJsonSafe(result);
            String schemaName = auditedOperation.schemaName().isBlank() ? null : auditedOperation.schemaName();
            String tableName = auditedOperation.tableName().isBlank() ? null : auditedOperation.tableName();
            AuditEventPayload payload = AuditEventPayload.builder()
                    .schemaName(schemaName)
                    .tableName(tableName)
                    .entityType(auditedOperation.entityType())
                    .entityId(entityId)
                    .action(auditedOperation.action())
                    .oldSnapshot(null)
                    .newSnapshot(newSnapshot)
                    .userId(AuditContextSupplier.getCurrentUserId().orElse(null))
                    .userEmail(AuditContextSupplier.getUserEmail())
                    .userRole(AuditContextSupplier.getUserRole())
                    .correlationId(AuditContextSupplier.getCorrelationId())
                    .ipAddress(AuditContextSupplier.getIpAddress())
                    .actionSource(AuditContextSupplier.getActionSource())
                    .timestamp(Instant.now())
                    .build();
            auditService.writeAsync(payload);
        } catch (Exception e) {
            log.warn("[audit] aspect failed to build or enqueue audit event: {}", e.getMessage());
        }
    }

    private static String resolveEntityId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            for (String methodName : new String[] { "getEndorsementId", "getId", "getClaimId" }) {
                Method m = findMethod(result.getClass(), methodName);
                if (m != null) {
                    Object id = m.invoke(result);
                    if (id instanceof UUID uuid) {
                        return uuid.toString();
                    }
                    if (id != null) {
                        return id.toString();
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name) {
        try {
            return clazz.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.trace("[audit] could not serialize return value for snapshot: {}", e.getMessage());
            return null;
        }
    }
}
