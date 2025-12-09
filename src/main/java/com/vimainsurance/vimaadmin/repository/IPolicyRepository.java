package com.vimainsurance.vimaadmin.repository;

import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Policy entity
 */
@Repository
public interface IPolicyRepository extends JpaRepository<Policy, Long> {

    /**
     * Find policy by policy number
     */
    Optional<Policy> findByPolicyNumber(String policyNumber);

    /**
     * Find policies by primary individual ID
     */
    List<Policy> findByPrimaryIndividualId(UUID primaryIndividualId);
    
    /**
     * Find policies by multiple primary individual IDs (batch query for optimization)
     */
    List<Policy> findByPrimaryIndividualIdIn(List<UUID> primaryIndividualIds);

    /**
     * Find policies by insurance provider ID
     */
    List<Policy> findByInsuranceProviderId(UUID insuranceProviderId);

    /**
     * Find policies by organization ID
     */
    List<Policy> findByOrganizationId(UUID organizationId);

    /**
     * Find policies by status
     */
    List<Policy> findByStatus(PolicyStatus status);

    /**
     * Find policies by lead ID
     */
    List<Policy> findByLeadId(UUID leadId);

    /**
     * Find active policies
     */
    @Query("SELECT p FROM Policy p WHERE p.status = 'ACTIVE'")
    List<Policy> findActivePolicies();

    /**
     * Find policies expiring within specified days
     */
    @Query("SELECT p FROM Policy p WHERE p.endDate <= :expiryDate AND p.status = 'ACTIVE'")
    List<Policy> findPoliciesExpiringBy(@Param("expiryDate") LocalDate expiryDate);

    /**
     * Find policies by primary individual ID with pagination
     */
    Page<Policy> findByPrimaryIndividualId(UUID primaryIndividualId, Pageable pageable);

    /**
     * Find policies by status with pagination
     */
    Page<Policy> findByStatus(PolicyStatus status, Pageable pageable);

    /**
     * Check if policy number exists
     */
    boolean existsByPolicyNumber(String policyNumber);

    /**
     * Count policies by status
     */
    long countByStatus(PolicyStatus status);

    /**
     * Find policies by date range
     */
    @Query("SELECT p FROM Policy p WHERE p.startDate >= :startDate AND p.endDate <= :endDate")
    List<Policy> findPoliciesByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find policies by insurance provider and status
     */
    List<Policy> findByInsuranceProviderIdAndStatus(UUID insuranceProviderId, PolicyStatus status);

    /**
     * Find policies by organization and status
     */
    List<Policy> findByOrganizationIdAndStatus(UUID organizationId, PolicyStatus status);
    
    /**
     * Sum of sumInsured for active policies (aggregation query to avoid loading all policies)
     */
    @Query("SELECT COALESCE(SUM(p.sumInsured), 0) FROM Policy p WHERE p.status = :status")
    BigDecimal sumSumInsuredByStatus(@Param("status") PolicyStatus status);
    
    /**
     * Sum of premiumAmount for active policies (aggregation query to avoid loading all policies)
     */
    @Query("SELECT COALESCE(SUM(p.premiumAmount), 0) FROM Policy p WHERE p.status = :status")
    BigDecimal sumPremiumAmountByStatus(@Param("status") PolicyStatus status);
}
