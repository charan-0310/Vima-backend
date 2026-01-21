package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.service.IReportExportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for report export operations
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "APIs for generating and exporting reports")
public class ReportExportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportExportController.class);

    @Autowired
    private IReportExportService reportExportService;

    /**
     * Export report data to Excel format
     *
     * @param companyId  Organization/Company ID
     * @param reportType Type of report (master, enrollment, payroll)
     * @param startDate  Optional start date for timeline filter (format: YYYY-MM-DD)
     * @param toDate     Optional end date for timeline filter (format: YYYY-MM-DD)
     * @return Excel file as a downloadable resource
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Export report to Excel",
            description = "Generates and exports member/endorsement data into a standard .xlsx format. " +
                    "Supports three report types: master (all members), enrollment (approved endorsements), and payroll (active members with premium)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Excel file generated successfully",
                    content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid parameters",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    public ResponseEntity<Resource> exportReport(
            @Parameter(description = "Organization/Company ID", required = true)
            @RequestParam UUID companyId,

            @Parameter(description = "Type of report: Employees, Endorsement", required = true)
            @RequestParam String reportType,

            @Parameter(description = "Start date for timeline filter (format: YYYY-MM-DD)")
            @RequestParam(required = false) String startDate,

            @Parameter(description = "End date for timeline filter (format: YYYY-MM-DD)")
            @RequestParam(required = false) String toDate,

            @Parameter(description = "Status filters (e.g., Pending, Approved, Active)")
            @RequestParam(required = false) String status,

            @Parameter(description = "Employee with includeDependents")
            @RequestParam(required = false) Boolean includeDependents


    ) {
        logger.info("[correlationId:{}] /reports/export (GET) endpoint called - companyId: {}, reportType: {}",
                MDC.get("correlationId"), companyId, reportType);

        try {
            // Build request DTO from query parameters
            ReportExportRequestDto requestDto = new ReportExportRequestDto();
            requestDto.setCompanyId(companyId);
            requestDto.setReportType(reportType);
            requestDto.setFromDate(startDate);
            requestDto.setToDate(toDate);
            requestDto.setStatus(status);
            requestDto.setIncludeDependents(includeDependents);


            return reportExportService.exportToExcel(requestDto);

        } catch (BadRequestException e) {
            logger.warn("[correlationId:{}] Bad request for report export: {}", MDC.get("correlationId"), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error exporting report", MDC.get("correlationId"), e);
            throw new RuntimeException("Failed to export report: " + e.getMessage(), e);
        }
    }

    /**
     * Get report records as JSON
     *
     * @param companyId         Organization/Company ID
     * @param reportType        Type of report (e.g., EMPLOYEE_ACTIVE, ENDORSEMENT)
     * @param startDate         Optional start date for timeline filter (format: YYYY-MM-DD)
     * @param toDate            Optional end date for timeline filter (format: YYYY-MM-DD)
     * @param status            Optional status filters (e.g., PENDING_APPROVAL, COMPLETED)
     * @param includeDependents Optional flag to include dependents (defaults to true for EMPLOYEE_ACTIVE)
     * @return JSON response containing report records
     */
    @GetMapping("/data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'VIMA_ADMIN', 'SALES_MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Get report records as JSON",
            description = "Returns employee records or endorsement records based on reportType."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Records fetched successfully",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    public ResponseEntity<ReportTableDto> getReports(
            @Parameter(description = "Organization/Company ID", required = true)
            @RequestParam UUID companyId,
            @Parameter(description = "Type of report: EMPLOYEE_ACTIVE, EMPLOYEE_INACTIVE, EMPLOYEE_CHANGES, ENDORSEMENT", required = true)
            @RequestParam String reportType,
            @Parameter(description = "Start date for timeline filter (format: YYYY-MM-DD)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "End date for timeline filter (format: YYYY-MM-DD)")
            @RequestParam(required = false) String toDate,
            @Parameter(description = "Status filters (e.g., PENDING_APPROVAL, COMPLETED, ACTIVE)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Include dependents for EMPLOYEE_ACTIVE; defaults to true if omitted")
            @RequestParam(required = false) Boolean includeDependents
    ) {
        logger.info("[correlationId:{}] /api/v1/reports (GET) - JSON records - companyId: {}, reportType: {}",
                MDC.get("correlationId"), companyId, reportType);
        try {
            ReportExportRequestDto requestDto = new ReportExportRequestDto();
            requestDto.setCompanyId(companyId);
            requestDto.setReportType(reportType);
            requestDto.setFromDate(startDate);
            requestDto.setToDate(toDate);
            requestDto.setStatus(status);
            requestDto.setIncludeDependents(includeDependents);

            ReportTableDto report = reportExportService.getReportTable(requestDto);
            return ResponseEntity.ok(report);
        } catch (BadRequestException e) {
            logger.warn("[correlationId:{}] Bad request for /api/v1/reports: {}", MDC.get("correlationId"), e.getMessage());
            return ResponseEntity.status(400).body(null);
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching report records", MDC.get("correlationId"), e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
