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
     * Note: startDate and endDate should not be null (use default dates in service layer)
     */
    @Query("""
        SELECT 
            EXTRACT(YEAR FROM e.createdAt) AS year,
            EXTRACT(MONTH FROM e.createdAt) AS month,
            COUNT(e) AS count
        FROM Endorsement e
        WHERE e.organization.organizationId IN :organizationIds
          AND e.createdAt >= :startDate
          AND e.createdAt <= :endDate
        GROUP BY 
            EXTRACT(YEAR FROM e.createdAt),
            EXTRACT(MONTH FROM e.createdAt)
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
    @Query("""
        SELECT 
            EXTRACT(YEAR FROM e.updatedAt) AS year,
            EXTRACT(MONTH FROM e.updatedAt) AS month,
            COUNT(e) AS count
        FROM Endorsement e
        WHERE e.organization.organizationId IN :organizationIds
          AND e.endorsementType = :endorsementType
          AND e.updatedAt >= :startDate
          AND e.updatedAt <= :endDate
        GROUP BY 
            EXTRACT(YEAR FROM e.updatedAt),
            EXTRACT(MONTH FROM e.updatedAt)
        ORDER BY 
            year ASC,
            month ASC
        """)
    List<Object[]> getMonthlyEndorsementDeletions(
        @Param("organizationIds") List<UUID> organizationIds,
        @Param("endorsementType") EndorsementType endorsementType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find endorsements by organization, status, and date range (for enrollment report export)
     */
    @Query("""
        SELECT e FROM Endorsement e
        WHERE e.organization.organizationId = :organizationId
        AND e.status = :status
        AND e.approvedAt >= :startDate
        AND e.approvedAt <= :endDate
        """)
    List<Endorsement> findByOrganizationAndDateRange(
        @Param("organizationId") UUID organizationId,
        @Param("status") AccountStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

