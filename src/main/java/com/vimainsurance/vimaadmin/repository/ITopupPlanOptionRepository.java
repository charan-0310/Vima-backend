package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.TopupPlanOption;

@Repository
public interface ITopupPlanOptionRepository extends JpaRepository<TopupPlanOption, UUID> {

    List<TopupPlanOption> findByCompanyIdAndIsActiveTrue(UUID companyId);

    List<TopupPlanOption> findByCompanyIdAndPlanType(UUID companyId, String planType);

    List<TopupPlanOption> findByCompanyId(UUID companyId);

    @Modifying
    @Query("UPDATE TopupPlanOption t SET t.isDeleted = true, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :id")
    int softDeleteById(@Param("id") UUID id);

    @Query("SELECT DISTINCT t.policyId FROM TopupPlanOption t WHERE t.policyId IS NOT NULL")
    List<Long> findDistinctTopupPolicyIds();
}
