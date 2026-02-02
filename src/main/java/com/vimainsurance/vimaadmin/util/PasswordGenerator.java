package com.vimainsurance.vimaadmin.util;

import java.security.SecureRandom;

public class PasswordGenerator {
    
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}|;:',.<>?/`~";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Generate a random password with letters, numbers, and symbols of specified length
     * @param length Length of the password (8-20 characters)
     * @return Random password with letters, numbers, and symbols
     */
    public static String generateRandomPassword(int length) {
        if (length < 8 || length > 20) {
            throw new IllegalArgumentException("Password length must be between 8 and 20 characters");
        }
        
        StringBuilder password = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            password.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
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
