package com.vimainsurance.vimaadmin.util;

import java.security.SecureRandom;

public class PasswordGenerator {
    
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Generate a random alphanumeric password of specified length
     * @param length Length of the password (8-10 characters)
     * @return Random alphanumeric password
     */
    public static String generateRandomPassword(int length) {
        if (length < 8 || length > 10) {
            throw new IllegalArgumentException("Password length must be between 8 and 10 characters");
        }
        
        StringBuilder password = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        
        return password.toString();
    }
    
    /**
     * Generate a random alphanumeric password of random length between 8-10 characters
     * @return Random alphanumeric password
     */
    public static String generateRandomPassword() {
        int length = 8 + RANDOM.nextInt(3); // Random length between 8-10
        return generateRandomPassword(length);
    }
}
