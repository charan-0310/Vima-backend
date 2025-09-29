package com.vimainsurance.vimaadmin.util;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";
    public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";
    
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private final SecureRandom secureRandom = new SecureRandom();

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
} 