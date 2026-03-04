package com.vimainsurance.vimaadmin.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.audit.entity.AuditPending;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditEventWriter auditEventWriter;

    @Mock
    private AuditPendingRepository auditPendingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditServiceImpl(auditEventWriter, auditPendingRepository, objectMapper);
    }

    /**
     * Payload as would be produced by the audit aspect for organization create (only name).
     * entityId can be null for CREATE before the aspect resolves it from result.
     */
    private static AuditEventPayload organizationCreatePayload(String organizationName) {
        return AuditEventPayload.builder()
                .schemaName("cpc")
                .tableName("organizations")
                .entityType("ORGANIZATION")
                .entityId(null)
                .action("CREATE")
                .oldSnapshot(null)
                .newSnapshot("{\"message\":\"Success\"}")
                .userId(null)
                .organizationId(null)
                .userEmail("admin@test.com")
                .userRole("ROLE_VIMA_ADMIN")
                .correlationId("test-correlation-123")
                .ipAddress("127.0.0.1")
                .actionSource(ActionSource.WEB)
                .timestamp(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Organization create – audit write fails, retry 3 times, then audit_pending")
    class OrganizationCreateAuditFailure {

        @Test
        @DisplayName("when writer throws, retries 3 times then saves to audit_pending")
        void writeAsync_organizationCreate_whenWriterThrows_retriesThreeTimesThenSavesToPending() {
            AuditEventPayload payload = organizationCreatePayload("Test Org Only Name");
            RuntimeException failure = new RuntimeException("DB unavailable");

            doThrow(failure).when(auditEventWriter).save(any(AuditEventPayload.class));

            auditService.writeAsync(payload);

            verify(auditEventWriter, times(3)).save(any(AuditEventPayload.class));

            ArgumentCaptor<AuditPending> pendingCaptor = ArgumentCaptor.forClass(AuditPending.class);
            verify(auditPendingRepository).save(pendingCaptor.capture());

            AuditPending saved = pendingCaptor.getValue();
            assertEquals("PENDING", saved.getStatus());
            assertNotNull(saved.getErrorMessage());
            assertTrue(saved.getErrorMessage().contains("DB unavailable"));
            assertEquals(0, saved.getRetryCount());
            assertNotNull(saved.getPayload());
            assertTrue(saved.getPayload().contains("ORGANIZATION"));
            assertTrue(saved.getPayload().contains("CREATE"));
        }

        @Test
        @DisplayName("pending payload JSON contains schema, table, entityType, action, correlationId")
        void writeAsync_organizationCreate_pendingPayloadContainsExpectedFields() {
            AuditEventPayload payload = organizationCreatePayload("Minimal Org");
            doThrow(new RuntimeException("Connection refused"))
                    .when(auditEventWriter).save(any(AuditEventPayload.class));

            auditService.writeAsync(payload);

            ArgumentCaptor<AuditPending> captor = ArgumentCaptor.forClass(AuditPending.class);
            verify(auditPendingRepository).save(captor.capture());

            String payloadJson = captor.getValue().getPayload();
            assertTrue(payloadJson.contains("cpc") && payloadJson.contains("schemaName"));
            assertTrue(payloadJson.contains("organizations"));
            assertTrue(payloadJson.contains("ORGANIZATION"));
            assertTrue(payloadJson.contains("CREATE"));
            assertTrue(payloadJson.contains("test-correlation-123"));
        }
    }

    @Nested
    @DisplayName("General audit failure → audit_pending")
    class GeneralAuditFailure {

        @Test
        @DisplayName("when writer always throws, saveToPending is called once after 3 attempts")
        void writeAsync_whenWriterAlwaysThrows_saveToPendingCalledOnce() {
            AuditEventPayload payload = AuditEventPayload.builder()
                    .schemaName("cpc")
                    .tableName("endorsements")
                    .entityType("ENDORSEMENT")
                    .entityId(UUID.randomUUID().toString())
                    .action("UPDATE")
                    .oldSnapshot("{}")
                    .newSnapshot("{}")
                    .userId(null)
                    .organizationId(UUID.randomUUID())
                    .userEmail("user@test.com")
                    .userRole("ROLE_HR_ADMIN")
                    .correlationId("corr-456")
                    .ipAddress(null)
                    .actionSource(ActionSource.WEB)
                    .timestamp(Instant.now())
                    .build();

            doThrow(new RuntimeException("Constraint violation"))
                    .when(auditEventWriter).save(any(AuditEventPayload.class));

            auditService.writeAsync(payload);

            verify(auditEventWriter, times(3)).save(any(AuditEventPayload.class));
            verify(auditPendingRepository, times(1)).save(any(AuditPending.class));
        }

        @Test
        @DisplayName("when payload is null, writer and pending are never called")
        void writeAsync_whenPayloadNull_doesNothing() {
            auditService.writeAsync(null);

            verify(auditEventWriter, never()).save(any(AuditEventPayload.class));
            verify(auditPendingRepository, never()).save(any(AuditPending.class));
        }

        @Test
        @DisplayName("when writer succeeds on first attempt, saveToPending is not called")
        void writeAsync_whenWriterSucceeds_firstAttempt_noSaveToPending() {
            AuditEventPayload payload = organizationCreatePayload("Success Org");
            doNothing().when(auditEventWriter).save(any(AuditEventPayload.class));

            auditService.writeAsync(payload);

            verify(auditEventWriter, times(1)).save(any(AuditEventPayload.class));
            verify(auditPendingRepository, never()).save(any(AuditPending.class));
        }

        @Test
        @DisplayName("when writer succeeds on second attempt after one failure, no saveToPending")
        void writeAsync_whenWriterSucceedsOnSecondAttempt_noSaveToPending() {
            AuditEventPayload payload = organizationCreatePayload("Retry Org");
            doThrow(new RuntimeException("Temporary failure"))
                    .doNothing()
                    .when(auditEventWriter).save(any(AuditEventPayload.class));

            auditService.writeAsync(payload);

            verify(auditEventWriter, times(2)).save(any(AuditEventPayload.class));
            verify(auditPendingRepository, never()).save(any(AuditPending.class));
        }
    }
}
