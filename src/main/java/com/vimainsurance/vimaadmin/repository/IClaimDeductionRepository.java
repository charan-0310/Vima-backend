package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.ClaimDeduction;

@Repository
public interface IClaimDeductionRepository extends JpaRepository<ClaimDeduction, UUID> {

    List<ClaimDeduction> findByClaim_Id(UUID claimId);
}
