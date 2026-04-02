package com.vimainsurance.vimaadmin.config;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vimainsurance.vimaadmin.config.oauth.VimaOAuth2SuccessHandler;
import com.vimainsurance.vimaadmin.config.oauth.VimaOAuth2UserService;
import com.vimainsurance.vimaadmin.util.AdminUserDetailsService;
import com.vimainsurance.vimaadmin.util.CorrelationIdFilter;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security Configuration
 *
 * JWT tokens are validated via Spring Security OAuth2 Resource Server using the configured issuer-uri.
 * - Dev/Prod: Authentik (issuer-uri from application-dev.properties / application-prod.properties).
 * - UAT: Keycloak (issuer-uri from application-uat.properties: keycloak.auth-server-url + keycloak.realm).
 *
 * React performs login and token exchange - no login endpoints or callback endpoints are required here.
 * All endpoints under /api/** are secured and require a valid JWT token.
 * Public endpoints: /health, /actuator/**, /public/**
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private AdminUserDetailsService userDetailsService;

    @Autowired(required = false)
    private JwtUserExtractor jwtUserExtractor;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Autowired
    private AuthentikJwtAuthenticationConverter jwtAuthenticationConverter;
    
    @Autowired
    private Environment environment;

    /** Context path (e.g. /dev) so permitAll matchers work when request URI includes it. */
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    /**
     * Filter to set up mock authentication when no JWT (dev and test profiles).
     * Dev: all roles so local development has full access.
     * Test: single role ROLE_VIMA_ADMIN so auth/me and feature flags match e2e expectations (no JWT in test).
     */
    private class DevAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                boolean isTestProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch("test"::equalsIgnoreCase);
                java.util.List<SimpleGrantedAuthority> authorities = isTestProfile
                    ? Arrays.asList(new SimpleGrantedAuthority("ROLE_VIMA_ADMIN"))
                    : Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_VIMA_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_SALES_MANAGER"),
                        new SimpleGrantedAuthority("ROLE_SALES_AGENT"),
                        new SimpleGrantedAuthority("ROLE_HR_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                    );
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    isTestProfile ? "e2e-vima-admin" : "dev-user",
                    null,
                    authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request, response);
        }
    }

    // Bean to provide TenantFilter so it can be injected and reused
    @Bean
    public TenantFilter tenantFilter() {
        // requireTenant=false to allow unauthenticated public endpoints like health to function
        TenantFilter tf = new TenantFilter();
        // Inject jwtUserExtractor if available so the programmatically created filter has what it needs
        if (this.jwtUserExtractor != null) {
            tf.setJwtUserExtractor(this.jwtUserExtractor);
        }
        return tf;
    }

    /**
     * Security filter chain configuration
     *
     * Enables OAuth2 Resource Server with JWT validation for Authentik.
     * All /api/** endpoints are secured and require valid JWT tokens.
     *
     * In dev profile only, authentication is bypassed (mock auth). Test profile uses real JWT.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Dev profile gets mock auth bypass so magic-link enrollment (no JWT) works. Local/test/uat/prod use real JWT.
        boolean isDevProfileOnly = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "dev".equalsIgnoreCase(p) || "test".equalsIgnoreCase(p));

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable());

        if (isDevProfileOnly) {
            // Dev mode only: bypass all authentication but set up a mock authentication
            // so @PreAuthorize checks pass. Test profile does NOT use this; it requires real JWT.
            http.authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            );

            // DevAuthenticationFilter must run BEFORE TenantFilter so that
            // TenantFilter can read roles from the mock authentication context.
            http.addFilterBefore(new DevAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
            http.addFilterAfter(tenantFilter(), DevAuthenticationFilter.class);
        } else {
            // Production mode: normal security
            // Note: TenantFilter will be added after OAuth2 Resource Server (line 198)
            // to ensure it can extract tenant from both headers/host AND JWT claims
            http.authorizeHttpRequests(auth -> auth
                            // CORS preflight (OPTIONS) must be allowed without auth so browser gets CORS headers
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            // Public endpoints - no authentication required
                            .requestMatchers("/health", "/actuator/**", "/public/**").permitAll()

                            // Legacy endpoints that may need authentication - keeping for backward compatibility
                            // These should eventually be migrated to use JWT tokens
                            .requestMatchers("/api/v1/login", "/oauth2/**", "/api/v1/zoho/auth/**",
                                    "/api/v1/nonce", "/api/v1/auth/challenge", "/api/v1/auth/login").permitAll()
                            // Enrollment token validation - public (no JWT; token in path)
                            .requestMatchers("/api/v1/enrollments/**").permitAll()
                            .requestMatchers("/api/v1/enrollment-submissions/**").permitAll()
                            // Test endpoint - requires specific authorities
                            .requestMatchers("/api/v1/test").hasAnyAuthority("VIMA_ADMIN", "SALES_AGENT")

                            // Swagger/OpenAPI documentation - public access
                            .requestMatchers(
                                    "/v3/api-docs/**",
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/favicon.ico"
                            ).permitAll()

                            // Enrollment — public (token-based auth, no JWT). Magic-link flow: validateTokenAndGetContext, calculate-premium, etc.
                            .requestMatchers("/api/v1/enrollment/**").permitAll()
                            .requestMatchers("/api/v1/enrollment-submissions/**").permitAll()
                            // With context-path (e.g. /dev, /prod), request URI may include it — match explicitly so magic-link step 4 (calculate-premium) works
                            .requestMatchers("/dev/api/v1/enrollment/**").permitAll()
                            .requestMatchers("/dev/api/v1/enrollment-submissions/**").permitAll()
                            .requestMatchers("/prod/api/v1/enrollment/**").permitAll()
                            .requestMatchers("/prod/api/v1/enrollment-submissions/**").permitAll()
                            .requestMatchers(contextPath + "/api/v1/enrollment/**").permitAll()
                            .requestMatchers(contextPath + "/api/v1/enrollment-submissions/**").permitAll()

                            // All other /api/** endpoints require authentication via JWT
                            .requestMatchers("/api/**").authenticated()

                            // All other requests require authentication
                            .anyRequest().authenticated()
                    )
                    // OAuth2 Resource Server: JWT validation (Authentik for dev/prod, Keycloak for UAT - issuer-uri is profile-specific in application-*.properties)
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt
                                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                            )
                    )
                    // Keep authentication provider for backward compatibility with legacy endpoints
                    .authenticationProvider(authenticationProvider())
                    // Add TenantFilter after OAuth2 Resource Server processes JWT
                    // This ensures tenant can be extracted from both headers/host AND JWT claims
                    .addFilterAfter(tenantFilter(), org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);
        }
        return http.build();
    }

    // Legacy OAuth2 client configuration - kept for backward compatibility
    // This is not used when JWT tokens are validated via resource server
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new VimaOAuth2UserService();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return new VimaOAuth2SuccessHandler();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * CORS configuration
     * Allows cross-origin requests from configured origins
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        configuration.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Correlation ID filter registration
     * Adds correlation IDs to requests for tracing
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}

