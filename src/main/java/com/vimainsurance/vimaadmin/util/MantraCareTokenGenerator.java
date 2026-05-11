package com.vimainsurance.vimaadmin.util;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Builds RS256-signed JWTs for MantraCare {@code POST /partner/user} ({@code token} field).
 * <p>
 * MantraCare expects a {@code kid} (key id) in the JWT <strong>header</strong> and
 * {@code user_identifier} and {@code invite_code} in the payload.
 * If MantraCare later requires JWE wrapping with their public key, add a separate step
 * (e.g. Nimbus JOSE) — this class only produces the inner signed JWT.
 */
public final class MantraCareTokenGenerator {

    private static final String HEADER_KID = "kid";

    private MantraCareTokenGenerator() {}

    /**
     * @param kid            Key identifier in the JWT header; must match the key registered at MantraCare
     *                       (often the same as {@link MantraCareTokenClaims#keyId()} as a string).
     * @param claims         Payload fields required by MantraCare.
     * @param rsaPrivateKey  Partner RSA private key (PKCS#8).
     * @param validityMillis Token lifetime from {@code iat}; use {@link MantraCareTokenClaims#DEFAULT_VALIDITY_MILLIS}
     *                       or set {@link MantraCareTokenClaims#validityMillis()} on the record.
     */
    public static String generateSignedJwt(
            String kid,
            MantraCareTokenClaims claims,
            RSAPrivateKey rsaPrivateKey) {
        Objects.requireNonNull(kid, "kid");
        Objects.requireNonNull(claims, "claims");
        Objects.requireNonNull(rsaPrivateKey, "rsaPrivateKey");

        long validity = claims.validityMillis() != null ? claims.validityMillis() : MantraCareTokenClaims.DEFAULT_VALIDITY_MILLIS;
        Date now = new Date();
        Date exp = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setHeaderParam(HEADER_KID, kid)
                .claim("key_id", claims.keyId())
                .claim("user_identifier", claims.userIdentifier())
                .claim("invite_code", claims.inviteCode())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(rsaPrivateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Convenience overload: uses {@link MantraCareTokenClaims#kid()} when non-null, otherwise
     * {@link MantraCareTokenClaims#keyId()} as string.
     */
    public static String generateSignedJwt(MantraCareTokenClaims claims, RSAPrivateKey rsaPrivateKey) {
        Objects.requireNonNull(claims, "claims");
        String kid = claims.kid() != null ? claims.kid() : String.valueOf(claims.keyId());
        return generateSignedJwt(kid, claims, rsaPrivateKey);
    }

    /**
     * Loads an RSA private key from a PEM string ({@code -----BEGIN PRIVATE KEY-----} PKCS#8).
     */
    public static RSAPrivateKey parseRsaPrivateKeyFromPem(String pem) {
        Objects.requireNonNull(pem, "pem");
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PrivateKey key = factory.generatePrivate(spec);
            if (!(key instanceof RSAPrivateKey rsa)) {
                throw new IllegalArgumentException("PEM did not decode to an RSA private key");
            }
            return rsa;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PKCS#8 RSA private key PEM", e);
        }
    }

    /**
     * Claims for MantraCare partner JWT.
     *
     * @param keyId          Registered partner key id used to derive {@code kid} header when explicit {@code kid} is absent.
     * @param userIdentifier Stable user id at the partner (e.g. phone E.164).
     * @param inviteCode     Partner invite code.
     * @param kid            Optional header {@code kid}; if null, {@link #generateSignedJwt(MantraCareTokenClaims, RSAPrivateKey)} uses {@code String.valueOf(keyId)}.
     * @param validityMillis Optional JWT lifetime; defaults to {@link #DEFAULT_VALIDITY_MILLIS}.
     */
    public record MantraCareTokenClaims(
            int keyId,
            String userIdentifier,
            String inviteCode,
            String kid,
            Long validityMillis) {

        public static final long DEFAULT_VALIDITY_MILLIS = 60 * 60 * 1000L;

        public MantraCareTokenClaims(int keyId, String userIdentifier, String inviteCode) {
            this(keyId, userIdentifier, inviteCode, null, null);
        }

        public MantraCareTokenClaims {
            Objects.requireNonNull(userIdentifier, "userIdentifier");
            Objects.requireNonNull(inviteCode, "inviteCode");
        }
    }
}
