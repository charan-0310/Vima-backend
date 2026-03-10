package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.PayrollReportScheduleRequestDto;
import com.vimainsurance.vimaadmin.dto.PayrollReportScheduleResponseDto;

/**
 * CRUD for payroll report schedules (recurring report generation).
 */
public interface IPayrollReportScheduleService {

    PayrollReportScheduleResponseDto create(PayrollReportScheduleRequestDto request);

    List<PayrollReportScheduleResponseDto> listByCompany(UUID companyId);

    void delete(UUID scheduleId);

    void updateNextRunDate(UUID scheduleId, java.time.LocalDate nextRun);
}
