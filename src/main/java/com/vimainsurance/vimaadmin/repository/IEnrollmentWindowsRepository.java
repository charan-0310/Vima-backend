package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

@Repository
public interface IEnrollmentWindowsRepository extends JpaRepository<EnrollmentWindows, UUID>, JpaSpecificationExecutor<EnrollmentWindows> {

    Optional<EnrollmentWindows> findByIdAndOrganization_OrganizationId(UUID id, UUID organizationId);

    List<EnrollmentWindows> findAllByOrganization_OrganizationId(UUID organizationId);

    List<EnrollmentWindows> findAllByOrganization_OrganizationIdAndStatus(UUID organizationId, EnrollementStatus status);
}

