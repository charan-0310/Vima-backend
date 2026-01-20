package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.ReportExportRequestDto;
import com.vimainsurance.vimaadmin.dto.ReportExportRowDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.ReportType;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IReportExportService;

/**
 * Service implementation for generating and exporting reports to Excel
 */
@Service
@Transactional(readOnly = true)
public class ReportExportServiceImpl implements IReportExportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportExportServiceImpl.class);

    // Standard headers for master report
    private static final String[] ACTIVE_EMPLOYEE_HEADERS = {"Employee ID", "Employee Name", "Relationship ", "Person Name",
            "Date of Birth", "Gender", "Email", "Phone", "Department", "Designation", "Join Date", "Coverage Start Date",
            "Policy number", "insurer", "TPA", "Sum Insured", "Status", "E-card status"};

    private static final String[] INACTIVE_EMPLOYEE_HEADERS = {"Employee ID", "Employee Name", "Relationship ", "Person Name",
            "Date of Birth", "Gender", "Email", "Phone", "Department", "Designation", "Join Date", "Coverage Start Date",
            "Exit Date", "Coverage End Date", "Days Covered", "Exit Reason", "Endorsement Id", "Exit Notes"};

    private static final String[] CHANGES_EMPLOYEE_HEADERS = {"Change Date", "Change Type", "Endorsement Id",
            "Endorsement Status", "Employee ID", "Employee Name", "Relationship ", "Person Name", "Date of Birth", "Gender",
            "Email", "Phone", "Department", "Reason"};

    // Standard headers for enrollment report
    private static final String[] ENDROSEMENT_HEADERS = {"Endorsement ID", "Endorsement Date", "Endorsement Type", "Status",
            "Employees Added", "Employees Removed", "Dependents Added", "Dependents Removed", "Total Lives Changed", "Submission Date",
            "Approval Date", "Completion Date", "Submitted By", "Approved By", "Notes"};

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IEndorsementRepository endorsementRepository;

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Override
    public ResponseEntity<Resource> exportToExcel(ReportExportRequestDto requestDto) {
        logger.info("Starting report export - type: {}, companyId: {}", requestDto.getReportType(), requestDto.getCompanyId());

        validateRequest(requestDto);

        ReportType reportType = ReportType.fromValue(requestDto.getReportType());

        try {
            ByteArrayInputStream excelStream;
            String fileName = switch (reportType) {
                case EMPLOYEE_ACTIVE, EMPLOYEE_INACTIVE, EMPLOYEE_CHANGES -> {
                    excelStream = generateEmployeeReport(requestDto);
                    yield generateFileName(reportType.getValue(), requestDto);
                }
                case ENDORSEMENT -> {
                    excelStream = generateEndorsementReport(requestDto);
                    yield generateFileName("Endorsement_report", requestDto);
                }
                default -> throw new BadRequestException("Unsupported report type: " + reportType);
            };

            InputStreamResource resource = new InputStreamResource(excelStream);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            logger.info("Report export completed successfully - fileName: {}", fileName);

            return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(resource);

        } catch (IOException e) {
            logger.error("Error generating Excel report", e);
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    /**
     * Validate the export request parameters
     */
    private void validateRequest(ReportExportRequestDto requestDto) {
        if (requestDto.getCompanyId() == null) {
            throw new BadRequestException("Company ID is required");
        }
        if (requestDto.getReportType() == null || requestDto.getReportType().isBlank()) {
            throw new BadRequestException("Report type is required");
        }

        // Validate organization exists
        organizationRepository.findById(requestDto.getCompanyId()).orElseThrow(() ->
                new BadRequestException("Organization not found with ID: " + requestDto.getCompanyId()));
    }

    /**
     * Generate master report - All members with status ACTIVE or INACTIVE
     */
    private ByteArrayInputStream generateEmployeeReport(ReportExportRequestDto requestDto) throws IOException {
        logger.info("Generating master report for companyId: {}", requestDto.getCompanyId());

        // Fetch members with ACTIVE and INACTIVE status
        List<AccountStatus> statuses = new ArrayList<>();
        switch (ReportType.fromValue(requestDto.getReportType())) {
            case EMPLOYEE_ACTIVE:
                statuses.add(AccountStatus.ACTIVE);
                break;
            case EMPLOYEE_INACTIVE:
                statuses.add(AccountStatus.INACTIVE);
                break;
            default:
                statuses.add(AccountStatus.ACTIVE);
                statuses.add(AccountStatus.INACTIVE);
                break;
        }
        List<Deals> members = new ArrayList<>();
        if (ReportType.EMPLOYEE_CHANGES.getValue().equalsIgnoreCase(requestDto.getReportType())) {
            members = dealsRepository.findByOrganizationIdAndStatusInAndEndorsementNotNull(requestDto.getCompanyId(), statuses);
        } else if(ReportType.EMPLOYEE_ACTIVE.getValue().equalsIgnoreCase(requestDto.getReportType())
                && Boolean.FALSE.equals(requestDto.getIncludeDependents())) {
            members = dealsRepository.findByOrganizationIdAndStatusInAndPrimaryIndividualIsNull(requestDto.getCompanyId(), statuses);
        } else {
            members = dealsRepository.findByOrganizationIdAndStatusIn(requestDto.getCompanyId(), statuses);
        }

        // Apply timeline filter if provided
        members = applyTimelineFilter(members, requestDto.getFromDate(), requestDto.getToDate());

        // Fetch organization details
        Organization organization = organizationRepository.findById(requestDto.getCompanyId()).orElse(null);
        String organizationName = organization != null ? organization.getOrganizationName() : "";

        // Fetch policies for all members
        List<UUID> memberIds = members.stream().map(Deals::getIndividualId).collect(Collectors.toList());
        Map<UUID, Policy> policyMap = fetchPoliciesForMembers(memberIds);

        List<UUID> endorsementIds = new ArrayList<>();
        // Fetch endorsements for all members using their endorsementId
        // Map endorsements
        Map<UUID, Endorsement> endorsementMap;
        if (ReportType.EMPLOYEE_CHANGES.getValue().equalsIgnoreCase(requestDto.getReportType())) {
           endorsementIds = members.stream().map(Deals::getEndorsementId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            // Map endorsements
            endorsementMap = fetchEndorsementsByIds(endorsementIds);
        } else {
            endorsementMap = new HashMap<>();
        }


        // Map to export DTOs
        List<ReportExportRowDto> rows = members.stream().map(member ->
                mapDealToEmployeeRow(member, organizationName, policyMap, endorsementMap)).collect(Collectors.toList());

        logger.info("Master report - Total records: {}", rows.size());

        ReportType reportType = ReportType.valueOf(requestDto.getReportType());

        String[] headers = switch (reportType) {
            case EMPLOYEE_ACTIVE -> ACTIVE_EMPLOYEE_HEADERS;
            case EMPLOYEE_INACTIVE -> INACTIVE_EMPLOYEE_HEADERS;
            case EMPLOYEE_CHANGES -> CHANGES_EMPLOYEE_HEADERS;
            default -> ACTIVE_EMPLOYEE_HEADERS;
        };

        return createExcelWorkbook("Employee Report", headers, rows, reportType);
    }


    /**
     * Generate enrollment report - All approved endorsements for selected month
     */
    private ByteArrayInputStream generateEndorsementReport(ReportExportRequestDto requestDto) throws IOException {
        logger.info("Generating enrollment report for companyId: {}", requestDto.getCompanyId());

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (requestDto.getFromDate() != null && !requestDto.getFromDate().isBlank()) {
            startDate = LocalDate.parse(requestDto.getFromDate()).atStartOfDay();
        }
        if (requestDto.getToDate() != null && !requestDto.getToDate().isBlank()) {
            endDate = LocalDate.parse(requestDto.getToDate()).atTime(23, 59, 59);
        }

        AccountStatus status = null;
        if (requestDto.getStatus() != null && "all".equalsIgnoreCase(requestDto.getStatus())) {
            status = null; // no status filter
        } else if (requestDto.getStatus() != null && !requestDto.getStatus().isBlank()) {
            try {
                status = AccountStatus.valueOf(requestDto.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + requestDto.getStatus());
            }
        }

        // Fetch endorsements for the organization with dynamic date filtering based on provided params
        List<Endorsement> endorsements;
        boolean ignoreStatusFilter = (status == null);
        if (startDate == null && endDate == null) {
            // No date filters
            endorsements = ignoreStatusFilter
                    ? endorsementRepository.findByOrganization(requestDto.getCompanyId())
                    : endorsementRepository.findByOrganization(requestDto.getCompanyId(), status);
        } else if (startDate != null && endDate == null) {
            // Only from-date
            endorsements = ignoreStatusFilter
                    ? endorsementRepository.findByOrganizationAndFromDate(requestDto.getCompanyId(), startDate)
                    : endorsementRepository.findByOrganizationAndFromDate(requestDto.getCompanyId(), status, startDate);
        } else if (startDate == null && endDate != null) {
            // Only to-date
            endorsements = ignoreStatusFilter
                    ? endorsementRepository.findByOrganizationAndToDate(requestDto.getCompanyId(), endDate)
                    : endorsementRepository.findByOrganizationAndToDate(requestDto.getCompanyId(), status, endDate);
        } else {
            // Both from and to
            endorsements = ignoreStatusFilter
                    ? endorsementRepository.findByOrganizationAndDateRange(requestDto.getCompanyId(), startDate, endDate)
                    : endorsementRepository.findByOrganizationAndDateRange(requestDto.getCompanyId(), status, startDate, endDate);
        }

        // Fetch organization details
        Organization organization = organizationRepository.findById(requestDto.getCompanyId()).orElse(null);
        String organizationName = organization != null ? organization.getOrganizationName() : "";

        // Map to export rows
        List<ReportExportRowDto> rows = endorsements.stream().map(endorsement ->
                mapEndorsementToRow(endorsement, organizationName)).collect(Collectors.toList());

        logger.info("Enrollment report - Total records: {}", rows.size());
        return createExcelWorkbook("Endorsement Report", ENDROSEMENT_HEADERS, rows, ReportType.ENDORSEMENT);
    }
    /**
     * Apply timeline filter to member list
     */
    private List<Deals> applyTimelineFilter(List<Deals> members, String fromDate, String toDate) {
        if (fromDate == null && toDate == null) {
            return members;
        }

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (fromDate != null && !fromDate.isBlank()) {
            try {
                startDateTime = LocalDate.parse(fromDate).atStartOfDay();
            } catch (DateTimeParseException e) {
                logger.warn("Invalid fromDate format: {}", fromDate);
            }
        }

        if (toDate != null && !toDate.isBlank()) {
            try {
                endDateTime = LocalDate.parse(toDate).atTime(23, 59, 59);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid toDate format: {}", toDate);
            }
        }

        final LocalDateTime finalStartDateTime = startDateTime;
        final LocalDateTime finalEndDateTime = endDateTime;

        return members.stream().filter(member -> {
            LocalDateTime createdAt = member.getCreatedAt();
            if (createdAt == null) return true;

            boolean afterStart = finalStartDateTime == null || !createdAt.isBefore(finalStartDateTime);
            boolean beforeEnd = finalEndDateTime == null || !createdAt.isAfter(finalEndDateTime);

            return afterStart && beforeEnd;
        }).collect(Collectors.toList());
    }

    /**
     * Fetch policies for a list of member IDs
     */
    private Map<UUID, Policy> fetchPoliciesForMembers(List<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        List<Policy> policies = policyRepository.findByPrimaryIndividualIdIn(memberIds);
        return policies.stream().collect(Collectors.toMap(Policy::getPrimaryIndividualId, policy -> policy, (existing, replacement) -> existing // Keep first in case of duplicates
        ));
    }

    private Map<UUID, Endorsement> fetchEndorsementsByIds(List<UUID> endorsementIds) {
        if (endorsementIds == null || endorsementIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Endorsement> endorsements = endorsementRepository.findByEndorsementIdIn(endorsementIds);
        return endorsements.stream().collect(Collectors.toMap(Endorsement::getEndorsementId, e -> e, (existing, replacement) -> existing));
    }


    /**
     * Map Deals entity to Master report row DTO
     */
    private ReportExportRowDto mapDealToEmployeeRow(Deals deal, String organizationName, Map<UUID, Policy> policyMap, Map<UUID, Endorsement> endorsementMap) {
        Policy policy = policyMap.get(deal.getIndividualId());
        Endorsement endorsement = endorsementMap.get(deal.getEndorsementId());

        return ReportExportRowDto.builder()
                .employeeNumber(deal.getEmployeeNumber())
                .firstName(deal.getFirstName())
                .relationship(deal.getRelationship())
                .fullName(deal.getFullName())
                .dateOfBirth(deal.getDateOfBirth())
                .gender(deal.getGender())
                .email(deal.getEmail())
                .phone(deal.getPhone())
                .department(deal.getDepartment())
                .designation(deal.getDesignation())
                .dateOfJoining(deal.getDateOfJoining())
                .policyStartDate(policy != null ? policy.getStartDate() : null)
                .policyNumber(policy != null ? policy.getPolicyNumber() : null)
                .coverageEndDate(policy != null ? policy.getEndDate() : null)
                .insurerRefNumber(policy != null ?  String.valueOf(policy.getInsuranceProviderId()) : null)
                .tpa("")
                .sumInsured(deal.getSumInsured() != null ? new BigDecimal(deal.getSumInsured().replace(",", "")) : null)
                .status(deal.getStatus() != null ? deal.getStatus().getValue() : null)
                .ecardStatus("")
                .exitDate(endorsement != null && endorsement.getUpdatedAt() != null ? endorsement.getUpdatedAt().toLocalDate() : null)
                .daysCovered(
                        policy != null && policy.getStartDate() != null && policy.getEndDate() != null
                                ? (int) (ChronoUnit.DAYS.between(policy.getStartDate(), policy.getEndDate()) + 1)
                                : null
                )
                .exitReason("")
                .exitNotes("")
                .changeDate(endorsement != null && endorsement.getUpdatedAt() != null ? endorsement.getUpdatedAt().toString() : "")
                .changeType(endorsement != null && endorsement.getEndorsementType() != null ? endorsement.getEndorsementType().getValue() : null)
                .endorsementId(endorsement != null ? endorsement.getEndorsementId() : null)
                .endorsementStatus(endorsement != null && endorsement.getStatus() != null ? endorsement.getStatus().getValue() : null)
                .reason("")
                .notes(policy != null && policy.getDocument() != null ? policy.getDocument().getNotes() : null)
                .build();
    }


    /**
     * Map Endorsement entity to export row DTO
     */
    private ReportExportRowDto mapEndorsementToRow(Endorsement endorsement, String organizationName) {

        return ReportExportRowDto.builder()
                .endorsementId(endorsement.getEndorsementId())
                .createdAt(endorsement.getCreatedAt() != null ? endorsement.getCreatedAt() : null)
                .endorsementType(endorsement.getEndorsementType() != null ? endorsement.getEndorsementType().getValue() : null)
                .endorsementStatus(endorsement.getStatus() != null ? endorsement.getStatus().getValue() : null)
                .totalEmployees(endorsement.getTotalEmployees()).totalEmployeesRemoved(0)
                .totalDependents(endorsement.getTotalDependents()).totalDependentsRemoved(0)
                .totalLivesChanged(endorsement.getTotalEmployees() + endorsement.getTotalDependents())
                .submissionDate("")
                .approvedAt(endorsement.getApprovedAt() != null ? endorsement.getApprovedAt().toLocalDate() : null)
                .completionDate(null)
                .submittedBy(endorsement.getUploadedBy() != null ? endorsement.getUploadedBy().getUsername() : null)
                .approvedBy(endorsement.getApprovedBy())
                .notes("").build();
    }

    /**
     * Create Excel workbook with the given data
     */
    private ByteArrayInputStream createExcelWorkbook(String sheetName, String[] headers, List<ReportExportRowDto> rows, ReportType reportType) throws IOException {

        // Use SXWorkbook for streaming to handle large datasets
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Create header row style
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (ReportExportRowDto row : rows) {
                Row dataRow = sheet.createRow(rowNum++);
                populateRow(dataRow, row, reportType, dateFormatter);
            }

            // Auto-size columns (for first 100 rows only due to SXSSF limitations)
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 4000); // Set default width
            }

            // Write to output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.dispose(); // Dispose of temporary files

            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    /**
     * Create header cell style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    /**
     * Populate a row with data based on report type
     */
    private void populateRow(Row row, ReportExportRowDto data, ReportType reportType, DateTimeFormatter dateFormatter) {
        int col = 0;
        switch (reportType) {
            case EMPLOYEE_ACTIVE:
                row.createCell(col++).setCellValue(nullSafe(data.getEmployeeNumber()));                 // Employee ID
                row.createCell(col++).setCellValue(nullSafe(data.getFirstName()));                      // Employee Name
                row.createCell(col++).setCellValue(nullSafe(data.getRelationship()));                  // Relationship
                row.createCell(col++).setCellValue(nullSafe(data.getFullName()));                      // Person Name
                row.createCell(col++).setCellValue(formatDate(data.getDateOfBirth(), dateFormatter));  // Date of Birth
                row.createCell(col++).setCellValue(nullSafe(data.getGender()));                        // Gender
                row.createCell(col++).setCellValue(nullSafe(data.getEmail()));                         // Email
                row.createCell(col++).setCellValue(nullSafe(data.getPhone()));                         // Phone
                row.createCell(col++).setCellValue(nullSafe(data.getDepartment()));                    // Department
                row.createCell(col++).setCellValue(nullSafe(data.getDesignation()));                   // Designation
                row.createCell(col++).setCellValue(formatDate(data.getDateOfJoining(), dateFormatter));// Join Date
                row.createCell(col++).setCellValue(formatDate(data.getPolicyStartDate(), dateFormatter));// Coverage Start Date
                row.createCell(col++).setCellValue(nullSafe(data.getPolicyNumber()));                  // Policy number
                row.createCell(col++).setCellValue(nullSafe(data.getInsurerRefNumber()));              // insurer
                row.createCell(col++).setCellValue(nullSafe(data.getTpa()));                           // TPA
                row.createCell(col++).setCellValue(data.getSumInsured() != null ? data.getSumInsured().toString() : ""); // Sum Insured
                row.createCell(col++).setCellValue(nullSafe(data.getStatus()));                        // Status
                row.createCell(col++).setCellValue(nullSafe(data.getEcardStatus()));                   // E-card status
                break;
            case EMPLOYEE_INACTIVE:

                row.createCell(col++).setCellValue(nullSafe(data.getEmployeeNumber()));                 // Employee ID
                row.createCell(col++).setCellValue(nullSafe(data.getFirstName()));                      // Employee Name
                row.createCell(col++).setCellValue(nullSafe(data.getRelationship()));                   // Relationship
                row.createCell(col++).setCellValue(nullSafe(data.getFullName()));                       // Person Name
                row.createCell(col++).setCellValue(formatDate(data.getDateOfBirth(), dateFormatter));   // Date of Birth
                row.createCell(col++).setCellValue(nullSafe(data.getGender()));                         // Gender
                row.createCell(col++).setCellValue(nullSafe(data.getEmail()));                          // Email
                row.createCell(col++).setCellValue(nullSafe(data.getPhone()));                          // Phone
                row.createCell(col++).setCellValue(nullSafe(data.getDepartment()));                     // Department
                row.createCell(col++).setCellValue(nullSafe(data.getDesignation()));                    // Designation
                row.createCell(col++).setCellValue(formatDate(data.getDateOfJoining(), dateFormatter)); // Join Date
                row.createCell(col++).setCellValue(formatDate(data.getPolicyStartDate(), dateFormatter)); // Coverage Start Date
                row.createCell(col++).setCellValue(formatDate(data.getExitDate(), dateFormatter));      // Exit Date
                row.createCell(col++).setCellValue(formatDate(data.getCoverageEndDate(), dateFormatter)); // Coverage End Date
                row.createCell(col++).setCellValue(data.getDaysCovered() != null ? data.getDaysCovered() : 0); // Days Covered
                row.createCell(col++).setCellValue(nullSafe(data.getExitReason()));                     // Exit Reason
                row.createCell(col++).setCellValue(data.getEndorsementId() != null ? data.getEndorsementId().toString() : ""); // Endorsement Id
                row.createCell(col++).setCellValue(nullSafe(data.getExitNotes()));                      // Exit Notes

                break;
            case EMPLOYEE_CHANGES:
                row.createCell(col++).setCellValue(nullSafe(data.getChangeDate()));                       // Change Date
                row.createCell(col++).setCellValue(nullSafe(data.getChangeType()));                       // Change Type
                row.createCell(col++).setCellValue(data.getEndorsementId() != null ? data.getEndorsementId().toString() : ""); // Endorsement Id
                row.createCell(col++).setCellValue(nullSafe(data.getEndorsementStatus()));                // Endorsement Status
                row.createCell(col++).setCellValue(nullSafe(data.getEmployeeNumber()));                   // Employee ID
                row.createCell(col++).setCellValue(nullSafe(data.getFirstName()));                        // Employee Name
                row.createCell(col++).setCellValue(nullSafe(data.getRelationship()));                     // Relationship
                row.createCell(col++).setCellValue(nullSafe(data.getFullName()));                         // Person Name
                row.createCell(col++).setCellValue(formatDate(data.getDateOfBirth(), dateFormatter));     // Date of Birth
                row.createCell(col++).setCellValue(nullSafe(data.getGender()));                           // Gender
                row.createCell(col++).setCellValue(nullSafe(data.getEmail()));                            // Email
                row.createCell(col++).setCellValue(nullSafe(data.getPhone()));                            // Phone
                row.createCell(col++).setCellValue(nullSafe(data.getDepartment()));                       // Department
                row.createCell(col++).setCellValue(nullSafe(data.getReason()));                           // Reason
                break;
            case ENDORSEMENT:
                // Follow ENROLLMENT_HEADERS order
                row.createCell(col++).setCellValue(data.getEndorsementId() != null ? data.getEndorsementId().toString() : "");
                row.createCell(col++).setCellValue(formatDateTime(data.getCreatedAt(), dateFormatter)); // Endorsement Date
                row.createCell(col++).setCellValue(nullSafe(data.getEndorsementType()));                // Endorsement Type
                row.createCell(col++).setCellValue(nullSafe(data.getEndorsementStatus()));             // Status
                row.createCell(col++).setCellValue(data.getTotalEmployees() != null ? data.getTotalEmployees() : 0); // Employees Added
                row.createCell(col++).setCellValue(data.getTotalEmployeesRemoved() != null ? data.getTotalEmployeesRemoved() : 0); // Employees Removed
                row.createCell(col++).setCellValue(data.getTotalDependents() != null ? data.getTotalDependents() : 0); // Dependents Added
                row.createCell(col++).setCellValue(data.getTotalDependentsRemoved() != null ? data.getTotalDependentsRemoved() : 0); // Dependents Removed
                row.createCell(col++).setCellValue(data.getTotalLivesChanged() != null ? data.getTotalLivesChanged() : 0); // Total Lives Changed
                row.createCell(col++).setCellValue(nullSafe(data.getSubmissionDate()));                // Submission Date
                row.createCell(col++).setCellValue(formatDate(data.getApprovedAt(), dateFormatter));   // Approval Date
                row.createCell(col++).setCellValue(formatDate(data.getCompletionDate(), dateFormatter)); // Completion Date
                row.createCell(col++).setCellValue(nullSafe(data.getSubmittedBy()));                   // Submitted By
                row.createCell(col++).setCellValue(nullSafe(data.getApprovedBy()));                    // Approved By
                row.createCell(col++).setCellValue(nullSafe(data.getNotes()));                         // Notes
                break;
              }
    }


    /**
     * Generate file name for the export
     */
    private String generateFileName(String prefix, ReportExportRequestDto requestDto) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String companyIdPart = requestDto.getCompanyId().toString().substring(0, 8);
        return String.format("%s_%s_%s.xlsx", prefix, companyIdPart, timestamp);
    }

    /**
     * Null-safe string value
     */
    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * Format date to string
     */
    private String formatDate(LocalDate date, DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : "";
    }

    /**
     * Format datetime to string
     */
    private String formatDateTime(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }
}

