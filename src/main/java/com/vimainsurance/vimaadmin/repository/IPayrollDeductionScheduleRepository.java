package com.vimainsurance.vimaadmin.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.PayrollDeductionSchedule;

@Repository
public interface IPayrollDeductionScheduleRepository extends JpaRepository<PayrollDeductionSchedule, UUID> {

    List<PayrollDeductionSchedule> findByOrganizationIdOrderByEmployeeIdAscEffectiveFromDesc(
            UUID organizationId);

    @Query("""
        SELECT p FROM PayrollDeductionSchedule p
        WHERE p.organizationId = :organizationId
        AND (p.effectiveTo IS NULL OR p.effectiveTo >= :asOfDate)
        AND p.effectiveFrom <= :asOfDate
        ORDER BY p.employeeId, p.effectiveFrom
        """)
    List<PayrollDeductionSchedule> findByOrganizationIdAndEffectiveOn(
            @Param("organizationId") UUID organizationId,
            @Param("asOfDate") LocalDate asOfDate);

    List<PayrollDeductionSchedule> findByEnrollmentSubmissionId(UUID enrollmentSubmissionId);

    void deleteByEnrollmentSubmissionId(UUID enrollmentSubmissionId);

    List<PayrollDeductionSchedule> findByOrganizationIdAndEnrollmentSubmissionIdIn(
            UUID organizationId, List<UUID> enrollmentSubmissionIds);
}
