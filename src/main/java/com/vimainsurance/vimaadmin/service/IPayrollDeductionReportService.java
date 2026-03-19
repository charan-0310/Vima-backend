package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ReportFormat;

/**
 * Generates payroll deduction reports (Excel/CSV) from cpc.payroll_deduction_schedules.
 */
public interface IPayrollDeductionReportService {

    /**
     * Generate report bytes for the given company and optional enrollment window.
     *
     * @param companyId organization id
     * @param windowId  optional enrollment window id; if null, all schedules for company are included
     * @param format    XLSX or CSV
     * @return report file bytes
     */
    byte[] generateReport(UUID companyId, UUID windowId, ReportFormat format);
}
