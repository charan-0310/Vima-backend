package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.PayrollReportScheduleRequestDto;
import com.vimainsurance.vimaadmin.dto.PayrollReportScheduleResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.enums.ReportFormat;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import com.vimainsurance.vimaadmin.service.IPayrollDeductionReportService;
import com.vimainsurance.vimaadmin.service.IPayrollReportScheduleService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/reports")
@Slf4j
public class PayrollReportController {

    private static final String PAYROLL_REPORTS_FLAG = "enrollment.payroll-reports";

    private final IPayrollDeductionReportService payrollDeductionReportService;
    private final IPayrollReportScheduleService payrollReportScheduleService;
    private final FeatureFlagService featureFlagService;

    public PayrollReportController(
            IPayrollDeductionReportService payrollDeductionReportService,
            IPayrollReportScheduleService payrollReportScheduleService,
            FeatureFlagService featureFlagService) {
        this.payrollDeductionReportService = payrollDeductionReportService;
        this.payrollReportScheduleService = payrollReportScheduleService;
        this.featureFlagService = featureFlagService;
    }

    @GetMapping("/payroll-deductions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> downloadReport(
            @RequestParam UUID companyId,
            @RequestParam(required = false) UUID windowId,
            @RequestParam(defaultValue = "xlsx") String format) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(PAYROLL_REPORTS_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<byte[]>().formErrorResponse(403, "Feature enrollment.payroll-reports is not enabled"));
        }
        try {
            ReportFormat reportFormat = "csv".equalsIgnoreCase(format) ? ReportFormat.CSV : ReportFormat.XLSX;
            byte[] bytes = payrollDeductionReportService.generateReport(companyId, windowId, reportFormat);
            String filename = "payroll-deductions-" + companyId + "." + reportFormat.getValue();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            MediaType mediaType = ReportFormat.CSV == reportFormat
                    ? MediaType.parseMediaType("text/csv")
                    : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            return ResponseEntity.ok().headers(headers).contentType(mediaType).body(bytes);
        } catch (Exception e) {
            log.error("Payroll report generation failed", e);
            return ResponseEntity.internalServerError()
                    .body(new BaseResponse<byte[]>().formErrorResponse("Failed to generate report"));
        }
    }

    @PostMapping("/payroll-deductions/schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<PayrollReportScheduleResponseDto>> createSchedule(
            @Valid @RequestBody PayrollReportScheduleRequestDto request) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(PAYROLL_REPORTS_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<PayrollReportScheduleResponseDto>().formErrorResponse(403, "Feature enrollment.payroll-reports is not enabled"));
        }
        BaseResponse<PayrollReportScheduleResponseDto> res = new BaseResponse<>();
        try {
            PayrollReportScheduleResponseDto created = payrollReportScheduleService.create(request);
            return ResponseEntity.ok(res.formSuccessResponse("Schedule created", created));
        } catch (Exception e) {
            log.error("Create payroll schedule failed", e);
            return ResponseEntity.badRequest().body(res.formErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/payroll-deductions/schedules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<List<PayrollReportScheduleResponseDto>>> listSchedules(
            @RequestParam UUID companyId) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(PAYROLL_REPORTS_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<List<PayrollReportScheduleResponseDto>>().formErrorResponse(403, "Feature enrollment.payroll-reports is not enabled"));
        }
        BaseResponse<List<PayrollReportScheduleResponseDto>> res = new BaseResponse<>();
        try {
            List<PayrollReportScheduleResponseDto> list = payrollReportScheduleService.listByCompany(companyId);
            return ResponseEntity.ok(res.formSuccessResponse("Schedules", list, (long) list.size()));
        } catch (Exception e) {
            log.error("List payroll schedules failed", e);
            return ResponseEntity.internalServerError().body(res.formErrorResponse("Failed to list schedules"));
        }
    }

    @DeleteMapping("/payroll-deductions/schedule/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> deleteSchedule(@PathVariable UUID id) {
        if (!featureFlagService.isFeatureEnabledForCurrentUser(PAYROLL_REPORTS_FLAG)) {
            return ResponseEntity.status(403).body(new BaseResponse<Void>().formErrorResponse(403, "Feature enrollment.payroll-reports is not enabled"));
        }
        BaseResponse<Void> res = new BaseResponse<>();
        try {
            payrollReportScheduleService.delete(id);
            return ResponseEntity.ok(res.formSuccessResponse("Schedule deleted", null));
        } catch (Exception e) {
            log.error("Delete payroll schedule failed", e);
            return ResponseEntity.badRequest().body(res.formErrorResponse(e.getMessage()));
        }
    }
}
