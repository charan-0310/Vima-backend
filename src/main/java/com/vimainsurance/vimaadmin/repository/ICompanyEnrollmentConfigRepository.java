package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.CompanyEnrollmentConfig;

@Repository
public interface ICompanyEnrollmentConfigRepository extends JpaRepository<CompanyEnrollmentConfig, UUID> {

    Optional<CompanyEnrollmentConfig> findByOrganizationId(UUID organizationId);
}
