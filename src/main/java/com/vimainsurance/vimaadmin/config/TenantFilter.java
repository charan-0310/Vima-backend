package com.vimainsurance.vimaadmin.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * TenantFilter extracts tenant id from request (header or host) and stores it in TenantContext.
 * It also verifies the authenticated principal's tenant if available.
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    // Setter so SecurityConfig can inject the JwtUserExtractor when creating the bean programmatically
    public void setJwtUserExtractor(JwtUserExtractor jwtUserExtractor) {
        this.jwtUserExtractor = jwtUserExtractor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            log.debug("[TenantFilter] incoming request: {} {}", request.getMethod(), request.getRequestURI());

            // Try to resolve tenant from request first (header or host)
            Map<String, List<String>> tenantMap = new HashMap<>();
            if(jwtUserExtractor != null){
                tenantMap.put("organizationIds", jwtUserExtractor.getCurrentOrganizations());
            }
            // 1) Check header (commonly used): X-Tenant-Id (comma separated if multiple)
            String tenantHeader = request.getHeader("X-Tenant-Id");
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                List<String> orgs = Arrays.stream(tenantHeader.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                tenantMap.put("organizationIds", orgs);
                log.debug("[TenantFilter] resolved tenant(s) from header X-Tenant-Id: {}", orgs);
            }

            // 2) If not present, try to infer from host (subdomain). Example: org1.api.example.com
            if (!tenantMap.containsKey("organizationIds")) {
                String host = request.getHeader("Host");
                if (host != null && host.contains(".")) {
                    String sub = host.split("\\.")[0];
                    if (!sub.equalsIgnoreCase("localhost") && !sub.matches("\\d+")) {
                        tenantMap.put("organizationIds", List.of(sub));
                        log.debug("[TenantFilter] resolved tenant from host subdomain: {}", sub);
                    }
                }
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // 3) If authenticated and jwtUserExtractor is available, augment tenant info from JWT claims
            if (auth != null) {
                if (jwtUserExtractor != null) {
                    try {
                        List<String> groups = jwtUserExtractor.getCurrentGroups();
                        tenantMap.putIfAbsent("Roles", groups.stream().filter(group -> group.startsWith("ROLE_")).toList());

                        List<String> orgsFromJwt = jwtUserExtractor.getCurrentOrganizations();
                        if (orgsFromJwt != null && !orgsFromJwt.isEmpty()) {
                            // Merge with any previously resolved orgs
                            List<String> existing = tenantMap.getOrDefault("organizationIds", Collections.emptyList());
                            Set<String> merged = new LinkedHashSet<>(existing);
                            merged.addAll(orgsFromJwt);
                            tenantMap.put("organizationIds", new ArrayList<>(merged));
                        }
                        log.debug("[TenantFilter] augmented tenant info from JWT for user {}: {}", auth.getName(), tenantMap);
                    } catch (Exception e) {
                        log.warn("[TenantFilter] failed to extract tenant info from JWT: {}", e.getMessage());
                    }
                } else {
                    log.debug("[TenantFilter] jwtUserExtractor not available, skipping JWT augmentation");
                }

                // Fallback: if no roles were resolved from JWT (e.g. dev mode mock auth),
                // read them from the SecurityContext authentication authorities
                List<String> resolvedRoles = tenantMap.getOrDefault("Roles", Collections.emptyList());
                if (resolvedRoles.isEmpty() && auth.getAuthorities() != null) {
                    List<String> authRoles = auth.getAuthorities().stream()
                            .map(a -> a.getAuthority())
                            .filter(a -> a.startsWith("ROLE_"))
                            .toList();
                    if (!authRoles.isEmpty()) {
                        tenantMap.put("Roles", authRoles);
                        log.debug("[TenantFilter] resolved roles from authentication authorities: {}", authRoles);
                    }
                }
            }

            String organizationIdHeader = request.getHeader("X-Organization-Id");
            if (organizationIdHeader != null && !organizationIdHeader.isBlank()) {
                tenantMap.put("organizationIds", List.of(organizationIdHeader));
                log.debug("[TenantFilter] resolved tenant from header X-Organization-Id: {}", organizationIdHeader);
            }


            // If we resolved anything, set the context so downstream code (repositories, services) can use it
            if (!tenantMap.isEmpty()) {
                TenantContext.setCurrentTenant(tenantMap);
                log.debug("[TenantFilter] Set TenantContext: {}", tenantMap);
            } else {
                log.debug("[TenantFilter] No tenant information resolved for request {}", request.getRequestURI());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // CRITICAL: Prevent memory leaks
            AuditContextSupplier.clearOrganizationId();
            AuditContextSupplier.clearCurrentUserId();
            AuditContextSupplier.clearActionSource();
            AuditContextSupplier.clearOldSnapshotJson();
            AuditContextSupplier.clearNewSnapshotEntity();
        }

    }

}
