package com.vimainsurance.vimaadmin.security.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * F-08 — JPA AttributeConverter that transparently encrypts a String field.
 *
 * Apply with {@code @Convert(converter = EncryptedStringConverter.class)} on the entity field.
 * Note: the converter must be marked autoApply=false so it does not silently apply to every
 * String column. Spring resolves the bean via {@link ApplicationContextAware} because Hibernate
 * instantiates converters reflectively without going through Spring's bean factory.
 *
 * Phased rollout (do NOT apply this annotation in the same release that adds the converter):
 *   1. Add a new {@code <field>_enc VARCHAR(8192)} column via Flyway.
 *   2. Backfill: SELECT id, plaintext_field; UPDATE … SET enc = AES_GCM(plaintext) (one-off job).
 *   3. Switch the entity to use the encrypted column with @Convert.
 *   4. Drop the plaintext column in a follow-up release.
 */
@Converter(autoApply = false)
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String>, ApplicationContextAware {

    private static volatile PiiCryptoService crypto;

    @Override
    public void setApplicationContext(org.springframework.context.ApplicationContext ctx) {
        crypto = ctx.getBean(PiiCryptoService.class);
    }

    @Autowired(required = false)
    public void setCryptoService(PiiCryptoService service) {
        crypto = service;
    }

    private static PiiCryptoService crypto() {
        if (crypto == null) {
            throw new IllegalStateException(
                    "F-08: PiiCryptoService not initialised. Ensure Spring boot completed and "
                            + "EncryptedStringConverter is registered as a Spring component.");
        }
        return crypto;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return crypto().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return crypto().decrypt(dbData);
    }
}
