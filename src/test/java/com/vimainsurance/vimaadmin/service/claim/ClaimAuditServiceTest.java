package com.vimainsurance.vimaadmin.service.claim;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimAuditLog;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.repository.IClaimAuditLogRepository;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;

@ExtendWith(MockitoExtension.class)
class ClaimAuditServiceTest {

    @Mock
    private IClaimRepository claimRepository;

    @Mock
    private IClaimAuditLogRepository auditLogRepository;

    @Mock
    private EntityManager entityManager;

    private ClaimAuditService auditService;

    private Claim claim;
    private UUID claimId;

    @BeforeEach
    void setUp() {
        auditService = new ClaimAuditService(claimRepository, auditLogRepository, entityManager);
        claimId = UUID.randomUUID();
        claim = new Claim();
        claim.setId(claimId);
        claim.setClaimNumber("VIMA-CLM-2025-0001");
    }

    @Test
    void logAction_claimExists_savesAuditLog() {
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(auditLogRepository.save(any(ClaimAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.logAction(claimId, "STATUS_UPDATE", ClaimStatus.DRAFT.getValue(), ClaimStatus.PENDING_REVIEW.getValue(),
                null, "ADMIN", "Approved", null, null, null);

        verify(claimRepository).findById(claimId);
        verify(auditLogRepository).save(any(ClaimAuditLog.class));
    }

    @Test
    void logAction_claimNotFound_throws() {
        when(claimRepository.findById(claimId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> auditService.logAction(claimId, "STATUS_UPDATE", null, null, null, "ADMIN", "Test", null, null, null));
    }

    @Test
    void logAction_withCorrelationId_savesWithOptionalFields() {
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(auditLogRepository.save(any(ClaimAuditLog.class))).thenAnswer(i -> i.getArgument(0));

        auditService.logAction(claimId, "API_CALL", "PENDING_REVIEW", "SUBMITTED_TO_INSURER",
                UUID.randomUUID(), "SYSTEM", "API submission", "corr-123", "/api/claim/submit", 200);

        verify(auditLogRepository).save(any(ClaimAuditLog.class));
    }
}
