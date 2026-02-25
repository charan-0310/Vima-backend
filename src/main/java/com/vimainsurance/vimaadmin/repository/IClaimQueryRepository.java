package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.ClaimQuery;
import com.vimainsurance.vimaadmin.enums.QueryStatus;

@Repository
public interface IClaimQueryRepository extends JpaRepository<ClaimQuery, UUID> {

    List<ClaimQuery> findByClaim_Id(UUID claimId);

    List<ClaimQuery> findByClaim_IdOrderByCreatedAtAsc(UUID claimId);

    List<ClaimQuery> findByClaim_IdAndQueryStatus(UUID claimId, QueryStatus queryStatus);

    @Query("SELECT COUNT(q) FROM ClaimQuery q WHERE q.claim.id = :claimId AND q.queryStatus = com.vimainsurance.vimaadmin.enums.QueryStatus.OPEN")
    long countOpenQueriesByClaimId(@Param("claimId") UUID claimId);
}
