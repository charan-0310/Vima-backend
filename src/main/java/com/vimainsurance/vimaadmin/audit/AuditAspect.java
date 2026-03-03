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
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired(required = false)
    private JwtUserExtractor jwtUserExtractor;

    @AfterReturning(
            pointcut = "@annotation(auditedOperation)",
            returning = "result")
    public void afterReturning(JoinPoint joinPoint, AuditedOperation auditedOperation, Object result) {
        try {
            // For update actions, prefer entity ID from method args (DTO/path param); fall back to result
            String entityId = resolveEntityId(auditedOperation.action(), joinPoint, result);
            String newSnapshot = truncateSnapshot(toJsonSafe(result));
            String oldSnapshot = isUpdateAction(auditedOperation.action())
                    ? truncateSnapshot(requestPayloadSnapshot(joinPoint))
                    : null;
            String schemaName = auditedOperation.schemaName().isBlank() ? null : auditedOperation.schemaName();
            String tableName = auditedOperation.tableName().isBlank() ? null : auditedOperation.tableName();
            UUID userId = resolveUserId();
            UUID organizationId = resolveOrganizationId(joinPoint, result);
            AuditEventPayload payload = AuditEventPayload.builder()
                    .schemaName(schemaName)
                    .tableName(tableName)
                    .entityType(auditedOperation.entityType())
                    .entityId(entityId)
                    .action(auditedOperation.action())
                    .oldSnapshot(oldSnapshot)
                    .newSnapshot(newSnapshot)
                    .userId(userId)
                    .organizationId(organizationId)
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

    /**
     * Resolve organization ID for audit: result/args implementing AuditIdentifiable, then ThreadLocal, then JWT.
     * No reflection; no TenantFilter/TenantContext.
     */
    private UUID resolveOrganizationId(JoinPoint joinPoint, Object result) {
        if (result instanceof AuditIdentifiable identifiable) {
            UUID orgId = identifiable.getAuditOrganizationId();
            if (orgId != null) {
                return orgId;
            }
        }
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg == null || arg instanceof MultipartFile || arg instanceof ServletRequest || arg instanceof ServletResponse) {
                    continue;
                }
                if (arg instanceof AuditIdentifiable identifiable) {
                    UUID orgId = identifiable.getAuditOrganizationId();
                    if (orgId != null) {
                        return orgId;
                    }
                }
            }
        }
        UUID fromContext = AuditContextSupplier.getOrganizationId();
        if (fromContext != null) {
            return fromContext;
        }
        if (jwtUserExtractor != null) {
            List<String> orgs = jwtUserExtractor.getCurrentOrganizations();
            if (orgs != null && !orgs.isEmpty()) {
                try {
                    return UUID.fromString(orgs.get(0).trim());
                } catch (Exception ignored) {
                    // ignore parse failure
                }
            }
        }
        return null;
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

    /**
     * Resolves entity ID for the audit event. For update-type actions, first tries method arguments
     * (DTO/path params) so the updated entity ID is recorded; then falls back to the return value.
     */
    private String resolveEntityId(String action, JoinPoint joinPoint, Object result) {
        if (isUpdateAction(action)) {
            String fromArgs = resolveEntityIdFromArgs(joinPoint);
            if (fromArgs != null && !fromArgs.isBlank()) {
                return fromArgs;
            }
        }
        return resolveEntityIdFromResult(result);
    }

    /**
     * Extracts entity ID from method arguments (e.g. DTO with getEndorsementId(), UUID path param).
     * Used for update actions so the audit row stores the ID of the entity being updated.
     */
    private static String resolveEntityIdFromArgs(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg == null || arg instanceof MultipartFile || arg instanceof ServletRequest || arg instanceof ServletResponse) {
                continue;
            }
            if (arg instanceof AuditIdentifiable identifiable) {
                String id = identifiable.getAuditEntityId();
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
            if (arg instanceof UUID uuid) {
                return uuid.toString();
            }
            if (arg instanceof Long l) {
                return l.toString();
            }
            String fromGetter = getEntityIdFromBean(arg);
            if (fromGetter != null) {
                return fromGetter;
            }
        }
        return null;
    }

    /** Try entity ID getters on a DTO/entity: getId(), getEndorsementId(), getOrganizationId(), etc. */
    private static String getEntityIdFromBean(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            for (Method m : obj.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!isIdGetter(m)) continue;
                Object val = m.invoke(obj);
                if (val == null) continue;
                if (val instanceof UUID uuid) return uuid.toString();
                if (val instanceof Long l) return l.toString();
                if (val instanceof String s && !s.isBlank()) return s;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static String resolveEntityIdFromResult(Object result) {
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
