package com.vimainsurance.vimaadmin.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vimainsurance.vimaadmin.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IFeatureFlagRepository extends JpaRepository<FeatureFlag, UUID>, JpaSpecificationExecutor<FeatureFlag> {

    Optional<FeatureFlag> findByFlagKey(String flagKey);

    @Query("select distinct f from FeatureFlag f left join fetch f.roles r where f.parentFeatureFlag is null")
    List<FeatureFlag> findAllWithRoles();

    @Query("select featureFlag from FeatureFlag featureFlag where featureFlag.parentFeatureFlag.id = :parentId")
    List<FeatureFlag> findSubFeatureFlagsByParentId(@Param("parentId") UUID parentId);

    @Query("select featureFlag from FeatureFlag featureFlag where featureFlag.parentFeatureFlag.id in :parentIds")
    List<FeatureFlag> findSubFeatureFlagsByParentIds(@Param("parentIds") Collection<UUID> parentIds);

    @Query("select f from FeatureFlag f where f.flagId in :flagIds")
    List<FeatureFlag> findAllByFlagIds(@Param("flagIds") List<UUID> flagIds);
}

