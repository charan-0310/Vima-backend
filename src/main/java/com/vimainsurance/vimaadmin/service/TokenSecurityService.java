package com.vimainsurance.vimaadmin.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for cryptographic token operations.
 * Handles generation, hashing, and verification of enrollment tokens.
 * 
 * Security features:
 * - 256-bit cryptographically secure random tokens
 * - Deterministic tokens (HMAC of invitation id) for "same link until window end"
 * - SHA-256 one-way hashing
 * - Constant-time comparison to prevent timing attacks
 * - URL-safe Base64 encoding
 */
@Service
public class TokenSecurityService {
    private static final Logger logger = LoggerFactory.getLogger(TokenSecurityService.class);
    
    private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.enrollment-token-secret:default-enrollment-token-secret-change-in-production}")
    private String enrollmentTokenSecret;
    
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
     * Generates a deterministic token for an enrollment invitation id.
     * The same invitation id always produces the same token, so the same magic link
     * can be sent in the first email and in reminders (valid until window end).
     *
     * @param invitationId The enrollment invitation id (must not be null)
     * @return URL-safe token string derived from the invitation id
     */
    public String generateTokenForInvitation(UUID invitationId) {
        if (invitationId == null) {
            throw new IllegalArgumentException("Invitation id cannot be null");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                enrollmentTokenSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(invitationId.toString().getBytes(StandardCharsets.UTF_8));
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
            logger.debug("Generated deterministic token for invitation: {}...", token.substring(0, Math.min(8, token.length())));
            return token;
        } catch (Exception e) {
            logger.error("Failed to generate deterministic token", e);
            throw new RuntimeException("Failed to generate enrollment token", e);
        }
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
