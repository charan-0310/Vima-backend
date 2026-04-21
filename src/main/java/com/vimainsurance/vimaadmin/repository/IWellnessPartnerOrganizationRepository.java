package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.WellnessPartnerOrganization;

@Repository
public interface IWellnessPartnerOrganizationRepository extends JpaRepository<WellnessPartnerOrganization, UUID> {

    List<WellnessPartnerOrganization> findByOrganizationIdOrderByDisplayOrderAsc(UUID orgId);

    List<WellnessPartnerOrganization> findByOrganizationIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID orgId);

    Optional<WellnessPartnerOrganization> findByPartnerIdAndOrganizationId(UUID partnerId, UUID orgId);

    boolean existsByPartnerIdAndOrganizationId(UUID partnerId, UUID orgId);

    Optional<WellnessPartnerOrganization> findByPartner_SlugAndOrganizationId(String slug, UUID orgId);

    @Query("SELECT wpo FROM WellnessPartnerOrganization wpo JOIN FETCH wpo.partner wp "
            + "WHERE wpo.organizationId = :orgId AND wpo.isActive = true AND wp.isActive = true "
            + "ORDER BY wpo.displayOrder ASC, wp.name ASC")
    List<WellnessPartnerOrganization> findActivePartnersByOrganization(@Param("orgId") UUID orgId);

    @Query("SELECT COALESCE(MAX(wpo.displayOrder), -1) FROM WellnessPartnerOrganization wpo WHERE wpo.organizationId = :orgId")
    int findMaxDisplayOrderByOrganizationId(@Param("orgId") UUID orgId);
}
