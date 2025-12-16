package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.EndorsementType;

@Repository
public interface IEndorsementRepository extends JpaRepository<Endorsement, UUID> {
    
    Optional<Endorsement> findByEndorsementId(UUID endorsementId);
    
    List<Endorsement> findByOrganization_OrganizationId(UUID organizationId);
    
    Page<Endorsement> findByOrganization_OrganizationId(UUID organizationId, Pageable pageable);
    
    List<Endorsement> findByStatus(AccountStatus status);
    
    Page<Endorsement> findByStatus(AccountStatus status, Pageable pageable);
    
    List<Endorsement> findByEndorsementType(EndorsementType endorsementType);
    
    Page<Endorsement> findByEndorsementType(EndorsementType endorsementType, Pageable pageable);
    
    @Query("""
        SELECT e FROM Endorsement e
        WHERE e.organization.organizationId = :organizationId
        AND e.status = :status
        """)
    List<Endorsement> findByOrganizationIdAndStatus(
        @Param("organizationId") UUID organizationId,
        @Param("status") AccountStatus status
    );
    
    @Query("""
        SELECT e FROM Endorsement e
        WHERE e.organization.organizationId = :organizationId
        AND e.endorsementType = :endorsementType
        """)
    List<Endorsement> findByOrganizationIdAndEndorsementType(
        @Param("organizationId") UUID organizationId,
        @Param("endorsementType") EndorsementType endorsementType
    );
    
    @Query("""
        SELECT e FROM Endorsement e
        WHERE e.organization.organizationId = :organizationId
        AND e.status = :status
        AND e.endorsementType = :endorsementType
        """)
    Page<Endorsement> findByOrganizationIdAndStatusAndEndorsementType(
        @Param("organizationId") UUID organizationId,
        @Param("status") AccountStatus status,
        @Param("endorsementType") EndorsementType endorsementType,
        Pageable pageable
    );
    
    @Query("""
        SELECT e FROM Endorsement e
        WHERE LOWER(e.organization.organizationName) LIKE LOWER(CONCAT('%', :organizationName, '%'))
        """)
    Page<Endorsement> findByOrganizationName(
        @Param("organizationName") String organizationName,
        Pageable pageable
    );
    
    @Query("""
        SELECT e FROM Endorsement e
        WHERE LOWER(e.organization.organizationName) LIKE LOWER(CONCAT('%', :organizationName, '%'))
        AND e.status = :status
        """)
    Page<Endorsement> findByOrganizationNameAndStatus(
        @Param("organizationName") String organizationName,
        @Param("status") AccountStatus status,
        Pageable pageable
    );
    
    @Query("""
        SELECT e FROM Endorsement e
        WHERE LOWER(e.organization.organizationName) LIKE LOWER(CONCAT('%', :organizationName, '%'))
        AND e.endorsementType = :endorsementType
        """)
    Page<Endorsement> findByOrganizationNameAndEndorsementType(
        @Param("organizationName") String organizationName,
        @Param("endorsementType") EndorsementType endorsementType,
        Pageable pageable
    );
    
    @Query("""
        SELECT e FROM Endorsement e
        WHERE LOWER(e.organization.organizationName) LIKE LOWER(CONCAT('%', :organizationName, '%'))
        AND e.status = :status
        AND e.endorsementType = :endorsementType
        """)
    Page<Endorsement> findByOrganizationNameAndStatusAndEndorsementType(
        @Param("organizationName") String organizationName,
        @Param("status") AccountStatus status,
        @Param("endorsementType") EndorsementType endorsementType,
        Pageable pageable
    );
}

