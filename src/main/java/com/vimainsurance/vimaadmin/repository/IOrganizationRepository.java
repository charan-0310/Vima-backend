package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.Industry;

@Repository
public interface IOrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findByOrganizationId(UUID organizationId);
    Optional<Organization> findByOrganizationName(String organizationName);
    List<Organization> findAllByStatus(String status);
    
    @Query("""
        SELECT o FROM Organization o
        WHERE (
            LOWER(o.organizationName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.gstin) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.panNumber) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.primaryContactName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.primaryContactEmail) LIKE LOWER(CONCAT('%', :search, '%'))
            OR o.primaryContactPhone LIKE CONCAT('%', :search, '%')
        )
        """)
    Page<Organization> searchOrganizations(@Param("search") String search, Pageable pageable);
    
    @Query("""
        SELECT o FROM Organization o
        WHERE o.status = :status
        """)
    Page<Organization> findAllByStatusWithPagination(@Param("status") String status, Pageable pageable);
    
    @Query("""
        SELECT o FROM Organization o
        WHERE o.status = :status
        AND (
            LOWER(o.organizationName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.gstin) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.panNumber) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.primaryContactName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(o.primaryContactEmail) LIKE LOWER(CONCAT('%', :search, '%'))
            OR o.primaryContactPhone LIKE CONCAT('%', :search, '%')
        )
        """)
    Page<Organization> searchOrganizationsByStatus(@Param("status") String status, @Param("search") String search, Pageable pageable);
    
    @Query("""
        SELECT o FROM Organization o
        WHERE o.industry = :industry
        """)
    Page<Organization> findAllByIndustry(@Param("industry") Industry industry, Pageable pageable);
}


