package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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
}

