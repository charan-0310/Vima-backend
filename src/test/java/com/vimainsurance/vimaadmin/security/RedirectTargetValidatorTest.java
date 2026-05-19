package com.vimainsurance.vimaadmin.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vimainsurance.vimaadmin.config.RedirectProperties;

class RedirectTargetValidatorTest {

    private RedirectTargetValidator validator;

    @BeforeEach
    void setUp() {
        RedirectProperties properties = new RedirectProperties();
        properties.setUrl("https://www.vimainsurance.com");
        properties.setAllowedTargets(List.of(
                "https://app.vimainsurance.com",
                "http://localhost:5173"));
        validator = new RedirectTargetValidator(properties);
        validator.init();
    }

    @Test
    void allowsBlank() {
        assertTrue(validator.isAllowed(null));
        assertTrue(validator.isAllowed("   "));
    }

    @Test
    void allowsConfiguredOriginAndPath() {
        assertTrue(validator.isAllowed("https://app.vimainsurance.com/login"));
        assertTrue(validator.isAllowed("http://localhost:5173/keycloak/callback"));
    }

    @Test
    void rejectsUnknownHostAndDangerousSchemes() {
        assertFalse(validator.isAllowed("https://evil.example/login"));
        assertFalse(validator.isAllowed("javascript:alert(1)"));
        assertFalse(validator.isAllowed("//evil.example/path"));
    }

    @Test
    void requireAllowedThrowsForDisallowed() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.requireAllowed("https://evil.example/"));
    }
}
