package com.vimainsurance.vimaadmin.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.PremiumRateTable;
import com.vimainsurance.vimaadmin.enums.PricingModel;

@Repository
public interface IPremiumRateTableRepository
        extends JpaRepository<PremiumRateTable, UUID>, JpaSpecificationExecutor<PremiumRateTable> {

    @Query("""
        SELECT p FROM PremiumRateTable p
        WHERE p.organizationId = :companyId
        AND p.productType = :planType
        AND p.pricingModel = :pricingModel
        AND p.effectiveFrom <= :today
        AND (p.effectiveTo IS NULL OR p.effectiveTo >= :today)
        """)
    List<PremiumRateTable> findByCompanyIdAndPlanTypeAndPricingModel(
            @Param("companyId") UUID companyId,
            @Param("planType") String planType,
            @Param("pricingModel") PricingModel pricingModel,
            @Param("today") LocalDate today);

    @Query("""
        SELECT p FROM PremiumRateTable p
        WHERE p.organizationId = :companyId
        AND p.productType = :planType
        AND (p.memberType IS NULL OR p.memberType = :memberType)
        AND p.ageBandMin <= :age
        AND (p.ageBandMax IS NULL OR p.ageBandMax > :age)
        AND p.effectiveFrom <= :today
        AND (p.effectiveTo IS NULL OR p.effectiveTo >= :today)
        ORDER BY p.ageBandMin DESC
        """)
    List<PremiumRateTable> findByCompanyIdAndPlanTypeAndMemberTypeAndAgeInBand(
            @Param("companyId") UUID companyId,
            @Param("planType") String planType,
            @Param("memberType") String memberType,
            @Param("age") int age,
            @Param("today") LocalDate today);

    @Query("""
        SELECT p FROM PremiumRateTable p
        WHERE p.organizationId = :companyId
        AND p.productType = :planType
        AND p.familySizeMin <= :familySize
        AND (p.familySizeMax IS NULL OR p.familySizeMax >= :familySize)
        AND p.effectiveFrom <= :today
        AND (p.effectiveTo IS NULL OR p.effectiveTo >= :today)
        ORDER BY p.familySizeMin DESC
        """)
    List<PremiumRateTable> findByCompanyIdAndPlanTypeAndFamilySizeRange(
            @Param("companyId") UUID companyId,
            @Param("planType") String planType,
            @Param("familySize") int familySize,
            @Param("today") LocalDate today);

    List<PremiumRateTable> findByOrganizationId(UUID organizationId);

    @Modifying
    @Query("UPDATE PremiumRateTable p SET p.isDeleted = true, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    int softDeleteById(@Param("id") UUID id);
}
