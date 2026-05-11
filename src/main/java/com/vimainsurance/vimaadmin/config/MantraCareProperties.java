package com.vimainsurance.vimaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Deployment-paired MantraCare signing credentials (PEM + key id). Operational HTTP defaults live in
 * {@code admin.wellness_partners.metadata} for the {@code mantracare} slug.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mantracare")
public class MantraCareProperties {

    /** Registered partner key id; must match MantraCare and the JWT {@code kid} header. */
    private int keyId = 4;

    /** Path to PKCS#8 PEM file (optional if {@link #privateKeyPem} is set). */
    private String privateKeyPath = "";

    /** Inline PKCS#8 PEM (optional if {@link #privateKeyPath} is set). */
    private String privateKeyPem = "";
}
