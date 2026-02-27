package com.vimainsurance.vimaadmin.util;

import org.springframework.core.env.Environment;

/**
 * Utility class for environment-related operations
 */
public class EnvironmentUtil {
    
    // Private constructor to prevent instantiation
    private EnvironmentUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Checks if the current environment is production
     * 
     * @param environment Spring Environment instance
     * @return true if production profile is active, false otherwise
     */
    public static boolean isProductionEnvironment(Environment environment) {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return false;
        }
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the current environment is development
     * 
     * @param environment Spring Environment instance
     * @return true if dev profile is active, false otherwise
     */
    public static boolean isDevEnvironment(Environment environment) {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        // Also check default profile if no active profiles are set
        String[] defaultProfiles = environment.getDefaultProfiles();
        for (String profile : defaultProfiles) {
            if ("dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}

