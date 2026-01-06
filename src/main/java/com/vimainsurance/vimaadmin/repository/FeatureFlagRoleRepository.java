package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.entity.FeatureFlagRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureFlagRoleRepository extends JpaRepository<FeatureFlagRole, UUID> {

   @Query("SELECT ffr FROM FeatureFlagRole ffr WHERE ffr.featureFlag.flagId IN :flagIds AND UPPER(ffr.roleName) = UPPER(:roleName)")
    List<FeatureFlagRole> findByFeatureFlagIdsAndRoleName(@Param("flagIds") List<UUID> flagIds, @Param("roleName") String roleName);
}
