package com.vimainsurance.vimaadmin.service.wellness.mantra;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.config.MantraCareProperties;
import com.vimainsurance.vimaadmin.service.wellness.exception.MantraCareSigningUnavailableException;
import com.vimainsurance.vimaadmin.util.MantraCareTokenGenerator;
import com.vimainsurance.vimaadmin.util.MantraCareTokenGenerator.MantraCareTokenClaims;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MantraCareSigningService {

    private static final Logger logger = LoggerFactory.getLogger(MantraCareSigningService.class);

    private final MantraCareProperties properties;

    private RSAPrivateKey rsaPrivateKey;
    private volatile boolean signingReady;

    @PostConstruct
    void loadPrivateKey() {
        try {
            String pem = resolvePemContent();
            if (pem == null || pem.isBlank()) {
                logger.warn("MantraCare private key not configured (mantracare.private-key-pem / mantracare.private-key-path); "
                        + "MantraCare redirects will return 503 until configured.");
                signingReady = false;
                return;
            }
            rsaPrivateKey = MantraCareTokenGenerator.parseRsaPrivateKeyFromPem(pem.trim());
            signingReady = true;
            logger.info("MantraCare RSA private key loaded successfully.");
        } catch (Exception e) {
            logger.warn("MantraCare private key failed to load: {}", e.getMessage());
            signingReady = false;
        }
    }

    private String resolvePemContent() throws Exception {
        if (properties.getPrivateKeyPem() != null && !properties.getPrivateKeyPem().isBlank()) {
            return properties.getPrivateKeyPem();
        }
        if (properties.getPrivateKeyPath() != null && !properties.getPrivateKeyPath().isBlank()) {
            String path = properties.getPrivateKeyPath().trim();
            if (path.startsWith("classpath:")) {
                String resourcePath = path.substring("classpath:".length()).trim();
                if (resourcePath.isEmpty()) {
                    return null;
                }
                ClassPathResource resource = new ClassPathResource(resourcePath);
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        }
        return null;
    }

    public boolean isSigningReady() {
        return signingReady && rsaPrivateKey != null;
    }

    public String signForEmployee(int keyId, String inviteCode, long tokenValidityMillis, String wellnessUserId) {
        if (!isSigningReady()) {
            throw new MantraCareSigningUnavailableException("MantraCare signing is not configured");
        }
        MantraCareTokenClaims claims = new MantraCareTokenClaims(
                keyId,
                wellnessUserId,
                inviteCode,
                String.valueOf(keyId),
                tokenValidityMillis);
        return MantraCareTokenGenerator.generateSignedJwt(claims, rsaPrivateKey);
    }
}
