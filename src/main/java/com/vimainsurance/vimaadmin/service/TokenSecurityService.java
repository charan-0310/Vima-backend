package com.vimainsurance.vimaadmin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for cryptographic token operations.
 * Handles generation, hashing, and verification of enrollment tokens.
 * 
 * Security features:
 * - 256-bit cryptographically secure random tokens
 * - SHA-256 one-way hashing
 * - Constant-time comparison to prevent timing attacks
 * - URL-safe Base64 encoding
 */
@Service
public class TokenSecurityService {
    private static final Logger logger = LoggerFactory.getLogger(TokenSecurityService.class);
    
    private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /**
     * Generates a cryptographically secure random token.
     * 
     * @return 32-byte (256-bit) token encoded as URL-safe Base64 string (43 characters)
     */
    public String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        
        // URL-safe Base64 encoding (no padding, no special chars that need escaping)
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Log only first 8 characters for debugging (never log full token)
        logger.debug("Generated token: {}...", token.substring(0, 8));
        
        return token;
    }
    
    /**
     * Hashes a raw token using SHA-256.
     * This is a one-way operation - the original token cannot be recovered from the hash.
     * 
     * @param rawToken The raw token to hash
     * @return Hex-encoded SHA-256 hash (64 characters)
     * @throws RuntimeException if SHA-256 algorithm is not available
     */
    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to hash token: SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Verifies a raw token against a stored hash using constant-time comparison.
     * 
     * Constant-time comparison prevents timing attacks where an attacker could
     * determine the hash by measuring how long the comparison takes.
     * 
     * @param rawToken The raw token to verify
     * @param storedHash The stored hash to compare against
     * @return true if the token matches the hash, false otherwise
     */
    public boolean verifyToken(String rawToken, String storedHash) {
        if (rawToken == null || storedHash == null) {
            return false;
        }
        
        try {
            // Compute hash of the provided token
            String computedHash = hashToken(rawToken);
            
            // Use constant-time comparison to prevent timing attacks
            // MessageDigest.isEqual() performs constant-time byte comparison
            return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            logger.error("Error verifying token", e);
            return false;
        }
    }
}
