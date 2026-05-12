package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vimainsurance.vimaadmin.entity.FeatureFlagCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IFeatureFlagCompanyRepository extends JpaRepository<FeatureFlagCompany, UUID> {

    /**
     * Find all FeatureFlagCompany records where the featureFlag is also associated
     * with the given role name in FeatureFlagRole table
     */
    @Query("SELECT DISTINCT ffc FROM FeatureFlagCompany ffc " +
            "JOIN FETCH ffc.featureFlag ff " +
            "JOIN FETCH ffc.organization org " +
            "WHERE ff.parentFeatureFlag IS NULL AND " +
            "ffc.featureFlag.flagId IN " +
            "(SELECT ffr.featureFlag.flagId FROM FeatureFlagRole ffr WHERE ffr.roleName = :roleName)")
    List<FeatureFlagCompany> findByRoleName(@Param("roleName") String roleName);

    /**
     * For feature management screens where VIMA_ADMIN can edit org flags, return all parent-level
     * organization feature rows regardless of role mapping.
     */
    @Query("SELECT DISTINCT ffc FROM FeatureFlagCompany ffc " +
            "JOIN FETCH ffc.featureFlag ff " +
            "JOIN FETCH ffc.organization org " +
            "WHERE ff.parentFeatureFlag IS NULL")
    List<FeatureFlagCompany> findAllParentFeaturesWithOrganization();

    /**
     * Find all FeatureFlagCompany records by organization ID and flag IDs
     */
    @Query("SELECT ffc FROM FeatureFlagCompany ffc " +
           "JOIN FETCH ffc.featureFlag ff " +
           "WHERE ffc.organization.organizationId = :organizationId " +
           "AND ffc.featureFlag.flagId IN :flagIds")
    List<FeatureFlagCompany> findByOrganizationIdAndFlagIds(
            @Param("organizationId") UUID organizationId,
            @Param("flagIds") List<UUID> flagIds);

    /**
     * Find FeatureFlagCompany by organization ID
     */
    @Query("SELECT ffc FROM FeatureFlagCompany ffc " +
           "JOIN FETCH ffc.featureFlag ff " +
           "WHERE ffc.organization.organizationId = :organizationId")
    List<FeatureFlagCompany> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find FeatureFlagCompany by flag ID and organization ID
     */
    @Query("SELECT ffc FROM FeatureFlagCompany ffc " +
            "JOIN FETCH ffc.featureFlag ff " +
            "JOIN FETCH ffc.organization org " +
            "WHERE ffc.featureFlag.flagId = :flagId " +
            "AND ffc.organization.organizationId = :organizationId")
    Optional<FeatureFlagCompany> findByFlagIdAndOrganizationId(
            @Param("flagId") UUID flagId,
            @Param("organizationId") UUID organizationId);

    /**
     * All company rows for the given flags (any org). Used to batch-load org overrides for auth/me.
     */
    @Query("SELECT ffc FROM FeatureFlagCompany ffc " +
            "JOIN FETCH ffc.featureFlag ff " +
            "JOIN FETCH ffc.organization org " +
            "WHERE ff.flagId IN :flagIds")
    List<FeatureFlagCompany> findAllByFeatureFlagIdsWithOrg(@Param("flagIds") List<UUID> flagIds);

    /**
     * Distinct parent-level feature assignments per organization (for sidebar counts).
     */
    @Query("SELECT o.organizationId, COUNT(DISTINCT ff.flagId) FROM FeatureFlagCompany ffc " +
            "JOIN ffc.organization o JOIN ffc.featureFlag ff " +
            "WHERE ff.parentFeatureFlag IS NULL " +
            "GROUP BY o.organizationId")
    List<Object[]> countDistinctParentFeaturesByOrganization();
}

