package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.ClaimSettlement;

@Repository
public interface IClaimSettlementRepository extends JpaRepository<ClaimSettlement, UUID> {

    Optional<ClaimSettlement> findByClaim_Id(UUID claimId);
}
