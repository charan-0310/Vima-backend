package com.vimainsurance.vimaadmin.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoder {
    
    /**
     * Encodes a password using SHA-256 followed by bcrypt
     * This matches the frontend implementation: bcrypt(SHA-256(password))
     */
    public static String encodePassword(String rawPassword) {
        try {
            // Step 1: Hash with SHA-256
            String sha256Hash = computeSHA256(rawPassword);
            System.out.println("SHA-256 Hash: " + sha256Hash);
            // Step 2: Encode with bcrypt
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String bcryptHash = encoder.encode(sha256Hash);
            
            return bcryptHash;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode password", e);
        }
    }
    
    /**
     * Verifies a password by comparing SHA-256 hash with bcrypt hash
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        try {
            // Step 1: Hash with SHA-256
            String sha256Hash = computeSHA256(rawPassword);
            
            // Step 2: Verify with bcrypt
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            return encoder.matches(sha256Hash, encodedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify password", e);
        }
    }
    
    /**
     * Computes SHA-256 hash of a string
     */
    private static String computeSHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        String password = "password123";
        
        // Encode password
        String encoded = encodePassword(password);
        System.out.println("Encoded password: " + encoded);
        
        // Verify password
        boolean matches = matches(password, encoded);
        System.out.println("Password matches: " + matches);
        
        // Test with wrong password
        boolean wrongMatches = matches("wrongpassword", encoded);
        System.out.println("Wrong password matches: " + wrongMatches);
    }
}

