package com.vimainsurance.vimaadmin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.PayrollReportSchedule;

@Repository
public interface IPayrollReportScheduleRepository extends JpaRepository<PayrollReportSchedule, UUID> {

    List<PayrollReportSchedule> findByIsActiveTrue();

    List<PayrollReportSchedule> findByOrganizationIdOrderByNextRunDateAsc(UUID organizationId);
}
