package com.vimainsurance.vimaadmin.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.vimainsurance.vimaadmin.config.oauth.VimaOAuth2SuccessHandler;
import com.vimainsurance.vimaadmin.config.oauth.VimaOAuth2UserService;
import com.vimainsurance.vimaadmin.util.AdminUserDetailsService;
import com.vimainsurance.vimaadmin.util.CorrelationIdFilter;
import com.vimainsurance.vimaadmin.util.EnvironmentUtil;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

/**
 * Spring Security Configuration
 * 
 * This backend only validates JWT tokens issued by Authentik.
 * React performs login and token exchange - no login endpoints or callback endpoints are required here.
 * 
 * JWT tokens are validated against the configured issuer-uri (http://localhost:9000/application/o/vima/)
 * using Spring Security OAuth2 Resource Server.
 * 
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
     * Filter to set up mock authentication in dev mode
     * This allows @PreAuthorize checks to pass without actual JWT tokens
     */
    private static class DevAuthenticationFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // Create a mock authentication with all authorities
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "dev-user",
                    null,
                    Arrays.asList(
                        new SimpleGrantedAuthority("SUPER_ADMIN"),
                        new SimpleGrantedAuthority("ADMIN"),
                        new SimpleGrantedAuthority("VIMA_ADMIN"),
                        new SimpleGrantedAuthority("SALES_MANAGER"),
                        new SimpleGrantedAuthority("SALES_AGENT"),
                        new SimpleGrantedAuthority("HR_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                    )
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
     * In dev profile, authentication is bypassed for easier development.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Check if dev profile is active using EnvironmentUtil
        boolean isDevProfile = EnvironmentUtil.isDevEnvironment(environment);

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable());

        if (isDevProfile) {
            // Ensure tenant is resolved early in the chain for both environments
            // Add tenant filter before security filters so DB resolvers and auth can read tenant
            http.addFilterBefore(tenantFilter(), UsernamePasswordAuthenticationFilter.class);

            // Dev mode: bypass all authentication but set up a mock authentication
            // so @PreAuthorize checks pass
            http.authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            );

            // Add a filter to set up mock authentication in dev mode.
            // Place DevAuthenticationFilter after TenantFilter so tenant info is available to the mock auth.
            http.addFilterAfter(new DevAuthenticationFilter(), TenantFilter.class);
        } else {
            // Production mode: normal security
            // Note: TenantFilter will be added after OAuth2 Resource Server (line 198)
            // to ensure it can extract tenant from both headers/host AND JWT claims
            http.authorizeHttpRequests(auth -> auth
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

                            // Enrollment — public (token-based auth, no JWT)
                            .requestMatchers("/api/v1/enrollment/**").permitAll()
                            .requestMatchers("/api/v1/enrollment-submissions/**").permitAll()
                            // With context-path (e.g. /dev, /prod), request URI includes it — match explicitly
                            .requestMatchers("/dev/api/v1/enrollment-submissions/**").permitAll()
                            .requestMatchers("/prod/api/v1/enrollment-submissions/**").permitAll()
                            .requestMatchers(contextPath + "/api/v1/enrollment/**").permitAll()
                            .requestMatchers(contextPath + "/api/v1/enrollment-submissions/**").permitAll()

                            // All other /api/** endpoints require authentication via JWT
                            .requestMatchers("/api/**").authenticated()

                            // All other requests require authentication
                            .anyRequest().authenticated()
                    )
                    // Enable OAuth2 Resource Server for JWT validation
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

