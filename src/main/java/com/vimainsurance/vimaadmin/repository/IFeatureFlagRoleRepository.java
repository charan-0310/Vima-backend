package com.vimainsurance.vimaadmin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.vimainsurance.vimaadmin.entity.FeatureFlagRole;

import java.util.UUID;


@Repository
public interface IFeatureFlagRoleRepository extends JpaRepository<FeatureFlagRole, UUID> {
}