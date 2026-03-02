package com.vimainsurance.vimaadmin.audit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

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

    public static String getUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            var roles = jwtAuth.getToken().getClaimAsStringList("groups");
            if (roles != null && !roles.isEmpty()) {
                return roles.get(0);
            }
        }
        return null;
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
