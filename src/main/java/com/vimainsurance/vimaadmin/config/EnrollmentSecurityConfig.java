package com.vimainsurance.vimaadmin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for enrollment security features.
 * Enables AOP for rate limiting and other enrollment-related aspects.
 */
@Configuration
@EnableAspectJAutoProxy
public class EnrollmentSecurityConfig {
    // AOP aspects will be auto-detected and registered
    // No additional configuration needed
}
