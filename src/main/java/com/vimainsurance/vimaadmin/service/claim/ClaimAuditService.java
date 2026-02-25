package com.vimainsurance.vimaadmin.service.claim;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimAuditLog;
import com.vimainsurance.vimaadmin.repository.IClaimAuditLogRepository;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimAuditService {

    private final IClaimRepository claimRepository;
    private final IClaimAuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public void logAction(UUID claimId, String action, String oldStatus, String newStatus,
            UUID actorId, String actorRole, String details) {
        logAction(claimId, action, oldStatus, newStatus, actorId, actorRole, details, null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void logAction(UUID claimId, String action, String oldStatus, String newStatus,
            UUID actorId, String actorRole, String details, String correlationId, String apiEndpoint, Integer apiResponseStatus) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        ClaimAuditLog logEntry = new ClaimAuditLog();
        logEntry.setClaim(claim);
        logEntry.setAction(action);
        logEntry.setOldStatus(oldStatus);
        logEntry.setNewStatus(newStatus);
        logEntry.setActorRole(actorRole);
        logEntry.setDetails(ClaimAuditDetailsJson.toJsonMessage(details));
        logEntry.setCorrelationId(correlationId);
        logEntry.setApiEndpoint(apiEndpoint);
        logEntry.setApiResponseStatus(apiResponseStatus);
        // actor_id is FK to admin_user; only set when actor is an AdminUser (not EMPLOYEE/SYSTEM)
        if (actorId != null && !"EMPLOYEE".equals(actorRole) && !"SYSTEM".equals(actorRole)) {
            logEntry.setActor(entityManager.getReference(AdminUser.class, actorId));
        }
        auditLogRepository.save(logEntry);
        log.debug("[claimNumber={}] Audit: action={}, oldStatus={}, newStatus={}", claim.getClaimNumber(), action, oldStatus, newStatus);
    }
}
