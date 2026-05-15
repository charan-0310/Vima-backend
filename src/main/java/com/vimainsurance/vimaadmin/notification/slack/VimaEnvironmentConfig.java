package com.vimainsurance.vimaadmin.notification.slack;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

/**
 * Resolves {@link VimaEnvironment} once at startup from the Spring active
 * profile list and exposes it as a singleton bean so downstream services can
 * inject {@code VimaEnvironment} directly instead of re-deriving it.
 */
@Slf4j
@Configuration
public class VimaEnvironmentConfig {

    @Bean
    public VimaEnvironment vimaEnvironment(Environment springEnv) {
        String[] active = springEnv.getActiveProfiles();
        VimaEnvironment resolved = VimaEnvironment.fromActiveProfiles(active);
        log.info("vima_environment_resolved env={} springProfiles=[{}]",
                resolved,
                String.join(",", active));
        return resolved;
    }
}
