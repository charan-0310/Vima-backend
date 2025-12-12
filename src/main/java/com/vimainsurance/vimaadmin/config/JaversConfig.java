package com.vimainsurance.vimaadmin.config;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.javers.core.diff.ListCompareAlgorithm;

import lombok.extern.slf4j.Slf4j;

/**
 * Javers configuration for entity auditing (diff-only mode)
 * Tracks only object diffs without database persistence
 * 
 * This configuration:
 * - Uses in-memory diff calculations only
 * - No database persistence
 * - No repository overhead
 * - Simple object comparison for change tracking
 */
@Slf4j
@Configuration
public class JaversConfig {

    /**
     * Creates Javers instance for diff-only tracking
     * No repository - only in-memory diff calculations
     * 
     * Note: Using minimal configuration to avoid Gson JsonElement adapter conflicts
     */
    @Bean
    public Javers javers() {
        // Use minimal configuration to avoid Gson adapter conflicts
        // The JsonElement adapter conflict occurs when Javers tries to register
        // its own adapter but one already exists in the classpath
        return JaversBuilder.javers()
                .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
                .build();
    }
}

