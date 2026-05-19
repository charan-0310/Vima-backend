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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

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
 * React performs Keycloak PKCE login; this API validates JWTs via OAuth2 Resource Server only.
 * Public: /health, /actuator/**, /public/**, enrollment magic-link paths (F-09).
 * F-20: Swagger is not anonymously accessible when springdoc is disabled (prod).
 * See docs/prd/audit/F-20_Authentication_Architecture.md.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final org.slf4j.Logger securityLogger =
            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

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

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean springdocApiDocsEnabled;

    /**
     * F-07 fail-fast guard. Throws on startup if a permissive profile (dev/local/test) is active
     * AND the deployment is running on a production-like environment. Catches the misconfiguration
     * where SPRING_PROFILES_ACTIVE accidentally includes "dev" on a prod EC2 instance.
     *
     * Production-like is identified by either:
     *   - VIMA_DEPLOY_ENV=prod / stage / uat (set in the EC2 user data / ECS task definition), or
     *   - the host name containing "vimainsurance.com".
     *
     * The bypass can still be intentionally allowed by setting VIMA_DEV_AUTH_BYPASS_ENABLED=true.
     */
    @jakarta.annotation.PostConstruct
    public void enforceProfileSafety() {
        java.util.Set<String> active = new java.util.HashSet<>(java.util.Arrays.asList(environment.getActiveProfiles()));
        boolean permissiveProfile = active.contains("dev") || active.contains("local") || active.contains("test");
        if (!permissiveProfile) {
            return;
        }
        String deployEnv = System.getenv("VIMA_DEPLOY_ENV");
        boolean isProdLikeDeploy = deployEnv != null
                && java.util.Arrays.asList("prod", "stage", "uat").contains(deployEnv.toLowerCase());
        String hostName = "";
        try {
            hostName = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            // best effort
        }
        boolean isProdLikeHost = hostName != null && hostName.toLowerCase().contains("vimainsurance");
        boolean explicitlyAllowed = "true".equalsIgnoreCase(System.getenv("VIMA_DEV_AUTH_BYPASS_ENABLED"));

        if ((isProdLikeDeploy || isProdLikeHost) && !explicitlyAllowed) {
            String msg = String.format(
                    "FATAL: permissive Spring profile %s detected on production-like host (deployEnv=%s, host=%s). "
                            + "Refusing to start. Set SPRING_PROFILES_ACTIVE to prod/stage/uat or set "
                            + "VIMA_DEV_AUTH_BYPASS_ENABLED=true to acknowledge.",
                    active, deployEnv, hostName);
            securityLogger.error(msg);
            throw new IllegalStateException(msg);
        }
        securityLogger.warn("Permissive profile {} active. This must NEVER happen on a production host.", active);
    }

    /**
     * Filter to set up mock authentication when there is no real JWT (dev/local/test).
     * <p>On {@code local}/{@code dev}, if the client sends {@code Authorization: Bearer ...}, this filter does
     * <b>nothing</b> so {@link BearerTokenAuthenticationFilter} can populate {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken}.
     * Otherwise {@code /auth/me} and feature flags would always resolve as the seeded {@code dev-user} (full roles)
     * instead of the Keycloak user.
     * <p>Test profile: single role ROLE_VIMA_ADMIN for e2e without Bearer.
     */
    private class DevAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }
            String authz = request.getHeader(HttpHeaders.AUTHORIZATION);
            boolean bearerPresent = authz != null && authz.regionMatches(true, 0, "Bearer ", 0, 7);
            boolean isTestProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch("test"::equalsIgnoreCase);
            if (bearerPresent && !isTestProfile) {
                filterChain.doFilter(request, response);
                return;
            }
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
        // Dev/local/test profiles get mock auth bypass for easier local testing.
        boolean isDevProfileOnly = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p ->
                        "dev".equalsIgnoreCase(p)
                                || "local".equalsIgnoreCase(p)
                                || "test".equalsIgnoreCase(p));
        boolean isDevOrLocalProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "dev".equalsIgnoreCase(p) || "local".equalsIgnoreCase(p));

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            // F-10: API responses — security headers (SPA headers remain on Amplify/CDN)
            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(Customizer.withDefaults())
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000))
                    .referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicyHeader(permissions -> permissions.policy(
                            "geolocation=(), microphone=(), camera=()")));

        if (isDevProfileOnly) {
            // Dev mode only: bypass all authentication but set up a mock authentication
            // so @PreAuthorize checks pass. Test profile does NOT use this; it requires real JWT.
            http.authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            );

            if (isDevOrLocalProfile) {
                // Validate Bearer JWTs so Keycloak logins are not overwritten by mock dev-user (see DevAuthenticationFilter).
                http.oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
                http.addFilterAfter(new DevAuthenticationFilter(), BearerTokenAuthenticationFilter.class);
            } else {
                http.addFilterBefore(new DevAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
            }
            // TenantFilter must run after DevAuthenticationFilter so it sees JWT or mock authorities.
            http.addFilterAfter(tenantFilter(), DevAuthenticationFilter.class);
        } else {
            // Production mode: normal security
            // Note: TenantFilter will be added after OAuth2 Resource Server (line 198)
            // to ensure it can extract tenant from both headers/host AND JWT claims
            http.authorizeHttpRequests(auth -> {
                        var rules = auth
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/health", "/actuator/**", "/public/**").permitAll()
                            .requestMatchers("/api/v1/test").hasAnyAuthority("VIMA_ADMIN", "SALES_AGENT")
                            .requestMatchers("/favicon.ico").permitAll();
                        if (springdocApiDocsEnabled) {
                            rules.requestMatchers(
                                            "/v3/api-docs/**",
                                            "/swagger-ui/**",
                                            "/swagger-ui.html")
                                    .hasAnyAuthority("SUPER_ADMIN", "ADMIN", "VIMA_ADMIN");
                        } else {
                            rules.requestMatchers(
                                            "/v3/api-docs/**",
                                            "/swagger-ui/**",
                                            "/swagger-ui.html")
                                    .denyAll();
                        }
                        rules.requestMatchers("/api/v1/enrollment/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/enrollment-submissions/insertUpdate")
                            .permitAll()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().authenticated();
                    })
                    // OAuth2 Resource Server: JWT validation (Authentik for dev/prod, Keycloak for UAT - issuer-uri is profile-specific in application-*.properties)
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt
                                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                            )
                    )
                    // Legacy DaoAuthenticationProvider — used by OAuth2 client flows and filters that load UserDetails
                    .authenticationProvider(authenticationProvider())
                    // Add TenantFilter after OAuth2 Resource Server processes JWT
                    // This ensures tenant can be extracted from both headers/host AND JWT claims
                    .addFilterAfter(tenantFilter(), org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);
        }
        return http.build();
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

