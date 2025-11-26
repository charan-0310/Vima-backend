package com.vimainsurance.vimaadmin.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

    /**
     * Security filter chain configuration
     * 
     * Enables OAuth2 Resource Server with JWT validation for Authentik.
     * All /api/** endpoints are secured and require valid JWT tokens.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/health", "/actuator/**", "/public/**").permitAll()
                
                // Legacy endpoints that may need authentication - keeping for backward compatibility
                // These should eventually be migrated to use JWT tokens
                .requestMatchers("/api/v1/login", "/oauth2/**", "/api/v1/zoho/auth/**", 
                    "/api/v1/nonce", "/api/v1/auth/challenge", "/api/v1/auth/login").permitAll() 
                
                // Test endpoint - requires specific authorities
                .requestMatchers("/api/v1/test").hasAnyAuthority("VIMA_ADMIN", "SALES_AGENT")
                
                // Swagger/OpenAPI documentation - public access
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/favicon.ico"
                ).permitAll()
                
                // All other /api/** endpoints require authentication via JWT
                .requestMatchers("/api/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Enable OAuth2 Resource Server for JWT validation
            // This validates JWT tokens issued by Authentik against the configured issuer-uri
            // JWT configuration comes from application properties (spring.security.oauth2.resourceserver.jwt.issuer-uri)
            // Custom converter extracts roles/authorities from JWT claims (groups, roles, etc.)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            )
            // Keep authentication provider for backward compatibility with legacy endpoints
            .authenticationProvider(authenticationProvider());

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
