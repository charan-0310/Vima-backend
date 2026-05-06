package com.vimainsurance.vimaadmin.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.EndorsementSource;
import com.vimainsurance.vimaadmin.enums.EndorsementType;

@Repository
public interface IEndorsementRepository extends JpaRepository<Endorsement, UUID>, JpaSpecificationExecutor<Endorsement> {

    /**
     * Do not override {@code findAll(Specification, Pageable)} with {@code @EntityGraph}: pagination + multi-table
     * fetch joins breaks Hibernate/PostgreSQL ({@code InvalidDataAccessResourceUsageException}). Use default
     * spec paging; {@link com.vimainsurance.vimaadmin.service.serviceimpl.EndorsementServiceImpl#getAllWithFilters}
     * runs in {@code @Transactional(readOnly = true)} so lazy associations resolve during mapping.
     */

    Optional<Endorsement> findByEndorsementId(UUID endorsementId);
    
    List<Endorsement> findByOrganization_OrganizationId(UUID organizationId);
    
    Page<Endorsement> findByOrganization_OrganizationId(UUID organizationId, Pageable pageable);
    
    List<Endorsement> findByStatus(AccountStatus status);
    
    Page<Endorsement> findByStatus(AccountStatus status, Pageable pageable);
    
    List<Endorsement> findByEndorsementType(EndorsementType endorsementType);

    List<Endorsement> findBySplitGroupId(UUID splitGroupId);
    
    Page<Endorsement> findByEndorsementType(EndorsementType endorsementType, Pageable pageable);

    List<Endorsement> findByEndorsementIdIn(List<UUID> endorsementIds);

    @Query("""
        SELECT COUNT(e)
        FROM Endorsement e
        WHERE e.status IN (
            'PENDING_APPROVAL',
            'PENDING_DELETE',
            'PENDING_EXIT'
        )
        """)
    Long getPendingCount();

    /**
     * Get monthly endorsement additions (created) grouped by month
     * Returns: [year, month, count] where year and month are integers, count is Long
     * Note: startDate and endDate can be null - if null, no date filtering is applied
     */
    @Query(value = """
        SELECT 
            EXTRACT(YEAR FROM e.created_at) AS year,
            EXTRACT(MONTH FROM e.created_at) AS month,
            COUNT(e.endorsement_id) AS count
        FROM cpc.endorsements e
        WHERE e.organization_id IN :organizationIds
          AND e.created_at >= :startDate
          AND e.created_at <= :endDate
        GROUP BY 
            EXTRACT(YEAR FROM e.created_at),
            EXTRACT(MONTH FROM e.created_at)
        ORDER BY 
            year ASC,
            month ASC
        """, nativeQuery = true)
    List<Object[]> getMonthlyEndorsementAdditions(
        @Param("organizationIds") List<UUID> organizationIds,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get monthly endorsement deletions (status INACTIVE) grouped by month
     * Returns: [year, month, count] where year and month are integers, count is Long
     * Note: startDate and endDate should not be null (use default dates in service layer)
     */
    @Query(value = """
        SELECT 
            EXTRACT(YEAR FROM e.updated_at) AS year,
            EXTRACT(MONTH FROM e.updated_at) AS month,
            COUNT(e.endorsement_id) AS count
        FROM cpc.endorsements e
        WHERE e.organization_id IN :organizationIds
          AND e.endorsement_type::text = CAST(:endorsementType AS VARCHAR)
          AND e.updated_at >= :startDate
          AND e.updated_at <= :endDate
        GROUP BY 
            EXTRACT(YEAR FROM e.updated_at),
            EXTRACT(MONTH FROM e.updated_at)
        ORDER BY 
            year ASC,
            month ASC
        """, nativeQuery = true)
    List<Object[]> getMonthlyEndorsementDeletions(
        @Param("organizationIds") List<UUID> organizationIds,
        @Param("endorsementType") EndorsementType endorsementType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find endorsements by organization, optionally filtering by status and approvedAt date range.
     * If `status`, `startDate`, or `endDate` are null, those filters are ignored.
     */
    @Query("""
    SELECT e FROM Endorsement e
    WHERE e.organization.organizationId = :organizationId
    AND e.status = COALESCE(:status, e.status)
    AND e.approvedAt >= COALESCE(:startDate, e.approvedAt)
    AND e.approvedAt <= COALESCE(:endDate, e.approvedAt)
    """)
    List<Endorsement> findByOrganizationAndDateRange(
            @Param("organizationId") UUID organizationId,
            @Param("status") AccountStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );



    @Query("""
    SELECT e FROM Endorsement e
    WHERE e.organization.organizationId = :organizationId
    AND e.approvedAt >= COALESCE(:startDate, e.approvedAt)
    AND e.approvedAt <= COALESCE(:endDate, e.approvedAt)
    """)
    List<Endorsement> findByOrganizationAndDateRange(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
       select e
       from Endorsement e
       where e.organization.organizationId = :organizationId
         and e.status = :status
       order by e.createdAt desc
       """)
    List<Endorsement> findByOrganization(@Param("organizationId") UUID organizationId,
                                         @Param("status") AccountStatus status);

    @Query("""
       select e
       from Endorsement e
       where e.organization.organizationId = :organizationId
       order by e.createdAt desc
       """)
    List<Endorsement> findByOrganization(@Param("organizationId") UUID organizationId);


    @Query("""
       select e
       from Endorsement e
       where e.organization.organizationId = :organizationId
         and e.status = COALESCE(:status, e.status)
         and e.createdAt >= :fromDate
       order by e.createdAt desc
       """)
    List<Endorsement> findByOrganizationAndFromDate(@Param("organizationId") UUID organizationId,
                                                    @Param("status") AccountStatus status,
                                                    @Param("fromDate") LocalDateTime fromDate);

    @Query("""
           select e
           from Endorsement e
           where e.organization.organizationId = :organizationId
             and e.createdAt >= :fromDate
           order by e.createdAt desc
           """)
    List<Endorsement> findByOrganizationAndFromDate(@Param("organizationId") UUID organizationId,
                                                    @Param("fromDate") LocalDateTime fromDate);

    @Query("""
       select e
       from Endorsement e
       where e.organization.organizationId = :organizationId
         and e.status = COALESCE(:status, e.status)
         and e.createdAt <= :toDate
       order by e.createdAt desc
       """)
    List<Endorsement> findByOrganizationAndToDate(@Param("organizationId") UUID organizationId,
                                                  @Param("status") AccountStatus status,
                                                  @Param("toDate") LocalDateTime toDate);

    @Query("""
       select e
       from Endorsement e
       where e.organization.organizationId = :organizationId
         and e.createdAt <= COALESCE(:toDate, e.createdAt)
       order by e.createdAt desc
       """)
    List<Endorsement> findByOrganizationAndToDate(@Param("organizationId") UUID organizationId,
                                                  @Param("toDate") LocalDateTime toDate);

    /**
     * Find the most recent self-enrollment endorsement for the given organization and enrollment window.
     * Used to attach approved submissions to the same endorsement for the whole window.
     */
    Optional<Endorsement> findFirstByOrganization_OrganizationIdAndEnrollmentWindow_IdAndSourceOrderByCreatedAtDesc(
            UUID organizationId, UUID enrollmentWindowId, EndorsementSource source);

    /**
     * Per-policy endorsement aggregates for company policy cards.
     * {@code endorsement_count} excludes pending statuses (same four as {@code pending_endorsement_count}),
     * so the UI shows processed/completed endorsements only.
     */
    @Query(value = """
       select
         e.policy_id,
         coalesce(sum(e.premium_amount), 0) as endorsement_premium,
         sum(
           case
             when e.status::text in ('PENDING_APPROVAL', 'PENDING_DELETE', 'PENDING_EXIT', 'PENDING')
             then 0
             else 1
           end
         ) as endorsement_count,
         sum(
           case
             when e.status::text in ('PENDING_APPROVAL', 'PENDING_DELETE', 'PENDING_EXIT', 'PENDING')
             then 1
             else 0
           end
         ) as pending_endorsement_count,
         max(e.updated_at) as last_endorsement_updated_at
       from cpc.endorsements e
       where e.organization_id = :organizationId
         and e.policy_id is not null
         and e.policy_id in (:policyIds)
       group by e.policy_id
       """, nativeQuery = true)
    List<Object[]> getPolicyPremiumSummaryByOrganizationAndPolicyIds(@Param("organizationId") UUID organizationId,
                                                                     @Param("policyIds") List<Long> policyIds);

}

