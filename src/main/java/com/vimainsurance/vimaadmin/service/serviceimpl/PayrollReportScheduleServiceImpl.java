package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.PayrollReportScheduleRequestDto;
import com.vimainsurance.vimaadmin.dto.PayrollReportScheduleResponseDto;
import com.vimainsurance.vimaadmin.entity.PayrollReportSchedule;
import com.vimainsurance.vimaadmin.repository.IPayrollReportScheduleRepository;
import com.vimainsurance.vimaadmin.service.IPayrollReportScheduleService;

@Service
public class PayrollReportScheduleServiceImpl implements IPayrollReportScheduleService {

    @Autowired
    private IPayrollReportScheduleRepository payrollReportScheduleRepository;

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "payroll_report_schedule", entityType = "PAYROLL_REPORT_SCHEDULE", action = "CREATE")
    public PayrollReportScheduleResponseDto create(PayrollReportScheduleRequestDto request) {
        String emails = request.getRecipientEmails() != null
                ? String.join(",", request.getRecipientEmails())
                : null;
        PayrollReportSchedule entity = PayrollReportSchedule.builder()
                .organizationId(request.getCompanyId())
                .enrollmentWindowId(request.getWindowId())
                .nextRunDate(LocalDate.now().plusMonths(1))
                .frequency(request.getFrequency() != null ? request.getFrequency() : "MONTHLY")
                .recipientEmails(emails)
                .isActive(true)
                .build();
        entity = payrollReportScheduleRepository.save(entity);
        return toResponseDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollReportScheduleResponseDto> listByCompany(UUID companyId) {
        return payrollReportScheduleRepository.findByOrganizationIdOrderByNextRunDateAsc(companyId)
                .stream().map(this::toResponseDto).toList();
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "payroll_report_schedule", entityType = "PAYROLL_REPORT_SCHEDULE", action = "DELETE")
    public void delete(UUID scheduleId) {
        payrollReportScheduleRepository.deleteById(scheduleId);
    }

    @Override
    @Transactional
    public void updateNextRunDate(UUID scheduleId, LocalDate nextRun) {
        payrollReportScheduleRepository.findById(scheduleId).ifPresent(s -> {
            s.setNextRunDate(nextRun);
            payrollReportScheduleRepository.save(s);
        });
    }

    private PayrollReportScheduleResponseDto toResponseDto(PayrollReportSchedule e) {
        return PayrollReportScheduleResponseDto.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .enrollmentWindowId(e.getEnrollmentWindowId())
                .nextRunDate(e.getNextRunDate())
                .frequency(e.getFrequency())
                .recipientEmails(e.getRecipientEmails())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
