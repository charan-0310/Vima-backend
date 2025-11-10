package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.ProductType;

public interface IDealsRepository extends JpaRepository<Deals, UUID> {

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
        """)
    Long countByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("""
        SELECT d FROM Deals d
        WHERE d.leadId = :leadId
        """)
    Optional<Deals> findByLeadId(@Param("leadId") UUID leadId);

}
