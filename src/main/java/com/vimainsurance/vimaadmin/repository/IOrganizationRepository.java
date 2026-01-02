package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.Industry;

@Repository
public interface IOrganizationRepository extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {
    Optional<Organization> findByOrganizationId(UUID organizationId);
    Optional<Organization> findByOrganizationName(String organizationName);
    List<Organization> findAllByStatus(String status);
    
    @Query("""
        SELECT o FROM Organization o
        WHERE o.industry = :industry
        """)
    Page<Organization> findAllByIndustry(@Param("industry") Industry industry, Pageable pageable);
}


