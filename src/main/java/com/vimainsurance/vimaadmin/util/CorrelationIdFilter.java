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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Correlation ID Filter with Authentik User Sync
 * 
 * This filter:
 * 1. Handles correlation IDs for request tracing
 * 2. Automatically creates/updates AdminUser records when users authenticate via Authentik
 * 
 * When a user authenticates with a valid Authentik JWT token, this filter:
 * - Checks if the user exists in the AdminUser table (by email or oauthProviderId)
 * - If not, creates a new AdminUser record with information from the JWT
 * - Updates the lastLogin timestamp for existing users
 * 
 * This backend only validates JWT tokens issued by Authentik.
 * React performs login and token exchange - no login endpoints or callback endpoints are required here.
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
            
            filterChain.doFilter(request, response);
            
            // Sync Authentik user to AdminUser table if authenticated with JWT
            // This runs after authentication is established by Spring Security filters
            syncAuthentikUser();
            
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
     * Sync Authentik user to AdminUser table
     * Creates or updates AdminUser record when user authenticates via Authentik JWT
     */
    private void syncAuthentikUser() {
        if (adminUserRepository == null || idGenerator == null || jwtUserExtractor == null) {
            // Dependencies not available, skip user sync
            return;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Only process if authenticated with JWT token
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                
                // Extract user information from JWT
                String email = jwtUserExtractor.getEmail(jwt);
                String preferredUsername = jwtUserExtractor.getPreferredUsername(jwt);
                String subject = jwtUserExtractor.getSubject(jwt);
                String name = jwt.getClaimAsString("name");
                List<String> roles = jwtUserExtractor.getRoles(jwt);
                if (email != null && !roles.contains("ROLE_EMPLOYEE")) {
                    // Check if user exists by email or oauthProviderId
                    Optional<AdminUser> userByEmail = adminUserRepository.findByEmail(email);
                    Optional<AdminUser> userByOAuthId = subject != null 
                        ? adminUserRepository.findByOauthProviderId(subject)
                        : Optional.empty();
                    
                    AdminUser user = userByEmail.orElse(userByOAuthId.orElse(null));
                    
                    if (user == null) {
                        // User doesn't exist - create new AdminUser
                        user = createAdminUserFromJwt(jwt, email, preferredUsername, subject, name);
                        logger.info("[correlationId:{}] Created new AdminUser from Authentik JWT: email={}, username={}, role={}", 
                            MDC.get(MDC_CORRELATION_ID_KEY), email, user.getUsername(), user.getRole());
                    } else {
                        // User exists - update lastLogin and potentially sync other fields
                        updateAdminUserFromJwt(user, jwt, email, preferredUsername, subject, name);
                        logger.debug("[correlationId:{}] Updated AdminUser lastLogin: email={}, username={}", 
                            MDC.get(MDC_CORRELATION_ID_KEY), email, user.getUsername());
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the request - allow authentication to proceed
            logger.error("[correlationId:{}] Error syncing AdminUser from Authentik JWT", 
                MDC.get(MDC_CORRELATION_ID_KEY), e);
        }
    }

    /**
     * Create a new AdminUser from JWT claims
     */
    private AdminUser createAdminUserFromJwt(Jwt jwt, String email, String preferredUsername, 
                                             String subject, String name) {
        AdminUser user = new AdminUser();
        
        // Set email (required)
        user.setEmail(email);
        
        // Set username - prefer preferred_username, fallback to email
        String username = preferredUsername != null ? preferredUsername : email.split("@")[0];
        user.setUsername(username);
        
        // Set full name - prefer name claim, fallback to preferred_username or email
        String fullName = name != null ? name : 
                         (preferredUsername != null ? preferredUsername : email.split("@")[0]);
        user.setFullName(fullName);
        
        // Extract role from JWT groups/roles - default to SALES_AGENT if none found
        List<String> roles = jwtUserExtractor.getRoles(jwt);
        String role = roles.isEmpty() ? "SALES_AGENT" : roles.get(0);
        // Remove "ROLE_" prefix if present, remove " Group" suffix if present
        role = role.replace("ROLE_", "").replace(" Group", "").trim();
        user.setRole(role);
        
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
        
        // No password hash needed - user authenticates via Authentik
        
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
                // Uncomment if you want to sync roles from Authentik:
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