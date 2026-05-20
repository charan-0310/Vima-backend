package com.vimainsurance.vimaadmin.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

class MantraCareTokenGeneratorTest {

    @Test
    void generateSignedJwt_includesKidHeaderAndMantraClaims() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();

        MantraCareTokenGenerator.MantraCareTokenClaims claims =
                new MantraCareTokenGenerator.MantraCareTokenClaims(
                        1,
                        "+919876543210",
                        "VIMA_TEST",
                        "1",
                        null);

        String jwt = MantraCareTokenGenerator.generateSignedJwt(claims, privateKey);

        Jws<Claims> jws = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(jwt);

        assertEquals("1", jws.getHeader().get("kid"));
        assertEquals(1, ((Number) jws.getPayload().get("key_id")).intValue());
        assertEquals("+919876543210", jws.getPayload().get("user_identifier"));
        assertEquals("VIMA_TEST", jws.getPayload().get("invite_code"));
    }

    @Test
    void parseRsaPrivateKeyFromPem_roundTripsPkcs8() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        RSAPrivateKey original = (RSAPrivateKey) kpg.generateKeyPair().getPrivate();

        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(original.getEncoded())
                + "\n-----END PRIVATE KEY-----";

        RSAPrivateKey parsed = MantraCareTokenGenerator.parseRsaPrivateKeyFromPem(pem);
        assertNotNull(parsed);
        assertEquals(original.getModulus(), parsed.getModulus());
    }
}
