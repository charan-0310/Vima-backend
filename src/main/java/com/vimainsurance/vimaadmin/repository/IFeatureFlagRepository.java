package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;
import java.util.UUID;

import com.vimainsurance.vimaadmin.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IFeatureFlagRepository extends JpaRepository<FeatureFlag, UUID>, JpaSpecificationExecutor<FeatureFlag> {

    /**
     * Load a FeatureFlag including roles and companies in a single query using fetch joins.
     */
    @Query("SELECT DISTINCT f FROM FeatureFlag f LEFT JOIN FETCH f.roles r LEFT JOIN FETCH f.companies c WHERE f.flagKey = :flagKey")
    Optional<FeatureFlag> findByFlagKeyWithRelations(@Param("flagKey") String flagKey);
}

