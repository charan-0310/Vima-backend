package com.vimainsurance.vimaadmin.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface IDealsRepository extends JpaRepository<Deals, UUID> , JpaSpecificationExecutor<Deals> {

    /**
     * Find deals by multiple individual IDs (batch query for optimization)
     */
    List<Deals> findByIndividualIdIn(List<UUID> individualIds);
    
    /**
     * Count total customers (primary members only)
     */
    Long countByIsPrimaryMemberTrue();
    

    @Query("""
        SELECT COUNT(d) FROM Deals d
        WHERE d.isPrimaryMember = true
        AND d.organization IS NULL
        """)
    Long countByIsPrimaryMemberTrueAndOrganizationIsNull();

    /**
     * Find deal by employee number
     */
    Optional<Deals> findByEmployeeNumber(String employeeNumber);
    
    /**
     * Find deal by employee number and organizationId
     */
    @Query("""
    SELECT d FROM Deals d
    WHERE (
        (d.fullName IS NOT NULL AND LOWER(d.fullName) = LOWER(:name))
        OR (d.fullName IS NULL AND LOWER(TRIM(CONCAT(CONCAT(COALESCE(d.firstName,''), ' '), COALESCE(d.lastName,'')))) = LOWER(:name))
        OR LOWER(d.firstName) = LOWER(:name)
        OR LOWER(d.lastName) = LOWER(:name)
    )
    AND (d.employeeNumber = :employeeNumber OR (d.primaryIndividual IS NOT NULL AND d.primaryIndividual.employeeNumber = :employeeNumber))
    AND (LOWER(d.relationship) = LOWER(:relationship) OR (LOWER(:relationship) = 'self' AND LOWER(d.relationship) = 'employee'))
    AND d.organization.organizationId = :organizationId
    """)
    Optional<Deals> findByNameAndEmployeeNumberAndRelationshipAndOrganizationId(
            @Param("name") String name,
            @Param("employeeNumber") String employeeNumber,
            @Param("relationship") String relationship,
            @Param("organizationId") UUID organizationId
    );

    /**
     * Find deal by employee number and organizationId
     */
    @Query("""
    SELECT d FROM Deals d
    WHERE d.employeeNumber = :employeeNumber
    AND d.organization.organizationId = :organizationId
    """)
    Optional<Deals> findByEmployeeNumberAndOrganizationId(
            @Param("employeeNumber") String employeeNumber,
            @Param("organizationId") UUID organizationId
    );

     /**
     * Find deal by employee number and organizationId
     */
     @Query("""
        SELECT d FROM Deals d
        WHERE d.employeeNumber = :employeeNumber
        AND d.organization.organizationId = :organizationId
        AND d.relationship = :relationship
        """)
    Optional<Deals> findByEmployeeNumberAndOrganizationIdAndRelationship(@Param("employeeNumber") String employeeNumber, @Param("organizationId") UUID organizationId, @Param("relationship") String relationship);
    

    @Query("""
        SELECT d FROM Deals d
        WHERE d.individualId = :individualId
        AND d.organization.organizationId = :organizationId
        """)
    Optional<Deals> findByIndividualIdAndOrganizationId(@Param("individualId") UUID individualId, @Param("organizationId") UUID organizationId);
    /**
     * Batch find deals by employee numbers and organizationId (optimized for large CSV imports)
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.employeeNumber IN :employeeNumbers
        AND d.organization.organizationId = :organizationId
        AND d.isPrimaryMember = true
        """)
    List<Deals> findByEmployeeNumberInAndOrganizationId(@Param("employeeNumbers") List<String> employeeNumbers, @Param("organizationId") UUID organizationId);
    
    /**
     * Batch find deals by employee numbers and organizationId (optimized for large CSV imports)
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE (d.phone IN :employeePhones OR d.email IN :employeeEmails)
        AND d.organization.organizationId = :organizationId
        AND d.isPrimaryMember = true
        """)
    List<Deals> findByEmployeePhoneAndEmployeeEmail(@Param("employeePhones") List<String> employeePhones, @Param("employeeEmails") List<String> employeeEmails, @Param("organizationId") UUID organizationId);
    
    
    /**
     * Find deal by email
     */
    Optional<Deals> findByEmail(String email);

    /**
     * Find deals by organization ID
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.organization.organizationId = :organizationId
        """)
    List<Deals> findByOrganizationId(UUID organizationId);

    /**
     * Search deals by multiple fields
     */
    @Query("""
        SELECT DISTINCT d FROM Deals d
        WHERE (
            LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR d.phone LIKE CONCAT('%', :search, '%')
            OR LOWER(d.panNumber) LIKE LOWER(CONCAT('%', :search, '%'))
            OR d.aadhaarNumber LIKE CONCAT('%', :search, '%')
            OR d.employeeNumber LIKE CONCAT('%', :search, '%')
            OR LOWER(d.city) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.state) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND d.isPrimaryMember = true
        AND d.organization IS NULL
        AND (:productType IS NULL OR EXISTS (
            SELECT 1 FROM Policy p 
            WHERE p.primaryIndividualId = d.individualId 
            AND CAST(p.productType AS string) = :productType
        ))
        """)
    Page<Deals> searchDeals(@Param("search") String search, @Param("productType") String productType, Pageable pageable);
    
    /**
     * Find deals by status with pagination
     */
    @Query("""
        SELECT DISTINCT d FROM Deals d
        WHERE d.status = :status
        AND d.isPrimaryMember = true
        AND d.organization IS NULL
        AND (:productType IS NULL OR EXISTS (
            SELECT 1 FROM Policy p 
            WHERE p.primaryIndividualId = d.individualId 
            AND CAST(p.productType AS string) = :productType
        ))
        """)
    Page<Deals> findAllByStatusWithPagination(@Param("status") String status, @Param("productType") String productType, Pageable pageable);
    
    /**
     * Search deals by status and search term
     */
    @Query("""
        SELECT DISTINCT d FROM Deals d
        WHERE d.status = :status
        AND d.isPrimaryMember = true
        AND d.organization IS NULL
        AND (:productType IS NULL OR EXISTS (
            SELECT 1 FROM Policy p 
            WHERE p.primaryIndividualId = d.individualId 
            AND CAST(p.productType AS string) = :productType
        ))
        AND (
            LOWER(d.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR d.phone LIKE CONCAT('%', :search, '%')
            OR LOWER(d.panNumber) LIKE LOWER(CONCAT('%', :search, '%'))
            OR d.aadhaarNumber LIKE CONCAT('%', :search, '%')
            OR d.employeeNumber LIKE CONCAT('%', :search, '%')
            OR LOWER(d.city) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.state) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<Deals> searchDealsByStatus(@Param("status") String status, @Param("search") String search, @Param("productType") String productType, Pageable pageable);

    /**
     * Find all primary members with pagination
     */
    @Query("""
        SELECT DISTINCT d FROM Deals d
        WHERE d.isPrimaryMember = true
        AND d.organization IS NULL
        AND (:productType IS NULL OR EXISTS (
            SELECT 1 FROM Policy p 
            WHERE p.primaryIndividualId = d.individualId 
            AND CAST(p.productType AS string) = :productType
        ))
        """)
    Page<Deals> findAllPrimaryMembers(@Param("productType") String productType, Pageable pageable);


    @Query("""
        SELECT COUNT(d) FROM Deals d
        WHERE d.organization.organizationId = :organizationId 
        AND d.isPrimaryMember = true
        """)
    Long countByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
        SELECT d FROM Deals d
        WHERE d.leadId = :leadId
        """)
    Optional<Deals> findByLeadId(@Param("leadId") UUID leadId);
    
    /**
     * Find all dependents by primary individual ID
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.primaryIndividual.individualId = :primaryIndividualId
        """)
    List<Deals> findByPrimaryIndividualId(@Param("primaryIndividualId") UUID primaryIndividualId);
    
    /**
     * Find all dependents by primary individual entity (for deletion updates)
     */
    default List<Deals> findByPrimaryIndividual(Deals primaryIndividual) {
        if (primaryIndividual == null || primaryIndividual.getIndividualId() == null) {
            return new ArrayList<>();
        }
        return findByPrimaryIndividualId(primaryIndividual.getIndividualId());
    }
    
    /**
     * Batch find primary employees by employee numbers, organizationId, and relationship (optimized for bulk uploads)
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.employeeNumber IN :employeeNumbers
        AND d.organization.organizationId = :organizationId
        AND d.relationship = :relationship
        """)
    List<Deals> findByEmployeeNumberInAndOrganizationIdAndRelationship(
        @Param("employeeNumbers") List<String> employeeNumbers, 
        @Param("organizationId") UUID organizationId,
        @Param("relationship") String relationship);
    
    /**
     * Batch find dependents by multiple primary individual IDs (optimized for bulk uploads)
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.primaryIndividual.individualId IN :primaryIndividualIds
        """)
    List<Deals> findByPrimaryIndividualIdIn(@Param("primaryIndividualIds") List<UUID> primaryIndividualIds);

    /**
     * Find deals by endorsementId
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.endorsementId = :endorsementId
        """)
    List<Deals> findByEndorsementId(@Param("endorsementId") UUID endorsementId);

    /**
     * Find deals by endorsementId with Pagination
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.endorsementId = :endorsementId
        """)
    Page<Deals> findByEndorsementId(@Param("endorsementId") UUID endorsementId,Pageable pageable);


    @Query("""
        SELECT COUNT(d) FROM Deals d
        WHERE d.endorsementId = :endorsementId
        AND d.relationship = 'SELF'
        """)
    Long countByEndorsementIdAndRelationshipSelf(@Param("endorsementId") UUID endorsementId);

    @Query("""
        SELECT COUNT(d) FROM Deals d
        WHERE d.endorsementId = :endorsementId
        AND (d.relationship != 'SELF' OR d.relationship IS NULL)
        """)
    Long countByEndorsementIdAndRelationshipNonSelf(@Param("endorsementId") UUID endorsementId);

    /**
     * Activate approved deals where date of joining has passed
     * UPDATE customers SET status='ACTIVE', updated_at=CURRENT_TIMESTAMP
     * WHERE status='APPROVED' AND date_of_joining <= CURRENT_DATE AND endorsement_id = :endorsementId
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Deals d 
        SET d.status = :activeStatus, d.updatedAt = :updatedAt
        WHERE d.status = :approvedStatus 
        AND d.dateOfJoining <= :currentDate
        AND d.endorsementId = :endorsementId
        """)
    int activateApprovedDeals(
        @Param("endorsementId") UUID endorsementId,
        @Param("approvedStatus") AccountStatus approvedStatus,
        @Param("activeStatus") AccountStatus activeStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * Deactivate leaving deals where date of exit has passed
     * UPDATE customers SET status='INACTIVE', updated_at=CURRENT_TIMESTAMP
     * WHERE status='LEAVING' AND date_of_exit <= CURRENT_DATE AND endorsement_id = :endorsementId
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Deals d 
        SET d.status = :inactiveStatus, d.updatedAt = :updatedAt
        WHERE d.status = :leavingStatus 
        AND d.dateOfExit <= :currentDate
        AND d.endorsementId = :endorsementId
        """)
    int deactivateLeavingDeals(
        @Param("endorsementId") UUID endorsementId,
        @Param("leavingStatus") AccountStatus leavingStatus,
        @Param("inactiveStatus") AccountStatus inactiveStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * Activate approved deals (without date check) - for confirm method
     * UPDATE customers SET status='ACTIVE', updated_at=CURRENT_TIMESTAMP
     * WHERE status='APPROVED' AND endorsement_id = :endorsementId
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Deals d 
        SET d.status = :activeStatus, d.updatedAt = :updatedAt
        WHERE d.status = :approvedStatus 
        AND d.endorsementId = :endorsementId
        """)
    int activateApprovedDealsForConfirm(
        @Param("endorsementId") UUID endorsementId,
        @Param("approvedStatus") AccountStatus approvedStatus,
        @Param("activeStatus") AccountStatus activeStatus,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * Deactivate leaving deals (without date check) - for confirm method
     * UPDATE customers SET status='INACTIVE', updated_at=CURRENT_TIMESTAMP
     * WHERE status='LEAVING' AND endorsement_id = :endorsementId
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Deals d 
        SET d.status = :inactiveStatus, d.updatedAt = :updatedAt
        WHERE d.status = :leavingStatus 
        AND d.endorsementId = :endorsementId
        """)
    int deactivateLeavingDealsForConfirm(
        @Param("endorsementId") UUID endorsementId,
        @Param("leavingStatus") AccountStatus leavingStatus,
        @Param("inactiveStatus") AccountStatus inactiveStatus,
        @Param("updatedAt") LocalDateTime updatedAt
    );


    List<Deals> findByDateOfJoiningIsBefore(LocalDate dateOfJoining);

    /**
     * Activate all approved deals where date of joining has passed (for scheduled confirmation)
     * UPDATE customers SET status='ACTIVE', updated_at=CURRENT_TIMESTAMP
     * WHERE status='APPROVED' AND date_of_joining <= CURRENT_DATE
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Deals d 
        SET d.status = :activeStatus, d.updatedAt = :updatedAt
        WHERE d.status = :approvedStatus 
        AND d.dateOfJoining <= :currentDate
        AND d.dateOfJoining IS NOT NULL
        """)
    int activateAllApprovedDealsByDate(
        @Param("approvedStatus") AccountStatus approvedStatus,
        @Param("activeStatus") AccountStatus activeStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * Deactivate all leaving deals where date of exit has passed (for scheduled confirmation)
     * UPDATE customers SET status='INACTIVE', updated_at=CURRENT_TIMESTAMP
     * WHERE status='LEAVING' AND date_of_exit <= CURRENT_DATE
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Deals d 
        SET d.status = :inactiveStatus, d.updatedAt = :updatedAt
        WHERE d.status = :leavingStatus 
        AND d.dateOfExit <= :currentDate
        AND d.dateOfExit IS NOT NULL
        """)
    int deactivateAllLeavingDealsByDate(
        @Param("leavingStatus") AccountStatus leavingStatus,
        @Param("inactiveStatus") AccountStatus inactiveStatus,
        @Param("currentDate") LocalDate currentDate,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * Find deals that will be activated (for extracting endorsement IDs)
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.status = :status 
        AND d.dateOfJoining <= :currentDate
        AND d.dateOfJoining IS NOT NULL
        """)
    List<Deals> findByStatusAndDateOfJoining(
        @Param("status") AccountStatus status,
        @Param("currentDate") LocalDate currentDate
    );

    /**
     * Find deals that will be deactivated (for extracting endorsement IDs)
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.status = :status 
        AND d.dateOfExit <= :currentDate
        AND d.dateOfExit IS NOT NULL
        """)
    List<Deals> findByStatusAndDateOfExit(
        @Param("status") AccountStatus status,
        @Param("currentDate") LocalDate currentDate
    );

    /**
     * Find deals by organization ID and status list (for report export)
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.organization.organizationId = :organizationId
        AND d.status IN :statuses
        """)
    List<Deals> findByOrganizationIdAndStatusIn(
        @Param("organizationId") UUID organizationId,
        @Param("statuses") List<AccountStatus> statuses
    );

    @Query("""
        SELECT COUNT(d) FROM Deals d
        WHERE d.organization.organizationId = :organizationId
        AND d.relationship = 'SELF'
        """)
    Long countByOrganizationIdAndRelationshipSelf(@Param("organizationId") UUID organizationId);

    @Query("""
        SELECT COUNT(d) FROM Deals d
        WHERE d.organization.organizationId = :organizationId
        AND (d.relationship != 'SELF' OR d.relationship IS NULL)
        """)
    Long countByOrganizationIdAndRelationshipNonSelf(@Param("organizationId") UUID organizationId);

    @Query("""   
        SELECT d FROM Deals d
        WHERE d.organization.organizationId = :organizationId
        AND d.status IN :statuses
        AND d.endorsementId IS NOT NULL
    """)
    List<Deals> findByOrganizationIdAndStatusInAndEndorsementNotNull(
            @Param("organizationId") UUID organizationId,
            @Param("statuses") List<AccountStatus> statuses
    );

    /**
     * Find deals by organization ID and statuses where primaryIndividual is null.
     */
    @Query("""
        SELECT d FROM Deals d
        WHERE d.organization.organizationId = :organizationId
        AND d.status IN :statuses
        AND d.isPrimaryMember is true
    """)
    List<Deals> findByOrganizationIdAndStatusInAndPrimaryIndividualIsNull(
            @Param("organizationId") UUID organizationId,
            @Param("statuses") List<AccountStatus> statuses
    );
    /**
     * Count deals (employees + dependents) added through endorsement additions
     * This counts actual deal records linked to endorsements, providing accurate addition counts
     * Note: startDate and endDate can be null - if null, no date filtering is applied
     */
    @Query(value = """
        SELECT COUNT(d.individual_id)
        FROM cpc.customers d
        INNER JOIN cpc.endorsements e ON d.endorsement_id = e.endorsement_id
        WHERE d.organization_id IN :organizationIds
          AND d.endorsement_id IS NOT NULL
          AND d.status::text = 'ACTIVE'
          AND e.status::text = 'COMPLETED'
          AND e.created_at >= COALESCE(CAST(:startDate AS TIMESTAMP), '1970-01-01'::TIMESTAMP)
          AND e.created_at <= COALESCE(CAST(:endDate AS TIMESTAMP), '9999-12-31 23:59:59'::TIMESTAMP)
        """, nativeQuery = true)
    Long countDealsForEndorsementAdditions(
        @Param("organizationIds") List<UUID> organizationIds,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count deals (employees + dependents) deleted through endorsement deletions
     * This counts actual deal records linked to deletion endorsements, providing accurate deletion counts
     * Note: startDate and endDate can be null - if null, no date filtering is applied
     */
    @Query(value = """
        SELECT COUNT(d.individual_id)
        FROM cpc.customers d
        INNER JOIN cpc.endorsements e ON d.endorsement_id = e.endorsement_id
        WHERE d.organization_id IN :organizationIds
          AND d.endorsement_id IS NOT NULL
          AND d.status::text = 'INACTIVE'
          AND e.endorsement_type::text = CAST(:endorsementType AS VARCHAR)
          AND e.status::text = 'COMPLETED'
          AND e.updated_at >= COALESCE(CAST(:startDate AS TIMESTAMP), '1970-01-01'::TIMESTAMP)
          AND e.updated_at <= COALESCE(CAST(:endDate AS TIMESTAMP), '9999-12-31 23:59:59'::TIMESTAMP)
        """, nativeQuery = true)
    Long countDealsForEndorsementDeletions(
        @Param("organizationIds") List<UUID> organizationIds,
        @Param("endorsementType") String endorsementType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

}
