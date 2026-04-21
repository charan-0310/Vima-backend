package com.vimainsurance.vimaadmin.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * <p><b>How to generate a sample JWT</b></p>
 *
 * <ol>
 *   <li><b>Quick (random key, local only)</b> — Run with system property {@code mantra.sample=true}
 *       (see below). Then open the
 *       <b>Reported values</b> / test output for {@code mantra-sample-jwt}, or read
 *       {@code target/mantra-sample-output.txt}. MantraCare’s API will <b>reject</b> the token until they
 *       register the public key.</li>
 *   <li><b>Real (MantraCare-registered key)</b> — Put your PKCS#8 PEM in a file, then either:
 *       <ul>
 *         <li>Load the file in code and call {@link MantraCareTokenGenerator#parseRsaPrivateKeyFromPem(String)}
 *             and {@link MantraCareTokenGenerator#generateSignedJwt(MantraCareTokenClaims, RSAPrivateKey)}, or</li>
 *         <li>Use OpenSSL to confirm PEM format:
 *           <pre>{@code
 * openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt -out partner-private.pem
 * openssl rsa -in partner-private.pem -pubout -out partner-public.pem
 *           }</pre>
 *           Send {@code partner-public.pem} to MantraCare; sign with {@code partner-private.pem}.</li>
 *       </ul>
 *   </li>
 *   <li><b>Postman</b> — Body: {@code {"token":"<paste JWT>"}} to
 *       {@code POST https://api.mantracare.org/partner/user} with {@code Content-Type: application/json}.</li>
 * </ol>
 *
 * <p><b>How to run this test</b> (it is skipped unless you opt in — avoids noise in CI):</p>
 * <ul>
 *   <li><b>Maven:</b> {@code mvn test -Dtest=MantraCareTokenGeneratorManualSampleTest -Dmantra.sample=true}</li>
 *   <li><b>IntelliJ / Cursor:</b> Run Configuration → Modify options → Add VM options →
 *       {@code -Dmantra.sample=true}, then run {@code printSampleJwtWithEphemeralKey}.</li>
 * </ul>
 */
class MantraCareTokenGeneratorManualSampleTest {

    @Test
    // @EnabledIfSystemProperty(named = "mantra.sample", matches = "true")
    void printSampleJwtWithEphemeralKey(TestReporter reporter) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();

        MantraCareTokenGenerator.MantraCareTokenClaims claims =
                new MantraCareTokenGenerator.MantraCareTokenClaims(
                        1,
                        "+919876543210",
                        "VIMA",
                        "1",
                        null);

        String jwt = MantraCareTokenGenerator.generateSignedJwt(claims, privateKey);

        String pubDer = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

        // Shown in IntelliJ / Eclipse "Reported" / stdout section of the test node (more reliable than println alone).
        reporter.publishEntry("mantra-sample-jwt", jwt);
        reporter.publishEntry("mantra-sample-public-spki-base64", pubDer);

        Path out = Path.of("target/mantra-sample-output.txt");
        Files.createDirectories(out.getParent());
        String fileBody = """
                Postman body (raw JSON):
                {"token":"%s"}

                SPKI public key Base64 (if MantraCare asks):
                %s
                """.formatted(jwt, pubDer);
        Files.writeString(out, fileBody);
        reporter.publishEntry("mantra-sample-file", out.toAbsolutePath().normalize().toString());

        // Stderr is less often swallowed than stdout in some runners.
        System.err.println();
        System.err.println("[MantraCare sample] JWT (also in target/mantra-sample-output.txt):");
        System.err.println(jwt);
        System.err.println();
    }
}
