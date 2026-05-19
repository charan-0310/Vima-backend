package com.vimainsurance.vimaadmin.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogSensitiveDataMaskerTest {

    @Test
    void masksBearerToken() {
        String in = "Auth header Bearer eyJhbGciOiJIUzI1NiJ9.abc.def for user";
        String out = LogSensitiveDataMasker.maskMessage(in);
        assertFalse(out.contains("eyJhbGci"));
        assertTrue(out.contains("Bearer [REDACTED]"));
    }

    @Test
    void masksStandaloneJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String out = LogSensitiveDataMasker.maskMessage("token=" + jwt);
        assertFalse(out.contains("eyJhbGci"));
        assertTrue(out.contains("[REDACTED]_JWT"));
    }

    @Test
    void masksAadhaarAndPan() {
        String in = "customer 1234 5678 9012 pan ABCDE1234F";
        String out = LogSensitiveDataMasker.maskMessage(in);
        assertTrue(out.contains("XXXX XXXX 9012"));
        assertTrue(out.contains("ABCXXXXX4F"));
        assertFalse(out.contains("1234 5678 9012"));
    }

    @Test
    void masksEmailAndPhone() {
        String in = "notify user@example.com on 9876543210";
        String out = LogSensitiveDataMasker.maskMessage(in);
        assertFalse(out.contains("user@example.com"));
        assertTrue(out.contains("@example.com"));
        assertFalse(out.contains("9876543210"));
        assertTrue(out.contains("3210"));
    }

    @Test
    void masksKeyValueSecrets() {
        String in = "config client_secret=GOCSPX-super-secret-value";
        String out = LogSensitiveDataMasker.maskMessage(in);
        assertFalse(out.contains("GOCSPX"));
        assertTrue(out.toLowerCase().contains("client_secret=[REDACTED]".toLowerCase())
                || out.contains("client_secret=" + "[REDACTED]"));
    }
}
