package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.WellnessPartner;

@Repository
public interface IWellnessPartnerRepository extends JpaRepository<WellnessPartner, UUID> {

    Optional<WellnessPartner> findBySlug(String slug);

    Optional<WellnessPartner> findBySlugAndIsActiveTrue(String slug);

    List<WellnessPartner> findByIsActiveTrueOrderByNameAsc();

    List<WellnessPartner> findByCategoryAndIsActiveTrueOrderByNameAsc(String category);

    boolean existsBySlug(String slug);

    @Query(value = "SELECT COUNT(*) FROM admin.wellness_partner_organizations wpo WHERE wpo.partner_id = :partnerId", nativeQuery = true)
    long countOrganizationsByPartnerId(@Param("partnerId") UUID partnerId);
}
