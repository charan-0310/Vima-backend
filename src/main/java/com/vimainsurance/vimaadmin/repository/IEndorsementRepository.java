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
import com.vimainsurance.vimaadmin.enums.EndorsementType;

@Repository
public interface IEndorsementRepository extends JpaRepository<Endorsement, UUID>, JpaSpecificationExecutor<Endorsement> {
    
    Optional<Endorsement> findByEndorsementId(UUID endorsementId);
    
    List<Endorsement> findByOrganization_OrganizationId(UUID organizationId);
    
    Page<Endorsement> findByOrganization_OrganizationId(UUID organizationId, Pageable pageable);
    
    List<Endorsement> findByStatus(AccountStatus status);
    
    Page<Endorsement> findByStatus(AccountStatus status, Pageable pageable);
    
    List<Endorsement> findByEndorsementType(EndorsementType endorsementType);
    
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
          AND e.created_at >= COALESCE(CAST(:startDate AS TIMESTAMP), '1970-01-01'::TIMESTAMP)
          AND e.created_at <= COALESCE(CAST(:endDate AS TIMESTAMP), '9999-12-31 23:59:59'::TIMESTAMP)
        GROUP BY 
            EXTRACT(YEAR FROM e.created_at),
            EXTRACT(MONTH FROM e.created_at)
        ORDER BY 
            year ASC,
            month ASC
        """)
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
          AND e.updated_at >= COALESCE(CAST(:startDate AS TIMESTAMP), '1970-01-01'::TIMESTAMP)
          AND e.updated_at <= COALESCE(CAST(:endDate AS TIMESTAMP), '9999-12-31 23:59:59'::TIMESTAMP)
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
      AND (:status IS NULL OR e.status = :status)
      AND (:startDate IS NULL OR e.approvedAt >= :startDate)
      AND (:endDate IS NULL OR e.approvedAt <= :endDate)
    """)
    List<Endorsement> findByOrganizationAndDateRange(
            @Param("organizationId") UUID organizationId,
            @Param("status") AccountStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Sum total employees and dependents from endorsement additions (created)
     * Note: startDate and endDate can be null - if null, no date filtering is applied
     */
    @Query(value = """
        SELECT COALESCE(SUM(e.total_employees + e.total_dependents), 0)
        FROM cpc.endorsements e
        WHERE e.organization_id IN :organizationIds
          AND e.created_at >= COALESCE(CAST(:startDate AS TIMESTAMP), '1970-01-01'::TIMESTAMP)
          AND e.created_at <= COALESCE(CAST(:endDate AS TIMESTAMP), '9999-12-31 23:59:59'::TIMESTAMP)
          AND e.status = 'COMPLETED'
        """, nativeQuery = true)
    Long sumTotalEmployeesAndDependentsForAdditions(
        @Param("organizationIds") List<UUID> organizationIds,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Sum total employees and dependents from endorsement deletions
     * Note: startDate and endDate can be null - if null, no date filtering is applied
     */
    @Query(value = """
        SELECT COALESCE(SUM(e.total_employees + e.total_dependents), 0)
        FROM cpc.endorsements e
        WHERE e.organization_id IN :organizationIds
          AND e.endorsement_type::text = CAST(:endorsementType AS VARCHAR)
          AND e.updated_at >= COALESCE(CAST(:startDate AS TIMESTAMP), '1970-01-01'::TIMESTAMP)
          AND e.updated_at <= COALESCE(CAST(:endDate AS TIMESTAMP), '9999-12-31 23:59:59'::TIMESTAMP)
          AND e.status = 'COMPLETED'
        """, nativeQuery = true)
    Long sumTotalEmployeesAndDependentsForDeletions(
        @Param("organizationIds") List<UUID> organizationIds,
        @Param("endorsementType") EndorsementType endorsementType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT e FROM Endorsement e
    WHERE e.organization.organizationId = :organizationId
      AND (:startDate IS NULL OR e.approvedAt >= :startDate)
      AND (:endDate IS NULL OR e.approvedAt <= :endDate)
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
             and (:status is null or e.status = :status)
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
             and (:status is null or e.status = :status)
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
             and (:status is null or e.status = :status)
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
            and e.createdAt <= :toDate
           order by e.createdAt desc
           """)
    List<Endorsement> findByOrganizationAndToDate(@Param("organizationId") UUID organizationId,
                                                  @Param("toDate") LocalDateTime toDate);


}

