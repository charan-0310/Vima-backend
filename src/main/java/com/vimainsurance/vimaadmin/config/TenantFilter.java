package com.vimainsurance.vimaadmin.config;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * TenantFilter extracts tenant id from request (header or host) and stores it in TenantContext.
 * It also verifies the authenticated principal's tenant if available.
 */
public class TenantFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && !isSuperAdmin(auth)) {
                UUID companyId = jwtUserExtractor.getCurrentCompanyId();
                if (companyId != null) {
                    TenantContext.setCurrentTenant(companyId.toString());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // CRITICAL: Prevent memory leaks
        }

    }

    private boolean isSuperAdmin(Authentication auth) {
        if (auth == null) {
            return false;
        }

        // Check granted authorities on Authentication
        if (auth.getAuthorities() != null) {
            boolean hasAuthority = auth.getAuthorities().stream()
                    .anyMatch(a -> {
                        String ga = a.getAuthority();
                        return "ROLE_SUPERADMIN".equalsIgnoreCase(ga) || "SUPERADMIN".equalsIgnoreCase(ga);
                    });
            if (hasAuthority) {
                return true;
            }
        }

        // If principal is a Jwt, inspect common claims that may contain roles
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) principal;

            // check common "roles" claim
            Object rolesClaim = jwt.getClaims().get("roles");
            if (rolesClaim instanceof java.util.Collection) {
                for (Object r : (java.util.Collection<?>) rolesClaim) {
                    String rv = String.valueOf(r);
                    if ("ROLE_SUPERADMIN".equalsIgnoreCase(rv) || "SUPER_ADMIN".equalsIgnoreCase(rv)) {
                        return true;
                    }
                }
            }

            // check "authorities" claim if present
            Object auths = jwt.getClaims().get("authorities");
            if (auths instanceof java.util.Collection) {
                for (Object r : (java.util.Collection<?>) auths) {
                    String rv = String.valueOf(r);
                    if ("ROLE_SUPERADMIN".equalsIgnoreCase(rv) || "SUPER_ADMIN".equalsIgnoreCase(rv)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}

