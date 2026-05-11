package com.vimainsurance.vimaadmin.security;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * F-03 — Bridge filter that lets the OAuth2 Resource Server BearerTokenAuthenticationFilter
 * read the JWT from the {@code vima_at} httpOnly cookie when no Authorization header is set.
 *
 * Order: must run BEFORE Spring Security so the wrapped request reaches BearerTokenResolver.
 *
 * Compatible with the current frontend (which sends Authorization: Bearer ...). Once the SPA
 * is cut over to credentialed cookies, the Authorization header path can be removed.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class CookieToBearerFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String existing = request.getHeader("Authorization");
        if (existing != null && existing.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response);
            return;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            chain.doFilter(request, response);
            return;
        }
        for (Cookie c : cookies) {
            if (AuthCookieService.ACCESS_COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                final String bearer = "Bearer " + c.getValue();
                HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("Authorization".equalsIgnoreCase(name)) return bearer;
                        return super.getHeader(name);
                    }
                    @Override
                    public java.util.Enumeration<String> getHeaders(String name) {
                        if ("Authorization".equalsIgnoreCase(name)) {
                            return java.util.Collections.enumeration(java.util.List.of(bearer));
                        }
                        return super.getHeaders(name);
                    }
                };
                chain.doFilter(wrapped, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
