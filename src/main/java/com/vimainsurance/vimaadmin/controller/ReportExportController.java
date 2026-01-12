package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

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

import com.vimainsurance.vimaadmin.dto.ReportExportRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
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
     * @param companyId Organization/Company ID
     * @param reportType Type of report (master, enrollment, payroll)
     * @param month Month for filtering (format: YYYY-MM)
     * @param statusFilters Optional status filters (ACTIVE, INACTIVE, etc.)
     * @param fromDate Optional start date for timeline filter (format: YYYY-MM-DD)
     * @param toDate Optional end date for timeline filter (format: YYYY-MM-DD)
     * @param premiumType Optional premium type filter for payroll reports
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

            @Parameter(description = "Type of report: master, enrollment, or payroll", required = true)
            @RequestParam String reportType,

            @Parameter(description = "Month for filtering (format: YYYY-MM)")
            @RequestParam(required = false) String month,

            @Parameter(description = "Status filters (comma-separated): ACTIVE, INACTIVE, PENDING, etc.")
            @RequestParam(required = false) List<String> statusFilters,

            @Parameter(description = "Start date for timeline filter (format: YYYY-MM-DD)")
            @RequestParam(required = false) String fromDate,

            @Parameter(description = "End date for timeline filter (format: YYYY-MM-DD)")
            @RequestParam(required = false) String toDate,

            @Parameter(description = "Premium type filter for payroll reports")
            @RequestParam(required = false) String premiumType
    ) {
        logger.info("[correlationId:{}] /reports/export (GET) endpoint called - companyId: {}, reportType: {}, month: {}",
            MDC.get("correlationId"), companyId, reportType, month);

        try {
            // Build request DTO from query parameters
            ReportExportRequestDto requestDto = new ReportExportRequestDto();
            requestDto.setCompanyId(companyId);
            requestDto.setReportType(reportType);
            requestDto.setMonth(month);
            requestDto.setStatusFilters(statusFilters);
            requestDto.setFromDate(fromDate);
            requestDto.setToDate(toDate);
            requestDto.setPremiumType(premiumType);

            return reportExportService.exportToExcel(requestDto);

        } catch (BadRequestException e) {
            logger.warn("[correlationId:{}] Bad request for report export: {}", MDC.get("correlationId"), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error exporting report", MDC.get("correlationId"), e);
            throw new RuntimeException("Failed to export report: " + e.getMessage(), e);
        }
    }
}

