package com.vimainsurance.vimaadmin.service.wellness.mantra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.vimainsurance.vimaadmin.config.MantraCareProperties;
import com.vimainsurance.vimaadmin.service.wellness.exception.MantraCareSigningUnavailableException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

class MantraCareSigningServiceTest {

    @Test
    void signForEmployee_whenKeyConfigured_producesJwtWithClaims() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----";

        MantraCareProperties props = new MantraCareProperties();
        props.setKeyId(4);
        props.setPrivateKeyPem(pem);

        MantraCareSigningService svc = new MantraCareSigningService(props);
        ReflectionTestUtils.invokeMethod(svc, "loadPrivateKey");

        assertTrue(svc.isSigningReady());
        String jwt = svc.signForEmployee(4, "INVITE", 300_000L, "wellness-uuid-1");

        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(keyPair.getPublic())
                .build()
                .parseClaimsJws(jwt);

        assertEquals("4", jws.getHeader().get("kid"));
        assertEquals("wellness-uuid-1", jws.getBody().get("user_identifier"));
        assertEquals("INVITE", jws.getBody().get("invite_code"));
    }

    @Test
    void signForEmployee_whenNotConfigured_throws() {
        MantraCareProperties props = new MantraCareProperties();
        MantraCareSigningService svc = new MantraCareSigningService(props);
        ReflectionTestUtils.invokeMethod(svc, "loadPrivateKey");
        assertFalse(svc.isSigningReady());
        assertThrows(MantraCareSigningUnavailableException.class, () -> svc.signForEmployee(4, "INV", 300_000L, "u"));
    }
}
