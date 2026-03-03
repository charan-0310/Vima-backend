package com.vimainsurance.vimaadmin.audit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.experimental.UtilityClass;

/**
 * Supplies audit context from request thread (MDC, SecurityContext, HttpServletRequest).
 * Stateless and loosely coupled: no injection, only static context.
 */
@UtilityClass
public class AuditContextSupplier {

    public static final String MDC_CORRELATION_ID = "correlationId";

    public static String getCorrelationId() {
        return MDC.get(MDC_CORRELATION_ID);
    }

    public static String getUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String email = jwtAuth.getToken().getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
        }
        return auth != null ? auth.getName() : null;
    }

    /** Username from JWT (preferred_username or sub) for lookup when userId is not in ThreadLocal. */
    public static String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String preferred = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (preferred != null && !preferred.isBlank()) {
                return preferred;
            }
        }
        return auth != null ? auth.getName() : null;
    }

    public static String getUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            List<String> roles = getRolesFromJwt(jwtAuth.getToken());
            return roles == null || roles.isEmpty()
                    ? null
                    : roles.stream().filter(role -> role != null && role.startsWith("ROLE_")).findFirst().orElse(null);
        }
        return null;
    }

    /**
     * Resolves roles from JWT: tries "groups" (Authentik), then "roles", then "realm_access.roles" (Keycloak-style).
     */
    private static List<String> getRolesFromJwt(Jwt jwt) {
        Object groups = jwt.getClaim("groups");
        if (groups instanceof List<?> list) {
            List<String> out = list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
            if (!out.isEmpty()) {
                return out;
            }
        }
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof List<?> list) {
            List<String> out = list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
            if (!out.isEmpty()) {
                return out;
            }
        }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            Object roles = realmAccess.get("roles");
            if (roles instanceof List<?> list) {
                return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
            }
        }
        return List.of();
    }

    public static String getIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null && attrs.getRequest() != null) {
                return attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception ignored) {
            // not in request scope (e.g. async/scheduled)
        }
        return null;
    }

    /**
     * Optional: set by caller before invoking audited method if user id is known (e.g. from AdminUser).
     */
    private static final ThreadLocal<UUID> CURRENT_USER_ID = new ThreadLocal<>();

    public static void setCurrentUserId(UUID userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Optional<UUID> getCurrentUserId() {
        return Optional.ofNullable(CURRENT_USER_ID.get());
    }

    public static void clearCurrentUserId() {
        CURRENT_USER_ID.remove();
    }

    /**
     * Optional: set by caller when the audited method only has a UUID organizationId (e.g. delete(UUID organizationId)).
     * Read by the audit aspect in the same request thread.
     */
    private static final ThreadLocal<UUID> CURRENT_ORGANIZATION_ID = new ThreadLocal<>();

    public static void setOrganizationId(UUID organizationId) {
        CURRENT_ORGANIZATION_ID.set(organizationId);
    }

    public static UUID getOrganizationId() {
        return CURRENT_ORGANIZATION_ID.get();
    }

    public static void clearOrganizationId() {
        CURRENT_ORGANIZATION_ID.remove();
    }

    /**
     * ActionSource from thread local (set by caller for BULK, SYSTEM, ENROLLMENT).
     */
    private static final ThreadLocal<ActionSource> ACTION_SOURCE = ThreadLocal.withInitial(() -> ActionSource.WEB);

    public static void setActionSource(ActionSource source) {
        ACTION_SOURCE.set(source);
    }

    public static ActionSource getActionSource() {
        return ACTION_SOURCE.get();
    }

    public static void clearActionSource() {
        ACTION_SOURCE.remove();
    }
}
