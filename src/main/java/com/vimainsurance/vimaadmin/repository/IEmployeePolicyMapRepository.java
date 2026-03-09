package com.vimainsurance.vimaadmin.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;

@Repository
public interface IEmployeePolicyMapRepository
        extends JpaRepository<EmployeePolicyMap, UUID>, JpaSpecificationExecutor<EmployeePolicyMap> {

    List<EmployeePolicyMap> findByIndividualIdAndStatus(UUID individualId, String status);

    List<EmployeePolicyMap> findByPolicyIdAndStatus(Long policyId, String status);

    List<EmployeePolicyMap> findByPrimaryEmployeeIdAndStatus(UUID primaryEmployeeId, String status);

    List<EmployeePolicyMap> findByIndividualIdAndRelationshipAndStatus(
            UUID individualId, String relationship, String status);

    Page<EmployeePolicyMap> findByOrganizationIdAndStatus(UUID organizationId, String status, Pageable pageable);

    boolean existsByIndividualIdAndPolicyIdAndStatus(UUID individualId, Long policyId, String status);

    List<EmployeePolicyMap> findByEnrollmentWindowIdAndStatus(UUID windowId, String status);

    List<EmployeePolicyMap> findByEndorsementIdAndStatus(UUID endorsementId, String status);

    @Modifying
    @Query("""
        UPDATE EmployeePolicyMap m
        SET m.status = 'CANCELLED', m.effectiveTo = :effectiveDate,
            m.cancellationReason = :reason, m.cancelledAt = CURRENT_TIMESTAMP,
            m.updatedAt = CURRENT_TIMESTAMP
        WHERE (m.individualId = :employeeId OR m.primaryEmployeeId = :employeeId)
        AND m.status = 'ACTIVE'
        """)
    int cancelAllForEmployee(
            @Param("employeeId") UUID employeeId,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("reason") String reason);
}
