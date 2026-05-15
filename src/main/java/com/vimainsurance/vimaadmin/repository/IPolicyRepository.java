package com.vimainsurance.vimaadmin.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;

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
     * Check if any policy is mapped to the given document
     */
    boolean existsByDocument_DocumentId(UUID documentId);

    /**
     * Find policy by document ID
     */
    Optional<Policy> findByDocument_DocumentId(UUID documentId);

    /**
     * Count policies by organization ID
     */
    long countByOrganizationId(UUID organizationId);

    /**
     * Sum of premium amount for policies by organization ID
     */
    @Query("SELECT COALESCE(SUM(p.premiumAmount), 0) FROM Policy p WHERE p.organizationId = :organizationId")
    BigDecimal sumPremiumAmountByOrganizationId(@Param("organizationId") UUID organizationId);
    
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

    /**
     * Count active retail (non-organization) policies
     */
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.status = :status AND p.organizationId IS NULL")
    long countByStatusAndOrganizationIdIsNull(@Param("status") PolicyStatus status);

    /**
     * Sum of sumInsured for active retail (non-organization) policies
     */
    @Query("SELECT COALESCE(SUM(p.sumInsured), 0) FROM Policy p WHERE p.status = :status AND p.organizationId IS NULL")
    BigDecimal sumSumInsuredByStatusAndOrganizationIdIsNull(@Param("status") PolicyStatus status);

    /**
     * Sum of premiumAmount for active retail (non-organization) policies
     */
    @Query("SELECT COALESCE(SUM(p.premiumAmount), 0) FROM Policy p WHERE p.status = :status AND p.organizationId IS NULL")
    BigDecimal sumPremiumAmountByStatusAndOrganizationIdIsNull(@Param("status") PolicyStatus status);

    /**
     * Retail dashboard scope: policies whose primaryIndividualId is a retail primary customer (customers.organization_id IS NULL).
     * This is more reliable than Policy.organizationId IS NULL because retail policies may still carry an organizationId.
     */
    @Query("""
            SELECT COUNT(p)
            FROM Policy p
            WHERE p.status = :status
              AND p.primaryIndividualId IN (
                SELECT d.individualId FROM Deals d
                WHERE d.isPrimaryMember = true AND d.organization IS NULL
              )
            """)
    long countRetailPoliciesByStatus(@Param("status") PolicyStatus status);

    @Query("""
            SELECT COALESCE(SUM(p.sumInsured), 0)
            FROM Policy p
            WHERE p.status = :status
              AND p.primaryIndividualId IN (
                SELECT d.individualId FROM Deals d
                WHERE d.isPrimaryMember = true AND d.organization IS NULL
              )
            """)
    BigDecimal sumRetailSumInsuredByStatus(@Param("status") PolicyStatus status);

    @Query("""
            SELECT COALESCE(SUM(p.premiumAmount), 0)
            FROM Policy p
            WHERE p.status = :status
              AND p.primaryIndividualId IN (
                SELECT d.individualId FROM Deals d
                WHERE d.isPrimaryMember = true AND d.organization IS NULL
              )
            """)
    BigDecimal sumRetailPremiumAmountByStatus(@Param("status") PolicyStatus status);

    /**
     * Find policy IDs whose product type is TOP_UP or SUPER_TOP_UP (for enrollment top-up mapping cancellation).
     */
    @Query("SELECT p.policyId FROM Policy p WHERE p.productType IN :productTypes")
    List<Long> findPolicyIdsByProductTypeIn(@Param("productTypes") List<ProductType> productTypes);

    /**
     * Find policies by organization and product type (e.g. PARENT_GMC for enrollment plans).
     */
    List<Policy> findByOrganizationIdAndProductType(UUID organizationId, ProductType productType);

    /**
     * Find active policies by organization and product type.
     */
    List<Policy> findByOrganizationIdAndProductTypeAndStatus(UUID organizationId, ProductType productType, PolicyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Policy p WHERE p.policyId = :policyId")
    Optional<Policy> findByIdForUpdate(@Param("policyId") Long policyId);

    /**
     * Minimal columns for endorsement list/detail when we must avoid lazy-loading the full Policy entity.
     * Includes linked CD account balance (when {@code cd_account_id} is set).
     * Insurer display: policy denormalized name, else master provider name, else CD account insurer label.
     */
    @Query(value = """
            SELECT p.policy_id,
                   CAST(p.product_type AS TEXT),
                   p.policy_number,
                   COALESCE(
                       NULLIF(TRIM(p.insurer_name), ''),
                       ip.provider_name,
                       ca.insurer_name
                   ) AS insurer_name,
                   ca.cd_balance,
                   p.description
            FROM cpc.policies p
            LEFT JOIN cpc.cd_accounts ca ON ca.cd_account_id = p.cd_account_id
            LEFT JOIN admin.insurance_providers ip ON ip.provider_id = p.insurance_provider_id
            WHERE p.policy_id IN (:ids)
            """, nativeQuery = true)
    List<Object[]> findListingColumnsByPolicyIds(@Param("ids") Collection<Long> ids);

    // --- Manager portfolio dashboard (group / org-scoped policies) ---

    @Query("""
        SELECT COUNT(p) FROM Policy p
        WHERE p.organizationId IS NOT NULL
          AND (p.isDeleted = false OR p.isDeleted IS NULL)
          AND p.status = :active
        """)
    long countActivePoliciesForOrganizations(@Param("active") PolicyStatus active);

    @Query("""
        SELECT COALESCE(SUM(p.premiumAmount), 0) FROM Policy p
        WHERE p.organizationId IS NOT NULL
          AND (p.isDeleted = false OR p.isDeleted IS NULL)
          AND p.status = :active
        """)
    java.math.BigDecimal sumInceptionPremiumForActiveOrganizationPolicies(@Param("active") PolicyStatus active);

    @Query("""
        SELECT COUNT(p) FROM Policy p
        WHERE p.organizationId IS NOT NULL
          AND (p.isDeleted = false OR p.isDeleted IS NULL)
          AND p.status = :active
          AND p.endDate IS NOT NULL
          AND p.endDate > :today
          AND p.endDate <= :within
        """)
    long countActiveOrgPoliciesExpiringBetween(@Param("active") PolicyStatus active,
            @Param("today") LocalDate today,
            @Param("within") LocalDate within);

    @Query("""
        SELECT COUNT(p) FROM Policy p
        WHERE p.organizationId IS NOT NULL
          AND (p.isDeleted = false OR p.isDeleted IS NULL)
          AND p.status = :active
          AND (p.endDate IS NULL OR p.endDate > :after)
        """)
    long countActiveOrgPoliciesWithEndAfter(@Param("active") PolicyStatus active, @Param("after") LocalDate after);

    @Query("""
        SELECT COUNT(p) FROM Policy p
        WHERE p.organizationId IS NOT NULL
          AND (p.isDeleted = false OR p.isDeleted IS NULL)
          AND p.status = :active
          AND p.endDate IS NOT NULL
          AND p.endDate >= :fromInclusive
          AND p.endDate <= :toInclusive
        """)
    long countActiveOrgPoliciesEndDateBetween(@Param("active") PolicyStatus active,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive);

    @Query("""
        SELECT COUNT(p) FROM Policy p
        WHERE p.organizationId IS NOT NULL
          AND (p.isDeleted = false OR p.isDeleted IS NULL)
          AND (
            p.status IN ('LAPSED', 'EXPIRED', 'CANCELLED')
            OR (p.endDate IS NOT NULL AND p.endDate < :today)
          )
        """)
    long countLapsedOrExpiredOrganizationPolicies(@Param("today") LocalDate today);

    @Query(value = """
        SELECT p.policy_id::text,
               o.organization_name,
               o.organization_displayname,
               o.organization_id::text,
               p.policy_number,
               CAST(p.product_type AS TEXT),
               COALESCE(p.sum_insured, 0),
               p.end_date
        FROM cpc.policies p
        INNER JOIN cpc.organizations o ON o.organization_id = p.organization_id
        WHERE p.organization_id IS NOT NULL
          AND (p.is_deleted IS NULL OR p.is_deleted = false)
          AND p.status::text = 'ACTIVE'
          AND p.end_date IS NOT NULL
          AND p.end_date > :today
          AND p.end_date <= :until
        ORDER BY p.end_date ASC
        """, nativeQuery = true)
    List<Object[]> findUpcomingRenewalsNative(@Param("today") LocalDate today, @Param("until") LocalDate until);
}
