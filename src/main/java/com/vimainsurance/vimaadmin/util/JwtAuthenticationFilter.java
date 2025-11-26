package com.vimainsurance.vimaadmin.util;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Legacy JWT Authentication Filter - DISABLED
 * 
 * This filter has been disabled in favor of Spring Security OAuth2 Resource Server.
 * OAuth2 Resource Server automatically validates JWT tokens from Authentik using RS256 signatures.
 * 
 * The old filter attempted to validate tokens using symmetric keys (HS256), which doesn't work
 * with Authentik's RS256 (RSA) signed tokens.
 * 
 * DO NOT USE THIS FILTER - it will cause conflicts with OAuth2 Resource Server.
 */
// @Component - DISABLED: Use OAuth2 Resource Server instead
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthenticationFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                if (jwtUtil.isTokenExpired(jwt)) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "JWT token expired");
                    return;
                }
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("Exception in JwtAuthenticationFilter", e);
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT token or expired");
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
