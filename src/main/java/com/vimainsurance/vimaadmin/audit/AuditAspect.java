package com.vimainsurance.vimaadmin.audit;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

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

    /** Max chars for old/new snapshot JSON to avoid oversized audit rows and memory pressure. */
    private static final int MAX_SNAPSHOT_CHARS = 50_000;
    /** Max collection elements to serialize in request payload (rest summarized). */
    private static final int MAX_COLLECTION_ELEMENTS = 100;

    private final IAuditService auditService;
    private final ObjectMapper objectMapper;
    private final IAdminUserRepository adminUserRepository;

    @AfterReturning(
            pointcut = "@annotation(auditedOperation)",
            returning = "result")
    public void afterReturning(JoinPoint joinPoint, AuditedOperation auditedOperation, Object result) {
        try {
            String entityId = resolveEntityId(result);
            String newSnapshot = truncateSnapshot(toJsonSafe(result));
            String oldSnapshot = isUpdateAction(auditedOperation.action())
                    ? truncateSnapshot(requestPayloadSnapshot(joinPoint))
                    : null;
            String schemaName = auditedOperation.schemaName().isBlank() ? null : auditedOperation.schemaName();
            String tableName = auditedOperation.tableName().isBlank() ? null : auditedOperation.tableName();
            UUID userId = resolveUserId();
            AuditEventPayload payload = AuditEventPayload.builder()
                    .schemaName(schemaName)
                    .tableName(tableName)
                    .entityType(auditedOperation.entityType())
                    .entityId(entityId)
                    .action(auditedOperation.action())
                    .oldSnapshot(oldSnapshot)
                    .newSnapshot(newSnapshot)
                    .userId(userId)
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

    /** Resolve userId: ThreadLocal first, then findByUsername from JWT. */
    private UUID resolveUserId() {
        return AuditContextSupplier.getCurrentUserId()
                .or(() -> Optional.ofNullable(AuditContextSupplier.getUsername())
                        .filter(u -> !u.isBlank())
                        .flatMap(adminUserRepository::findByUsername)
                        .map(AdminUser::getId))
                .orElse(null);
    }

    /** Only capture request payload as oldSnapshot for update-type actions. */
    private static boolean isUpdateAction(String action) {
        if (action == null || action.isBlank()) {
            return false;
        }
        String a = action.toUpperCase();
        return a.contains("UPDATE") || "PATCH".equals(a) || "EDIT".equals(a);
    }

    /**
     * Serializes method arguments as JSON for oldSnapshot (request payload).
     * Excludes non-serializable/sensitive types; caps large collections to avoid huge payloads.
     */
    private String requestPayloadSnapshot(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> serializable = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                serializable.add(null);
            } else if (arg instanceof MultipartFile m) {
                serializable.add("[MultipartFile: " + m.getOriginalFilename() + "]");
            } else if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
                serializable.add("[request/response omitted]");
            } else if (arg instanceof Collection<?> c) {
                if (c.size() > MAX_COLLECTION_ELEMENTS) {
                    List<Object> capped = new ArrayList<>(MAX_COLLECTION_ELEMENTS + 1);
                    capped.add("[Collection size=" + c.size() + ", first " + MAX_COLLECTION_ELEMENTS + " shown]");
                    int i = 0;
                    for (Object o : c) {
                        if (i++ >= MAX_COLLECTION_ELEMENTS) {
                            break;
                        }
                        capped.add(o);
                    }
                    serializable.add(capped);
                } else {
                    serializable.add(arg);
                }
            } else {
                serializable.add(arg);
            }
        }
        return toJsonSafe(serializable);
    }

    /**
     * Truncates snapshot JSON to avoid oversized audit rows (DB/memory). Appends a marker when truncated.
     */
    private static String truncateSnapshot(String json) {
        if (json == null || json.length() <= MAX_SNAPSHOT_CHARS) {
            return json;
        }
        return json.substring(0, MAX_SNAPSHOT_CHARS) + " [truncated, total " + json.length() + " chars]";
    }

    private static String resolveEntityId(Object result) {
        if (result == null) {
            return null;
        }
        if (result instanceof AuditIdentifiable identifiable) {
            return identifiable.getAuditEntityId();
        }
        try {
            for (Method m : result.getClass().getMethods()) {
                if (isIdGetter(m) && (m.getParameterCount() == 0)) {
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

    private static boolean isIdGetter(Method m) {
        String name = m.getName();
        return ("getId".equals(name) || (name.startsWith("get") && name.endsWith("Id") && name.length() > 5));
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
