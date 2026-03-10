package com.vimainsurance.vimaadmin.scheduler;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.dto.EmailAttachment;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.entity.PayrollReportSchedule;
import com.vimainsurance.vimaadmin.enums.ReportFormat;
import com.vimainsurance.vimaadmin.repository.IPayrollReportScheduleRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.service.IPayrollDeductionReportService;
import com.vimainsurance.vimaadmin.service.IPayrollReportScheduleService;

import lombok.extern.slf4j.Slf4j;

/**
 * Monthly job: runs on the 1st at 6 AM (configurable). Loads active payroll report schedules,
 * generates Excel report per schedule, emails with attachment, and updates next_run_date.
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "payroll.report.job", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PayrollReportGeneratorJob {

    @Autowired
    private IPayrollReportScheduleRepository payrollReportScheduleRepository;
    @Autowired
    private IPayrollDeductionReportService payrollDeductionReportService;
    @Autowired
    private IPayrollReportScheduleService payrollReportScheduleService;
    @Autowired
    private IEmailService emailService;

    @Scheduled(cron = "${payroll.report.job.cron:0 0 6 1 * *}", zone = "UTC")
    public void run() {
        log.info("PayrollReportGeneratorJob: starting");
        List<PayrollReportSchedule> schedules = payrollReportScheduleRepository.findByIsActiveTrue();
        for (PayrollReportSchedule s : schedules) {
            try {
                byte[] report = payrollDeductionReportService.generateReport(
                        s.getOrganizationId(), s.getEnrollmentWindowId(), ReportFormat.XLSX);
                String filename = "payroll-deductions-" + s.getOrganizationId() + ".xlsx";
                EmailAttachment attachment = new EmailAttachment();
                attachment.setFileName(filename);
                attachment.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                attachment.setContent(report);

                List<String> recipients = s.getRecipientEmails() != null && !s.getRecipientEmails().isBlank()
                        ? Arrays.asList(s.getRecipientEmails().split("\\s*,\\s*"))
                        : List.of();
                if (recipients.isEmpty()) {
                    log.warn("PayrollReportGeneratorJob: schedule {} has no recipient emails, skipping", s.getId());
                    advanceNextRun(s);
                    continue;
                }
                EmailRequest req = EmailRequest.builder()
                        .toList(recipients)
                        .subject("Payroll Deduction Report – " + LocalDate.now().getMonth() + " " + LocalDate.now().getYear())
                        .templateName("payroll-deduction-report")
                        .templateVariables(java.util.Map.<String, Object>of(
                                "companyName", "Company",
                                "reportMonth", LocalDate.now().toString(),
                                "attachmentFileName", filename,
                                "totalEmployerCost", "—",
                                "totalEmployeeCost", "—",
                                "employeeCount", "—"))
                        .attachments(List.of(attachment))
                        .build();
                emailService.sendEmailWithAttachments(req);
                advanceNextRun(s);
                log.info("PayrollReportGeneratorJob: sent report for schedule {}", s.getId());
            } catch (Exception e) {
                log.error("PayrollReportGeneratorJob: failed for schedule {}: {}", s.getId(), e.getMessage(), e);
            }
        }
        log.info("PayrollReportGeneratorJob: completed");
    }

    private void advanceNextRun(PayrollReportSchedule s) {
        LocalDate next = s.getNextRunDate() != null ? s.getNextRunDate().plusMonths(1) : LocalDate.now().plusMonths(1);
        payrollReportScheduleService.updateNextRunDate(s.getId(), next);
    }
}
