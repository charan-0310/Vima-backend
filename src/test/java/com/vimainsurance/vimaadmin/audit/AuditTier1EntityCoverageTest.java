package com.vimainsurance.vimaadmin.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * F-18: Guards Tier 1 sensitive actions remain wired to the audit pipeline in service code.
 * Uses source scan (no Spring context) so refactors that drop audit strings fail CI.
 */
class AuditTier1EntityCoverageTest {

    private static final Path SERVICE_IMPL =
            Paths.get("src/main/java/com/vimainsurance/vimaadmin/service/serviceimpl");

    private static final Set<String> TIER1_ENTITY_TYPES = Set.of(
            "CLAIM_SETTLEMENT",
            "CLAIM_DEDUCTION",
            "CLAIM_DOCUMENT",
            "CLAIM_QUERY",
            "ORG_BROADCAST_EMAIL",
            "ORG_EMPLOYEE_LOGIN",
            "ORG_EMPLOYEE_HEALTH_CARD_EMAIL",
            "POLICY_DOCUMENT");

    private static final Set<String> TIER1_ACTION_MARKERS = Set.of(
            "MANUAL_ADD",
            "MANUAL_DELETE");

    @Test
    void tier1EntityTypesPresentInServiceAuditCalls() throws IOException {
        String corpus = readAllServiceImplSources();
        List<String> missingEntities = TIER1_ENTITY_TYPES.stream()
                .filter(e -> !corpus.contains("\"" + e + "\""))
                .sorted()
                .toList();
        assertTrue(
                missingEntities.isEmpty(),
                "Tier 1 entity_type missing from service audit calls: " + missingEntities);
    }

    @Test
    void manualEmployeeMutationsUsePlatformAudit() throws IOException {
        String corpus = readAllServiceImplSources();
        List<String> missingActions = TIER1_ACTION_MARKERS.stream()
                .filter(a -> !corpus.contains("\"" + a + "\""))
                .sorted()
                .toList();
        assertTrue(
                missingActions.isEmpty(),
                "Tier 1 manual employee actions missing from audit publisher: " + missingActions);
    }

    @Test
    void auditedOperationAnnotationStillUsedInServices() throws IOException {
        String corpus = readAllServiceImplSources();
        assertTrue(
                corpus.contains("@AuditedOperation"),
                "Expected @AuditedOperation on service mutations");
        assertTrue(
                corpus.contains("PlatformAuditPublisher"),
                "Expected PlatformAuditPublisher for non-AOP audit paths");
    }

    private static String readAllServiceImplSources() throws IOException {
        if (!Files.isDirectory(SERVICE_IMPL)) {
            throw new IllegalStateException("Run test from Vima-backend module root: " + SERVICE_IMPL);
        }
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> paths = Files.walk(SERVICE_IMPL)) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    sb.append(Files.readString(p));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read " + p, e);
                }
            });
        }
        return sb.toString();
    }
}
