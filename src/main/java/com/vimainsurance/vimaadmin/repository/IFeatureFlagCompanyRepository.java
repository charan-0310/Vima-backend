package com.vimainsurance.vimaadmin.repository;

import java.util.UUID;

import com.vimainsurance.vimaadmin.entity.FeatureFlagCompany;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IFeatureFlagCompanyRepository extends JpaRepository<FeatureFlagCompany, UUID> {
}

