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
}

