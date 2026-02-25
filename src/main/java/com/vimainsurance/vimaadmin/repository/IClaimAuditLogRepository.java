package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.ClaimAuditLog;

@Repository
public interface IClaimAuditLogRepository extends JpaRepository<ClaimAuditLog, UUID> {

    List<ClaimAuditLog> findByClaim_IdOrderByCreatedAtDesc(UUID claimId);
}
