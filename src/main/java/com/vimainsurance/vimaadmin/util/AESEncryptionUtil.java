package com.vimainsurance.vimaadmin.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class AESEncryptionUtil {
    private static final String ENCRYPTION_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_SIZE = 32; // 256 bits

    private static SecretKey SECRET_KEY;

    @Value("${vima.aes.key:}")
    private String propertyKey;

    @PostConstruct
    public void init() {
        String key = propertyKey;
        if (key == null || key.isEmpty()) {
            key = System.getenv("VIMA_AES_KEY");
        }
        if (key == null || key.length() != AES_KEY_SIZE) {
            throw new IllegalStateException("AES key must be set to 32 chars (256 bits) via vima.aes.key property or VIMA_AES_KEY env var");
        }
        SECRET_KEY = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
    }

    // For standalone use: load key from a properties file
    public static void loadKeyFromProperties(String propertiesFilePath) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesFilePath)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Could not load properties file: " + propertiesFilePath);
            throw e;
        }
        String key = props.getProperty("vima.aes.key");
        if (key == null) {
            System.err.println("vima.aes.key property not found in " + propertiesFilePath);
        } else {
            System.out.println("Loaded key from properties. Length: " + key.length());
        }
        if (key == null || key.length() != AES_KEY_SIZE) {
            throw new IllegalArgumentException("Key must be 32 characters (256 bits)");
        }
        SECRET_KEY = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        System.out.println("SECRET_KEY initialized successfully.");
    }

    public static String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, spec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    public static String decrypt(String cipherText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, spec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
} 