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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final String[] MASTER_HEADERS = {
        "Employee Number", "Full Name", "First Name", "Last Name", "Date of Birth", "Gender",
        "Relationship", "Is Primary Member", "Email", "Phone", "Address", "City", "State", "Pincode",
        "PAN Number", "Aadhaar Number", "Designation", "Date of Joining", "Date of Exit",
        "Organization ID", "Organization Name", "Status", "Account Type", "Marital Status",
        "Sum Insured", "Policy Number", "Policy Start Date", "Policy End Date"
    };

    // Standard headers for enrollment report
    private static final String[] ENROLLMENT_HEADERS = {
        "Endorsement ID", "Endorsement Type", "Status", "Total Employees", "Total Dependents",
        "Approved At", "Approved By", "Organization ID", "Organization Name",
        "Premium Amount", "Premium Change Type", "Insurer Ref Number", "Created At"
    };

    // Standard headers for payroll report
    private static final String[] PAYROLL_HEADERS = {
        "Employee Number", "Full Name", "First Name", "Last Name", "Date of Birth", "Gender",
        "Relationship", "Email", "Phone", "Designation", "Date of Joining",
        "Organization ID", "Organization Name", "Status",
        "Premium Amount", "Sum Insured", "Policy Number", "Policy Start Date", "Policy End Date"
    };

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
        logger.info("Starting report export - type: {}, companyId: {}, month: {}",
            requestDto.getReportType(), requestDto.getCompanyId(), requestDto.getMonth());

        validateRequest(requestDto);

        ReportType reportType = ReportType.fromValue(requestDto.getReportType());

        try {
            ByteArrayInputStream excelStream;
            String fileName;

            switch (reportType) {
                case MASTER:
                    excelStream = generateMasterReport(requestDto);
                    fileName = generateFileName("master_report", requestDto);
                    break;
                case ENROLLMENT:
                    excelStream = generateEnrollmentReport(requestDto);
                    fileName = generateFileName("enrollment_report", requestDto);
                    break;
                case PAYROLL:
                    excelStream = generatePayrollReport(requestDto);
                    fileName = generateFileName("payroll_report", requestDto);
                    break;
                default:
                    throw new BadRequestException("Unsupported report type: " + reportType);
            }

            InputStreamResource resource = new InputStreamResource(excelStream);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            logger.info("Report export completed successfully - fileName: {}", fileName);

            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);

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
        organizationRepository.findById(requestDto.getCompanyId())
            .orElseThrow(() -> new BadRequestException("Organization not found with ID: " + requestDto.getCompanyId()));
    }

    /**
     * Generate master report - All members with status ACTIVE or INACTIVE
     */
    private ByteArrayInputStream generateMasterReport(ReportExportRequestDto requestDto) throws IOException {
        logger.info("Generating master report for companyId: {}", requestDto.getCompanyId());

        // Fetch members with ACTIVE and INACTIVE status
        List<AccountStatus> statuses = new ArrayList<>();
        if (requestDto.getStatusFilters() != null && !requestDto.getStatusFilters().isEmpty()) {
            for (String status : requestDto.getStatusFilters()) {
                statuses.add(AccountStatus.fromValue(status));
            }
        } else {
            statuses.add(AccountStatus.ACTIVE);
            statuses.add(AccountStatus.INACTIVE);
        }

        List<Deals> members = dealsRepository.findByOrganizationIdAndStatusIn(
            requestDto.getCompanyId(),
            statuses
        );

        // Apply timeline filter if provided
        members = applyTimelineFilter(members, requestDto.getFromDate(), requestDto.getToDate());

        // Fetch organization details
        Organization organization = organizationRepository.findById(requestDto.getCompanyId()).orElse(null);
        String organizationName = organization != null ? organization.getOrganizationName() : "";

        // Fetch policies for all members
        List<UUID> memberIds = members.stream()
            .map(Deals::getIndividualId)
            .collect(Collectors.toList());
        Map<UUID, Policy> policyMap = fetchPoliciesForMembers(memberIds);

        // Map to export DTOs
        List<ReportExportRowDto> rows = members.stream()
            .map(member -> mapDealToMasterRow(member, organizationName, policyMap))
            .collect(Collectors.toList());

        logger.info("Master report - Total records: {}", rows.size());
        return createExcelWorkbook("Master Report", MASTER_HEADERS, rows, ReportType.MASTER);
    }

    /**
     * Generate enrollment report - All approved endorsements for selected month
     */
    private ByteArrayInputStream generateEnrollmentReport(ReportExportRequestDto requestDto) throws IOException {
        logger.info("Generating enrollment report for companyId: {}, month: {}",
            requestDto.getCompanyId(), requestDto.getMonth());

        LocalDateTime startDate;
        LocalDateTime endDate;

        if (requestDto.getMonth() != null && !requestDto.getMonth().isBlank()) {
            YearMonth yearMonth = YearMonth.parse(requestDto.getMonth());
            startDate = yearMonth.atDay(1).atStartOfDay();
            endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        } else if (requestDto.getFromDate() != null && requestDto.getToDate() != null) {
            startDate = LocalDate.parse(requestDto.getFromDate()).atStartOfDay();
            endDate = LocalDate.parse(requestDto.getToDate()).atTime(23, 59, 59);
        } else {
            // Default to current month
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1).atStartOfDay();
            endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        }

        // Fetch approved endorsements for the organization and date range
        List<Endorsement> endorsements = endorsementRepository.findByOrganizationAndDateRange(
            requestDto.getCompanyId(),
            AccountStatus.APPROVED,
            startDate,
            endDate
        );

        // Apply status filter if provided
        if (requestDto.getStatusFilters() != null && !requestDto.getStatusFilters().isEmpty()) {
            List<AccountStatus> statuses = requestDto.getStatusFilters().stream()
                .map(AccountStatus::fromValue)
                .collect(Collectors.toList());
            endorsements = endorsements.stream()
                .filter(e -> statuses.contains(e.getStatus()))
                .collect(Collectors.toList());
        }

        // Fetch organization details
        Organization organization = organizationRepository.findById(requestDto.getCompanyId()).orElse(null);
        String organizationName = organization != null ? organization.getOrganizationName() : "";

        // Map to export rows
        List<ReportExportRowDto> rows = endorsements.stream()
            .map(endorsement -> mapEndorsementToRow(endorsement, organizationName))
            .collect(Collectors.toList());

        logger.info("Enrollment report - Total records: {}", rows.size());
        return createExcelWorkbook("Enrollment Report", ENROLLMENT_HEADERS, rows, ReportType.ENROLLMENT);
    }

    /**
     * Generate payroll report - Active members with premium amount
     */
    private ByteArrayInputStream generatePayrollReport(ReportExportRequestDto requestDto) throws IOException {
        logger.info("Generating payroll report for companyId: {}", requestDto.getCompanyId());

        // Fetch active members
        List<AccountStatus> statuses = new ArrayList<>();
        if (requestDto.getStatusFilters() != null && !requestDto.getStatusFilters().isEmpty()) {
            for (String status : requestDto.getStatusFilters()) {
                statuses.add(AccountStatus.fromValue(status));
            }
        } else {
            statuses.add(AccountStatus.ACTIVE);
        }

        List<Deals> members = dealsRepository.findByOrganizationIdAndStatusIn(
            requestDto.getCompanyId(),
            statuses
        );

        // Apply timeline filter if provided
        members = applyTimelineFilter(members, requestDto.getFromDate(), requestDto.getToDate());

        // Fetch organization details
        Organization organization = organizationRepository.findById(requestDto.getCompanyId()).orElse(null);
        String organizationName = organization != null ? organization.getOrganizationName() : "";

        // Fetch policies for all members
        List<UUID> memberIds = members.stream()
            .map(Deals::getIndividualId)
            .collect(Collectors.toList());
        Map<UUID, Policy> policyMap = fetchPoliciesForMembers(memberIds);

        // Apply premium type filter if provided
        if (requestDto.getPremiumType() != null && !requestDto.getPremiumType().isBlank()) {
            // Filter members who have policies with matching premium type
            members = members.stream()
                .filter(member -> {
                    Policy policy = policyMap.get(member.getIndividualId());
                    if (policy != null && policy.getPaymentFrequency() != null) {
                        return policy.getPaymentFrequency().name().equalsIgnoreCase(requestDto.getPremiumType());
                    }
                    return false;
                })
                .collect(Collectors.toList());
        }

        // Map to export DTOs
        List<ReportExportRowDto> rows = members.stream()
            .map(member -> mapDealToPayrollRow(member, organizationName, policyMap))
            .collect(Collectors.toList());

        logger.info("Payroll report - Total records: {}", rows.size());
        return createExcelWorkbook("Payroll Report", PAYROLL_HEADERS, rows, ReportType.PAYROLL);
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

        return members.stream()
            .filter(member -> {
                LocalDateTime createdAt = member.getCreatedAt();
                if (createdAt == null) return true;

                boolean afterStart = finalStartDateTime == null || !createdAt.isBefore(finalStartDateTime);
                boolean beforeEnd = finalEndDateTime == null || !createdAt.isAfter(finalEndDateTime);

                return afterStart && beforeEnd;
            })
            .collect(Collectors.toList());
    }

    /**
     * Fetch policies for a list of member IDs
     */
    private Map<UUID, Policy> fetchPoliciesForMembers(List<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        List<Policy> policies = policyRepository.findByPrimaryIndividualIdIn(memberIds);
        return policies.stream()
            .collect(Collectors.toMap(
                Policy::getPrimaryIndividualId,
                policy -> policy,
                (existing, replacement) -> existing // Keep first in case of duplicates
            ));
    }

    /**
     * Map Deals entity to Master report row DTO
     */
    private ReportExportRowDto mapDealToMasterRow(Deals deal, String organizationName, Map<UUID, Policy> policyMap) {
        Policy policy = policyMap.get(deal.getIndividualId());

        return ReportExportRowDto.builder()
            .employeeNumber(deal.getEmployeeNumber())
            .fullName(deal.getFullName())
            .firstName(deal.getFirstName())
            .lastName(deal.getLastName())
            .dateOfBirth(deal.getDateOfBirth())
            .gender(deal.getGender())
            .relationship(deal.getRelationship())
            .isPrimaryMember(deal.getIsPrimaryMember())
            .email(deal.getEmail())
            .phone(deal.getPhone())
            .address(deal.getAddress())
            .city(deal.getCity())
            .state(deal.getState())
            .pincode(deal.getPincode())
            .panNumber(deal.getPanNumber())
            .aadhaarNumber(deal.getAadhaarNumber())
            .designation(deal.getDesignation())
            .dateOfJoining(deal.getDateOfJoining())
            .dateOfExit(deal.getDateOfExit())
            .organizationId(deal.getOrganization() != null ? deal.getOrganization().getOrganizationId() : null)
            .organizationName(organizationName)
            .status(deal.getStatus() != null ? deal.getStatus().getValue() : null)
            .accountType(deal.getAccountType() != null ? deal.getAccountType().getValue() : null)
            .maritalStatus(deal.getMaritalStatus())
            .sumInsured(deal.getSumInsured() != null ? new BigDecimal(deal.getSumInsured().replace(",", "")) : null)
            .policyNumber(policy != null ? policy.getPolicyNumber() : null)
            .policyStartDate(policy != null ? policy.getStartDate() : null)
            .policyEndDate(policy != null ? policy.getEndDate() : null)
            .build();
    }

    /**
     * Map Deals entity to Payroll report row DTO
     */
    private ReportExportRowDto mapDealToPayrollRow(Deals deal, String organizationName, Map<UUID, Policy> policyMap) {
        Policy policy = policyMap.get(deal.getIndividualId());

        return ReportExportRowDto.builder()
            .employeeNumber(deal.getEmployeeNumber())
            .fullName(deal.getFullName())
            .firstName(deal.getFirstName())
            .lastName(deal.getLastName())
            .dateOfBirth(deal.getDateOfBirth())
            .gender(deal.getGender())
            .relationship(deal.getRelationship())
            .email(deal.getEmail())
            .phone(deal.getPhone())
            .designation(deal.getDesignation())
            .dateOfJoining(deal.getDateOfJoining())
            .organizationId(deal.getOrganization() != null ? deal.getOrganization().getOrganizationId() : null)
            .organizationName(organizationName)
            .status(deal.getStatus() != null ? deal.getStatus().getValue() : null)
            .premiumAmount(policy != null ? policy.getPremiumAmount() : null)
            .sumInsured(policy != null ? policy.getSumInsured() : null)
            .policyNumber(policy != null ? policy.getPolicyNumber() : null)
            .policyStartDate(policy != null ? policy.getStartDate() : null)
            .policyEndDate(policy != null ? policy.getEndDate() : null)
            .build();
    }

    /**
     * Map Endorsement entity to export row DTO
     */
    private ReportExportRowDto mapEndorsementToRow(Endorsement endorsement, String organizationName) {
        return ReportExportRowDto.builder()
            .endorsementId(endorsement.getEndorsementId())
            .endorsementType(endorsement.getEndorsementType() != null ? endorsement.getEndorsementType().getValue() : null)
            .endorsementStatus(endorsement.getStatus() != null ? endorsement.getStatus().getValue() : null)
            .totalEmployees(endorsement.getTotalEmployees())
            .totalDependents(endorsement.getTotalDependents())
            .approvedAt(endorsement.getApprovedAt() != null ? endorsement.getApprovedAt().toLocalDate() : null)
            .approvedBy(endorsement.getApprovedBy())
            .organizationId(endorsement.getOrganization() != null ? endorsement.getOrganization().getOrganizationId() : null)
            .organizationName(organizationName)
            .premiumAmount(endorsement.getPremiumAmount())
            .premiumChangeType(endorsement.getPremiumChangeType() != null ? endorsement.getPremiumChangeType().getValue() : null)
            .insurerRefNumber(endorsement.getInsurerRefNumber())
            .createdAt(endorsement.getCreatedAt())
            .build();
    }

    /**
     * Create Excel workbook with the given data
     */
    private ByteArrayInputStream createExcelWorkbook(String sheetName, String[] headers,
            List<ReportExportRowDto> rows, ReportType reportType) throws IOException {

        // Use SXSSFWorkbook for streaming to handle large datasets
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
            case MASTER:
                row.createCell(col++).setCellValue(nullSafe(data.getEmployeeNumber()));
                row.createCell(col++).setCellValue(nullSafe(data.getFullName()));
                row.createCell(col++).setCellValue(nullSafe(data.getFirstName()));
                row.createCell(col++).setCellValue(nullSafe(data.getLastName()));
                row.createCell(col++).setCellValue(formatDate(data.getDateOfBirth(), dateFormatter));
                row.createCell(col++).setCellValue(nullSafe(data.getGender()));
                row.createCell(col++).setCellValue(nullSafe(data.getRelationship()));
                row.createCell(col++).setCellValue(data.getIsPrimaryMember() != null ? data.getIsPrimaryMember().toString() : "");
                row.createCell(col++).setCellValue(nullSafe(data.getEmail()));
                row.createCell(col++).setCellValue(nullSafe(data.getPhone()));
                row.createCell(col++).setCellValue(nullSafe(data.getAddress()));
                row.createCell(col++).setCellValue(nullSafe(data.getCity()));
                row.createCell(col++).setCellValue(nullSafe(data.getState()));
                row.createCell(col++).setCellValue(nullSafe(data.getPincode()));
                row.createCell(col++).setCellValue(nullSafe(data.getPanNumber()));
                row.createCell(col++).setCellValue(nullSafe(data.getAadhaarNumber()));
                row.createCell(col++).setCellValue(nullSafe(data.getDesignation()));
                row.createCell(col++).setCellValue(formatDate(data.getDateOfJoining(), dateFormatter));
                row.createCell(col++).setCellValue(formatDate(data.getDateOfExit(), dateFormatter));
                row.createCell(col++).setCellValue(data.getOrganizationId() != null ? data.getOrganizationId().toString() : "");
                row.createCell(col++).setCellValue(nullSafe(data.getOrganizationName()));
                row.createCell(col++).setCellValue(nullSafe(data.getStatus()));
                row.createCell(col++).setCellValue(nullSafe(data.getAccountType()));
                row.createCell(col++).setCellValue(nullSafe(data.getMaritalStatus()));
                row.createCell(col++).setCellValue(data.getSumInsured() != null ? data.getSumInsured().toString() : "");
                row.createCell(col++).setCellValue(nullSafe(data.getPolicyNumber()));
                row.createCell(col++).setCellValue(formatDate(data.getPolicyStartDate(), dateFormatter));
                row.createCell(col++).setCellValue(formatDate(data.getPolicyEndDate(), dateFormatter));
                break;

            case ENROLLMENT:
                row.createCell(col++).setCellValue(data.getEndorsementId() != null ? data.getEndorsementId().toString() : "");
                row.createCell(col++).setCellValue(nullSafe(data.getEndorsementType()));
                row.createCell(col++).setCellValue(nullSafe(data.getEndorsementStatus()));
                row.createCell(col++).setCellValue(data.getTotalEmployees() != null ? data.getTotalEmployees() : 0);
                row.createCell(col++).setCellValue(data.getTotalDependents() != null ? data.getTotalDependents() : 0);
                row.createCell(col++).setCellValue(formatDate(data.getApprovedAt(), dateFormatter));
                row.createCell(col++).setCellValue(nullSafe(data.getApprovedBy()));
                row.createCell(col++).setCellValue(data.getOrganizationId() != null ? data.getOrganizationId().toString() : "");
                row.createCell(col++).setCellValue(nullSafe(data.getOrganizationName()));
                row.createCell(col++).setCellValue(data.getPremiumAmount() != null ? data.getPremiumAmount().doubleValue() : 0.0);
                row.createCell(col++).setCellValue(nullSafe(data.getPremiumChangeType()));
                row.createCell(col++).setCellValue(nullSafe(data.getInsurerRefNumber()));
                row.createCell(col++).setCellValue(formatDateTime(data.getCreatedAt(), dateFormatter));
                break;

            case PAYROLL:
                row.createCell(col++).setCellValue(nullSafe(data.getEmployeeNumber()));
                row.createCell(col++).setCellValue(nullSafe(data.getFullName()));
                row.createCell(col++).setCellValue(nullSafe(data.getFirstName()));
                row.createCell(col++).setCellValue(nullSafe(data.getLastName()));
                row.createCell(col++).setCellValue(formatDate(data.getDateOfBirth(), dateFormatter));
                row.createCell(col++).setCellValue(nullSafe(data.getGender()));
                row.createCell(col++).setCellValue(nullSafe(data.getRelationship()));
                row.createCell(col++).setCellValue(nullSafe(data.getEmail()));
                row.createCell(col++).setCellValue(nullSafe(data.getPhone()));
                row.createCell(col++).setCellValue(nullSafe(data.getDesignation()));
                row.createCell(col++).setCellValue(formatDate(data.getDateOfJoining(), dateFormatter));
                row.createCell(col++).setCellValue(data.getOrganizationId() != null ? data.getOrganizationId().toString() : "");
                row.createCell(col++).setCellValue(nullSafe(data.getOrganizationName()));
                row.createCell(col++).setCellValue(nullSafe(data.getStatus()));
                row.createCell(col++).setCellValue(data.getPremiumAmount() != null ? data.getPremiumAmount().doubleValue() : 0.0);
                row.createCell(col++).setCellValue(data.getSumInsured() != null ? data.getSumInsured().doubleValue() : 0.0);
                row.createCell(col++).setCellValue(nullSafe(data.getPolicyNumber()));
                row.createCell(col++).setCellValue(formatDate(data.getPolicyStartDate(), dateFormatter));
                row.createCell(col++).setCellValue(formatDate(data.getPolicyEndDate(), dateFormatter));
                break;
        }
    }

    /**
     * Generate file name for the export
     */
    private String generateFileName(String prefix, ReportExportRequestDto requestDto) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
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

