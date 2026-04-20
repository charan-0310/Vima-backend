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

import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.Industry;

@Repository
public interface IOrganizationRepository extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {
    Optional<Organization> findByOrganizationId(UUID organizationId);

    /**
     * All organizations with this exact name (may be more than one if historical duplicates exist).
     * Prefer this over a single-result lookup to avoid {@code NonUniqueResultException}.
     */
    List<Organization> findAllByOrganizationName(String organizationName);
    List<Organization> findAllByStatus(String status);
    List<Organization> findByIsDemoOrgTrueOrderByDemoCreatedAtDesc();

    @Query(value = """
        SELECT *
        FROM cpc.organizations
        WHERE is_demo_org = true
          AND (demo_expires_at IS NULL OR demo_expires_at < :now)
        ORDER BY demo_expires_at ASC NULLS FIRST
        FOR UPDATE SKIP LOCKED
        LIMIT 1
        """, nativeQuery = true)
    Optional<Organization> findReusableDemoOrgForUpdate(@Param("now") LocalDateTime now);
    
    @Query("""
        SELECT o FROM Organization o
        WHERE o.industry = :industry
        """)
    Page<Organization> findAllByIndustry(@Param("industry") Industry industry, Pageable pageable);
}


