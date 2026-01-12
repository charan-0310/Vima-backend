package com.vimainsurance.vimaadmin.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ReportExportRequestDto;

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
}

