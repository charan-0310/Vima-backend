package com.vimainsurance.vimaadmin.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * F-08 — Field-level encryption service for PII columns.
 *
 * Uses AES/GCM/NoPadding with a 256-bit data key. The data key is sourced from
 * {@code vima.pii.encryption-key} which in production is provided via AWS KMS
 * envelope encryption (see manual env todo). The wire format is a Base64 string of:
 *
 *   [1 byte version][12 byte IV][N bytes ciphertext+tag]
 *
 * version=1 — direct AES-GCM with the configured data key.
 * Future versions can introduce per-row data keys without breaking existing rows.
 *
 * Sibling classes:
 *   - {@link EncryptedStringConverter}     — JPA AttributeConverter for opaque PII
 *   - {@link DeterministicHashConverter}   — produces SHA-256 hash of the normalised plaintext for indexed lookups
 *
 * Apply via {@code @Convert(converter = EncryptedStringConverter.class)} on the entity field
 * once the Flyway phase-1 migration has added the new ciphertext column.
 */
@Service
public class PiiCryptoService {

    private static final byte VERSION = 0x01;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final String ALGO = "AES/GCM/NoPadding";

    @Value("${vima.pii.encryption-key:}")
    private String base64Key;

    private SecretKeySpec keySpec;
    private final SecureRandom rng = new SecureRandom();

    private SecretKeySpec key() {
        if (keySpec == null) {
            if (base64Key == null || base64Key.isBlank()) {
                throw new IllegalStateException(
                        "F-08: vima.pii.encryption-key is not configured. Set VIMA_PII_ENCRYPTION_KEY "
                                + "(base64-encoded 32-byte AES key, sourced from KMS envelope encryption).");
            }
            byte[] raw = Base64.getDecoder().decode(base64Key);
            if (raw.length != 32) {
                throw new IllegalStateException("F-08: vima.pii.encryption-key must decode to 32 bytes (AES-256).");
            }
            keySpec = new SecretKeySpec(raw, "AES");
        }
        return keySpec;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            rng.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[1 + IV_LENGTH + ct.length];
            out[0] = VERSION;
            System.arraycopy(iv, 0, out, 1, IV_LENGTH);
            System.arraycopy(ct, 0, out, 1 + IV_LENGTH, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("F-08: PII encryption failed", e);
        }
    }

    public String decrypt(String ciphertextB64) {
        if (ciphertextB64 == null) return null;
        try {
            byte[] in = Base64.getDecoder().decode(ciphertextB64);
            if (in.length < 1 + IV_LENGTH + 1 || in[0] != VERSION) {
                throw new IllegalArgumentException("Unsupported PII ciphertext version: " + (in.length > 0 ? in[0] : -1));
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(in, 1, iv, 0, IV_LENGTH);
            byte[] ct = new byte[in.length - 1 - IV_LENGTH];
            System.arraycopy(in, 1 + IV_LENGTH, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("F-08: PII decryption failed", e);
        }
    }

    /** SHA-256 hash of the normalised value (lower-cased, trimmed). For indexed lookups (e.g. find by email). */
    public String hash(String value) {
        if (value == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(value.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException("F-08: PII hashing failed", e);
        }
    }
}
