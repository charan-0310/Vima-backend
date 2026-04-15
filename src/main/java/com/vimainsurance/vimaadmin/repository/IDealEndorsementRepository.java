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

import com.vimainsurance.vimaadmin.entity.DealEndorsement;

/**
 * Repository interface for DealEndorsement entity
 * Handles the many-to-many relationship between Deals and Endorsement
 */
@Repository
public interface IDealEndorsementRepository extends JpaRepository<DealEndorsement, UUID> {

    /**
     * Find all DealEndorsement records by deal (individual) ID
     */
    List<DealEndorsement> findByDeal_IndividualId(UUID individualId);

    /**
     * Find all DealEndorsement records by endorsement ID
     */
    List<DealEndorsement> findByEndorsement_EndorsementId(UUID endorsementId);

    @Query("""
        SELECT de FROM DealEndorsement de
        JOIN FETCH de.deal d
        JOIN FETCH de.endorsement e
        WHERE e.endorsementId IN :endorsementIds
        """)
    List<DealEndorsement> findByEndorsement_EndorsementIdIn(@Param("endorsementIds") List<UUID> endorsementIds);

    /**
     * Find all DealEndorsement records by endorsement ID with pagination
     */
    Page<DealEndorsement> findByEndorsement_EndorsementId(UUID endorsementId, Pageable pageable);

    /**
     * Find a specific DealEndorsement by deal ID and endorsement ID
     */
    Optional<DealEndorsement> findByDeal_IndividualIdAndEndorsement_EndorsementId(
            UUID individualId, UUID endorsementId);

    /**
     * Check if a relationship exists between a deal and an endorsement
     */
    boolean existsByDeal_IndividualIdAndEndorsement_EndorsementId(
            UUID individualId, UUID endorsementId);

    /**
     * Delete all DealEndorsement records for a specific deal
     */
    void deleteByDeal_IndividualId(UUID individualId);

    /**
     * Delete all DealEndorsement records for a specific endorsement
     */
    void deleteByEndorsement_EndorsementId(UUID endorsementId);

    /**
     * Delete a specific DealEndorsement by deal ID and endorsement ID
     */
    void deleteByDeal_IndividualIdAndEndorsement_EndorsementId(
            UUID individualId, UUID endorsementId);

    /**
     * Count endorsements for a specific deal
     */
    Long countByDeal_IndividualId(UUID individualId);

    /**
     * Count deals for a specific endorsement
     */
    Long countByEndorsement_EndorsementId(UUID endorsementId);

    /**
     * Find all DealEndorsement records by organization ID through the deal
     */
    @Query("""
        SELECT de FROM DealEndorsement de
        WHERE de.deal.organization.organizationId = :organizationId
        """)
    List<DealEndorsement> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all DealEndorsement records by organization ID through the endorsement
     */
    @Query("""
        SELECT de FROM DealEndorsement de
        WHERE de.endorsement.organization.organizationId = :organizationId
        """)
    List<DealEndorsement> findByEndorsementOrganizationId(@Param("organizationId") UUID organizationId);
}

