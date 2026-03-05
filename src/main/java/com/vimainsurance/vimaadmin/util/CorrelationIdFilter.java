package com.vimainsurance.vimaadmin.util;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Correlation ID Filter with Keycloak (IdP) User Sync
 *
 * This filter:
 * 1. Handles correlation IDs for request tracing
 * 2. Automatically creates/updates AdminUser records when users authenticate via Keycloak (JWT)
 *
 * When a user authenticates with a valid Keycloak JWT token, this filter:
 * - Requires email (and username) from the JWT; if missing, no sync (controller returns 403)
 * - Checks if the user exists in the AdminUser table (by email or oauthProviderId)
 * - If not, creates a new AdminUser record with information from the JWT
 * - Updates the lastLogin timestamp for existing users
 *
 * React performs login and token exchange - no login endpoints or callback endpoints are required here.
 *
 * When running with dev/test profile, Spring Security uses mock authentication (principal "dev-user" or "e2e-vima-admin").
 * This filter ensures a corresponding AdminUser row exists for that principal so /auth/me and other lookups succeed.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";
    public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";
    
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private IAdminUserRepository adminUserRepository;

    @Autowired(required = false)
    private IdGenerator idGenerator;

    @Autowired(required = false)
    private JwtUserExtractor jwtUserExtractor;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Handle Correlation ID (existing functionality)
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // // Generate CSP nonce for this request
            // String nonce = generateNonce();
            // request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
            
            // // Build and set CSP headers with the specified policy
            // String cspPolicy = buildCspPolicy(nonce);
            // response.setHeader("Content-Security-Policy", cspPolicy);
            // response.setHeader("X-Content-Type-Options", "nosniff");
            // response.setHeader("X-Frame-Options", "DENY");
            // response.setHeader("X-XSS-Protection", "1; mode=block");
            // response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // logger.debug("[correlationId:{}] Generated CSP nonce: {} for {} {}", 
            //     correlationId, nonce, request.getMethod(), request.getRequestURI());
            
            // Sync Keycloak/IdP user to AdminUser table BEFORE processing the request.
            // This ensures the user exists in admin_users when controllers (e.g. /auth/me)
            // look up the current user, avoiding "User account not found" on first login.
            syncIdpUserToAdminUsers();
            
            filterChain.doFilter(request, response);
            
            // Log request and response details (existing functionality)
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
            String userAgent = request.getHeader("User-Agent");
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();
            String timestamp = Instant.now().toString();
            logger.info("[{}] [correlationId:{}] {} {} from IP={} User-Agent='{}' -> status={}",
                    timestamp, correlationId, method, uri, ipAddress, userAgent, status);
        } finally {
            MDC.remove(MDC_CORRELATION_ID_KEY);
        }
    }
    
    /**
     * Generate cryptographically secure nonce
     */
    private String generateNonce() {
        byte[] nonceBytes = new byte[16]; // 128-bit nonce
        secureRandom.nextBytes(nonceBytes);
        return Base64.getEncoder().encodeToString(nonceBytes);
    }
    
    // /**
    //  * Build CSP policy with the generated nonce using the specified policy
    //  */
    // private String buildCspPolicy(String nonce) {
    //     return "default-src 'self'; " +
    //            "script-src 'nonce-" + nonce + "' 'strict-dynamic'; " +
    //            "style-src 'self' https://fonts.googleapis.com; " +
    //            "font-src 'self' https://fonts.gstatic.com; " +
    //            "img-src 'self' data: https:; " +
    //            "frame-src https://www.google.com; " +
    //            "connect-src 'self' https://api.vimainsurance.com; " +
    //            "object-src 'none'; " +
    //            "base-uri 'self'; " +
    //            "frame-ancestors 'self'; " +
    //            "form-action 'self'; " +
    //            "upgrade-insecure-requests";
    // }
    
    // /**
    //  * Utility method to get nonce from request
    //  */
    // public static String getNonce(HttpServletRequest request) {
    //     return (String) request.getAttribute(CSP_NONCE_ATTRIBUTE);
    // }

    /**
     * Sync Keycloak/IdP user to AdminUser table.
     * Creates or updates AdminUser when user authenticates via Keycloak JWT.
     * Requires email from JWT (user logged in with it); if email or username is missing we do not create (controller will return 403).
     */
    private void syncIdpUserToAdminUsers() {
        if (adminUserRepository == null || idGenerator == null || jwtUserExtractor == null) {
            return;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                String email = jwtUserExtractor.getEmail(jwt);
                String preferredUsername = jwtUserExtractor.getPreferredUsername(jwt);
                String subject = jwtUserExtractor.getSubject(jwt);
                String name = jwt.getClaimAsString("name");
                List<String> roles = jwtUserExtractor.getRoles(jwt);

                // Require email (and effective username); no placeholder - if missing, controller returns 403
                boolean hasEmail = email != null && !email.isBlank();
                boolean hasUsername = (preferredUsername != null && !preferredUsername.isBlank()) || hasEmail;
                boolean canSync = !roles.contains("ROLE_EMPLOYEE") && hasEmail && hasUsername;

                if (canSync) {
                    // Check if user exists by email or oauthProviderId
                    Optional<AdminUser> userByEmail = adminUserRepository.findByEmail(email);
                    Optional<AdminUser> userByOAuthId = subject != null 
                        ? adminUserRepository.findByOauthProviderId(subject)
                        : Optional.empty();
                    
                    AdminUser user = userByEmail.orElse(userByOAuthId.orElse(null));
                    
                    if (user == null) {
                        // User doesn't exist - create new AdminUser
                        user = createAdminUserFromJwt(jwt, email, preferredUsername, subject, name);
                        logger.info("[correlationId:{}] Created new AdminUser from Keycloak JWT: email={}, username={}, role={}", 
                            MDC.get(MDC_CORRELATION_ID_KEY), email, user.getUsername(), user.getRole());
                    } else {
                        // User exists - update lastLogin and potentially sync other fields
                        updateAdminUserFromJwt(user, jwt, email, preferredUsername, subject, name);
                        logger.debug("[correlationId:{}] Updated AdminUser lastLogin: email={}, username={}", 
                            MDC.get(MDC_CORRELATION_ID_KEY), email, user.getUsername());
                    }
                }
            } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
                // Dev/test profile: mock auth (dev-user or e2e-vima-admin) — ensure that principal exists in admin_users
                String mockUsername = authentication.getName();
                if (mockUsername != null && !mockUsername.isBlank()) {
                    ensureMockUserExists(mockUsername);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the request - allow authentication to proceed
            logger.error("[correlationId:{}] Error syncing AdminUser from Keycloak JWT", 
                MDC.get(MDC_CORRELATION_ID_KEY), e);
        }
    }

    /**
     * Ensure admin_users has a row for the given mock username (dev-user or e2e-vima-admin).
     * DevAuthenticationFilter sets principal to "dev-user" (dev) or "e2e-vima-admin" (test); both must exist for /auth/me.
     */
    private void ensureMockUserExists(String username) {
        if (adminUserRepository == null || idGenerator == null) {
            return;
        }
        try {
            Optional<AdminUser> existing = adminUserRepository.findByUsername(username);
            if (existing.isPresent()) {
                AdminUser u = existing.get();
                u.setLastLogin(LocalDateTime.now());
                adminUserRepository.save(u);
                return;
            }
            AdminUser user = new AdminUser();
            user.setUsername(username);
            user.setEmail(username + "@local");
            user.setFullName("e2e-vima-admin".equals(username) ? "E2E Vima Admin" : "Dev/Test User");
            user.setRole("e2e-vima-admin".equals(username) ? "VIMA_ADMIN" : "SUPER_ADMIN");
            user.setAgentId(idGenerator.generateVimaId());
            user.setIsActive(true);
            user.setLastLogin(LocalDateTime.now());
            user.setCreatedAt(LocalDateTime.now());
            user.setOauthProvider("local");
            adminUserRepository.save(user);
            logger.info("[correlationId:{}] Created AdminUser for mock user: {}", 
                MDC.get(MDC_CORRELATION_ID_KEY), username);
        } catch (Exception e) {
            logger.warn("[correlationId:{}] Could not ensure mock user {} in admin_users: {}", 
                MDC.get(MDC_CORRELATION_ID_KEY), username, e.getMessage());
        }
    }

    /**
     * Create a new AdminUser from JWT claims
     */
    private AdminUser createAdminUserFromJwt(Jwt jwt, String email, String preferredUsername, 
                                             String subject, String name) {
        AdminUser user = new AdminUser();
        
        // Normalize to lowercase so DB matches identity provider and avoids login mismatch
        email = email != null ? email.trim().toLowerCase() : null;
        String username = preferredUsername != null ? preferredUsername.trim().toLowerCase() : null;
        if (username == null && email != null) {
            username = email.split("@")[0];
        }
        
        // Set email (required)
        user.setEmail(email);
        
        // Set username - prefer preferred_username, fallback to email local part
        user.setUsername(username);
        
        // Set full name - prefer name claim, fallback to preferred_username or email
        String fullName = name != null ? name : 
                         (preferredUsername != null ? preferredUsername : (email != null ? email.split("@")[0] : "User"));
        user.setFullName(fullName);
        
        // Extract role from JWT groups/roles - default to SALES_AGENT if none found or unknown
        List<String> roles = jwtUserExtractor.getRoles(jwt);
        String roleStr = roles.isEmpty() ? "SALES_AGENT" : roles.get(0);
        roleStr = roleStr.replace("ROLE_", "").replace(" Group", "").trim();
        try {
            user.setRole(UserRole.fromValue(roleStr).getValue());
        } catch (IllegalArgumentException e) {
            user.setRole(UserRole.SALES_AGENT.getValue());
        }
        
        // Set OAuth provider information
        user.setOauthProvider("Keycloak");
        if (subject != null) {
            user.setOauthProviderId(subject);
        }
        
        // Generate agent ID
        user.setAgentId(idGenerator.generateVimaId());
        
        // Set defaults
        user.setIsActive(true);
        user.setLastLogin(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        
        // No password hash needed - user authenticates via Keycloak
        
        // Save user
        return adminUserRepository.save(user);
    }

    /**
     * Update existing AdminUser with information from JWT
     */
    private void updateAdminUserFromJwt(AdminUser user, Jwt jwt, String email, String preferredUsername, 
                                       String subject, String name) {
        boolean updated = false;
        
        // Update lastLogin timestamp
        user.setLastLogin(LocalDateTime.now());
        updated = true;
        
        // Update OAuth provider info if not set
        if (user.getOauthProvider() == null || !user.getOauthProvider().equals("Keycloak")) {
            user.setOauthProvider("Keycloak");
            updated = true;
        }
        
        // Update OAuth provider ID if not set
        if (subject != null && (user.getOauthProviderId() == null || !user.getOauthProviderId().equals(subject))) {
            user.setOauthProviderId(subject);
            updated = true;
        }
        
        // Optionally update role from JWT if it has changed
        // Extract roles from the passed JWT token
        List<String> roles = jwtUserExtractor.getRoles(jwt);
        if (!roles.isEmpty()) {
            String jwtRole = roles.get(0).replace("ROLE_", "").replace(" Group", "").trim();
            if (user.getRole() == null || !user.getRole().equals(jwtRole)) {
                // Only update role if it's different (may want to keep manual role assignments)
                // Uncomment if you want to sync roles from Keycloak:
                // user.setRole(jwtRole);
                // updated = true;
            }
        }
        
        // Update full name if name claim is provided and different
        if (name != null && (user.getFullName() == null || !user.getFullName().equals(name))) {
            user.setFullName(name);
            updated = true;
        }
        
        // Save if updated
        if (updated) {
            adminUserRepository.save(user);
        }
    }
} 