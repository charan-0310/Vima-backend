package com.vimainsurance.vimaadmin.util;

import java.security.SecureRandom;

public class PasswordGenerator {
    
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}|;:',.<>?/`~";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:',.<>?/`~";

    /**
     * Generate a random password with letters, numbers, and symbols of specified length.
     * Guarantees at least one upper case, one lower case, and one digit so it satisfies
     * common Keycloak (and other) password policies.
     *
     * @param length Length of the password (8-20 characters)
     * @return Random password with letters, numbers, and symbols
     */
    public static String generateRandomPassword(int length) {
        if (length < 8 || length > 20) {
            throw new IllegalArgumentException("Password length must be between 8 and 20 characters");
        }

        StringBuilder password = new StringBuilder(length);
        // Ensure at least one of each required type (Keycloak: min upper, min lower, min digit)
        password.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        password.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));

        for (int i = 3; i < length; i++) {
            password.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }

        // Shuffle so required chars are not always at the start
        for (int i = password.length() - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = password.charAt(i);
            password.setCharAt(i, password.charAt(j));
            password.setCharAt(j, tmp);
        }

        return password.toString();
    }
    
    /**
     * Generate a random 12-character password with letters, numbers, and symbols
     * @return Random 12-character password
     */
    public static String generateRandomPassword() {
        return generateRandomPassword(12);
    }
}
