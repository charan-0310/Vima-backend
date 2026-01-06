package com.vimainsurance.vimaadmin.repository;

import java.util.List;
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
           "WHERE ffc.featureFlag.flagId IN " +
           "(SELECT ffr.featureFlag.flagId FROM FeatureFlagRole ffr WHERE ffr.roleName = :roleName)")
    List<FeatureFlagCompany> findByRoleName(@Param("roleName") String roleName);

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
}

