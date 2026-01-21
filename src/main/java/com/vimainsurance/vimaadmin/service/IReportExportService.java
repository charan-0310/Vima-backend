package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.ReportDto;
import com.vimainsurance.vimaadmin.dto.ReportTableDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ReportExportRequestDto;
import com.vimainsurance.vimaadmin.dto.ReportExportRowDto;
import java.util.List;

/**
 * Service interface for generating and exporting reports
 */
public interface IReportExportService {

    /**
     * Export report data to Excel format
     *
     * @param requestDto the export request parameters
     * @return ResponseEntity containing the Excel file as a Resource
     */
    ResponseEntity<Resource> exportToExcel(ReportExportRequestDto requestDto);

    /**
     * Get report records as JSON rows for the given request.
     * Returns employee rows for EMPLOYEE_* report types, endorsement rows for ENDORSEMENT.
     */
    ReportDto getReportsRecords(ReportExportRequestDto requestDto);

    ReportTableDto getReportTable(ReportExportRequestDto requestDto);
}
