package com.vimainsurance.vimaadmin.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.CostSharingRule;
import com.vimainsurance.vimaadmin.enums.CoverageCategory;

@Repository
public interface ICostSharingRuleRepository extends JpaRepository<CostSharingRule, UUID> {

    List<CostSharingRule> findByCompanyIdAndPlanType(UUID companyId, String planType);

    List<CostSharingRule> findByCompanyIdAndPlanTypeAndCoverageCategory(
            UUID companyId, String planType, CoverageCategory coverageCategory);

    @Query("""
        SELECT c FROM CostSharingRule c
        WHERE c.companyId = :companyId
        AND c.effectiveFrom <= :effectiveDate
        AND (c.effectiveTo IS NULL OR c.effectiveTo >= :effectiveDate)
        AND c.isDeleted = false
        """)
    List<CostSharingRule> findActiveRulesForCompany(
            @Param("companyId") UUID companyId,
            @Param("effectiveDate") LocalDate effectiveDate);

    List<CostSharingRule> findByCompanyId(UUID companyId);

    @Modifying
    @Query("UPDATE CostSharingRule c SET c.isDeleted = true, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    int softDeleteById(@Param("id") UUID id);
}
