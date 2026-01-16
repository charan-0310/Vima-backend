package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.AccountStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for export report request parameters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportRequestDto {

    /**
     * Organization/Company ID for filtering data
     */
    private UUID companyId;

    /**
     * Type of report: master, enrollment, payroll
     */
    private String reportType;

    /**
     * Month for filtering (format: YYYY-MM)
     */
    private String month;

    /**
     * Optional: Filter by status (ACTIVE, INACTIVE, etc.)
     */
    private List<String> statusFilters;

    /**
     * Optional: Start date for timeline filter (format: YYYY-MM-DD)
     */
    private String fromDate;

    /**
     * Optional: End date for timeline filter (format: YYYY-MM-DD)
     */
    private String toDate;

    /**
     * Optional: Premium type filter for payroll reports
     */
    private String premiumType;
}

