package com.vimainsurance.vimaadmin.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting client IP addresses from HTTP requests.
 * Handles various proxy headers and direct connections.
 */
public class IpAddressExtractor {
    
    /**
     * Extracts the client IP address from the HTTP request.
     * 
     * Checks headers in this order:
     * 1. X-Forwarded-For (used by most proxies and load balancers)
     * 2. X-Real-IP (alternative proxy header)
     * 3. Direct connection IP (request.getRemoteAddr())
     * 
     * For X-Forwarded-For with multiple IPs (client, proxy1, proxy2, ...),
     * returns the first IP (the original client).
     * 
     * @param request HTTP servlet request
     * @return Client IP address, or null if request is null
     */
    public static String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // Check X-Forwarded-For header (used by proxies/load balancers)
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // Check X-Real-IP header (alternative proxy header)
            ip = request.getHeader("X-Real-IP");
        }
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // Fall back to direct connection IP
            ip = request.getRemoteAddr();
        }
        
        // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
        // Take the first IP (the original client)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
