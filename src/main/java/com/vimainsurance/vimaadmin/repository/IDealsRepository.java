package com.vimainsurance.vimaadmin.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.vimainsurance.vimaadmin.entity.Deals;

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
        WHERE d.employeeNumber = :employeeNumber
        AND d.organization.organizationId = :organizationId
        """)
    Optional<Deals> findByEmployeeNumberAndOrganizationId(@Param("employeeNumber") String employeeNumber, @Param("organizationId") UUID organizationId);
    
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
}
