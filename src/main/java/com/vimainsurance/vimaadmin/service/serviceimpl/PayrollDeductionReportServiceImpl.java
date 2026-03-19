package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.PayrollDeductionSchedule;
import com.vimainsurance.vimaadmin.enums.ReportFormat;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IPayrollDeductionScheduleRepository;
import com.vimainsurance.vimaadmin.service.IPayrollDeductionReportService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PayrollDeductionReportServiceImpl implements IPayrollDeductionReportService {

    private static final String[] REPORT_HEADERS = {
        "Employee ID", "Name", "Department", "Grade", "Product", "Coverage Amount",
        "Total Annual Premium", "Employer Share", "Employee Share", "Deduction Frequency",
        "Deduction Amount Per Period", "Effective Start Date", "Status", "Corrections"
    };
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int SXSSF_ROW_ACCESS_WINDOW = 200;

    @Autowired
    private IPayrollDeductionScheduleRepository payrollDeductionScheduleRepository;
    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;
    @Autowired
    private IDealsRepository dealsRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReport(UUID companyId, UUID windowId, ReportFormat format) {
        List<PayrollDeductionSchedule> rows = fetchSchedules(companyId, windowId);
        Set<UUID> employeeIds = rows.stream().map(PayrollDeductionSchedule::getEmployeeId).collect(Collectors.toSet());
        Map<UUID, Deals> employeeMap = employeeIds.isEmpty()
                ? Map.of()
                : dealsRepository.findByIndividualIdIn(new ArrayList<>(employeeIds))
                        .stream().collect(Collectors.toMap(Deals::getIndividualId, d -> d, (a, b) -> a));

        if (ReportFormat.XLSX == format) {
            return buildExcel(rows, employeeMap);
        }
        return buildCsv(rows, employeeMap);
    }

    private List<PayrollDeductionSchedule> fetchSchedules(UUID companyId, UUID windowId) {
        if (windowId != null) {
            List<UUID> submissionIds = enrollmentSubmissionRepository.findAllByEnrollmentWindow_Id(windowId)
                    .stream().map(s -> s.getId()).collect(Collectors.toList());
            if (submissionIds.isEmpty()) {
                return List.of();
            }
            return payrollDeductionScheduleRepository.findByOrganizationIdAndEnrollmentSubmissionIdIn(companyId, submissionIds);
        }
        return payrollDeductionScheduleRepository.findByOrganizationIdOrderByEmployeeIdAscEffectiveFromDesc(companyId);
    }

    private byte[] buildExcel(List<PayrollDeductionSchedule> rows, Map<UUID, Deals> employeeMap) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(SXSSF_ROW_ACCESS_WINDOW)) {
            Sheet sheet = workbook.createSheet("Payroll Deductions");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle summaryStyle = createSummaryStyle(workbook);

            int rowNum = 0;
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < REPORT_HEADERS.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(REPORT_HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            BigDecimal totalEmployer = BigDecimal.ZERO;
            BigDecimal totalEmployee = BigDecimal.ZERO;
            Map<String, BigDecimal> byFrequency = new LinkedHashMap<>();

            for (PayrollDeductionSchedule p : rows) {
                Row dataRow = sheet.createRow(rowNum++);
                Deals emp = employeeMap.get(p.getEmployeeId());
                String empId = emp != null && emp.getEmployeeNumber() != null ? emp.getEmployeeNumber() : p.getEmployeeId().toString();
                String name = emp != null ? nullSafe(emp.getFullName()) : "";
                String dept = emp != null ? nullSafe(emp.getDepartment()) : "";
                String grade = emp != null ? nullSafe(emp.getDesignation()) : "";
                String status = p.getEffectiveTo() != null ? "DEPARTED" : "ACTIVE";
                String corrections = "Regular";

                dataRow.createCell(0).setCellValue(empId);
                dataRow.createCell(1).setCellValue(name);
                dataRow.createCell(2).setCellValue(dept);
                dataRow.createCell(3).setCellValue(grade);
                dataRow.createCell(4).setCellValue(nullSafe(p.getProductType()));
                if (p.getCoverageAmount() != null) {
                    dataRow.createCell(5).setCellValue(p.getCoverageAmount().doubleValue());
                } else {
                    dataRow.createCell(5).setCellValue("");
                }
                dataRow.createCell(6).setCellValue(p.getTotalPremiumAnnual() != null ? p.getTotalPremiumAnnual().doubleValue() : 0);
                dataRow.createCell(7).setCellValue(p.getEmployerShareAnnual() != null ? p.getEmployerShareAnnual().doubleValue() : 0);
                dataRow.createCell(8).setCellValue(p.getEmployeeShareAnnual() != null ? p.getEmployeeShareAnnual().doubleValue() : 0);
                dataRow.createCell(9).setCellValue(nullSafe(p.getDeductionFrequency()));
                dataRow.createCell(10).setCellValue(p.getDeductionAmountPerPeriod() != null ? p.getDeductionAmountPerPeriod().doubleValue() : 0);
                dataRow.createCell(11).setCellValue(p.getEffectiveFrom() != null ? p.getEffectiveFrom().format(DATE_FORMAT) : "");
                dataRow.createCell(12).setCellValue(status);
                dataRow.createCell(13).setCellValue(corrections);

                if (p.getEmployerShareAnnual() != null) totalEmployer = totalEmployer.add(p.getEmployerShareAnnual());
                if (p.getEmployeeShareAnnual() != null) totalEmployee = totalEmployee.add(p.getEmployeeShareAnnual());
                String freq = p.getDeductionFrequency() != null ? p.getDeductionFrequency() : "MONTHLY";
                byFrequency.merge(freq, p.getEmployeeShareAnnual() != null ? p.getEmployeeShareAnnual() : BigDecimal.ZERO, BigDecimal::add);
            }

            long employeeCount = rows.stream().map(PayrollDeductionSchedule::getEmployeeId).distinct().count();

            rowNum++;
            Row sumRow1 = sheet.createRow(rowNum++);
            sumRow1.createCell(0).setCellValue("Summary");
            sumRow1.getCell(0).setCellStyle(summaryStyle);
            rowNum++;
            createSummaryRow(sheet, rowNum++, "Total Employer Cost", totalEmployer, summaryStyle);
            createSummaryRow(sheet, rowNum++, "Total Employee Cost", totalEmployee, summaryStyle);
            createSummaryRow(sheet, rowNum++, "Employee Count", BigDecimal.valueOf(employeeCount), summaryStyle);
            for (Map.Entry<String, BigDecimal> e : byFrequency.entrySet()) {
                createSummaryRow(sheet, rowNum++, "By Frequency (" + e.getKey() + ") - Employee Share", e.getValue(), summaryStyle);
            }

            for (int i = 0; i < REPORT_HEADERS.length; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("buildExcel failed", e);
            throw new RuntimeException("Failed to generate payroll report", e);
        }
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle style) {
        Row r = sheet.createRow(rowNum);
        r.createCell(0).setCellValue(label);
        r.createCell(1).setCellValue(value != null ? value.doubleValue() : 0);
        if (style != null) {
            r.getCell(0).setCellStyle(style);
            r.getCell(1).setCellStyle(style);
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        Font f = wb.createFont();
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setBold(true);
        s.setFont(f);
        return s;
    }

    private CellStyle createSummaryStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFont(wb.createFont());
        return s;
    }

    private byte[] buildCsv(List<PayrollDeductionSchedule> rows, Map<UUID, Deals> employeeMap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            w.write(String.join(",", REPORT_HEADERS));
            w.write("\n");
            BigDecimal totalEmployer = BigDecimal.ZERO;
            BigDecimal totalEmployee = BigDecimal.ZERO;
            Map<String, BigDecimal> byFrequency = new LinkedHashMap<>();

            for (PayrollDeductionSchedule p : rows) {
                Deals emp = employeeMap.get(p.getEmployeeId());
                String empId = emp != null && emp.getEmployeeNumber() != null ? emp.getEmployeeNumber() : p.getEmployeeId().toString();
                String name = emp != null ? escapeCsv(nullSafe(emp.getFullName())) : "";
                String dept = emp != null ? escapeCsv(nullSafe(emp.getDepartment())) : "";
                String grade = emp != null ? escapeCsv(nullSafe(emp.getDesignation())) : "";
                String status = p.getEffectiveTo() != null ? "DEPARTED" : "ACTIVE";
                String line = String.join(",",
                    escapeCsv(empId), name, dept, grade,
                    escapeCsv(nullSafe(p.getProductType())),
                    p.getCoverageAmount() != null ? p.getCoverageAmount().toPlainString() : "",
                    p.getTotalPremiumAnnual() != null ? p.getTotalPremiumAnnual().toPlainString() : "",
                    p.getEmployerShareAnnual() != null ? p.getEmployerShareAnnual().toPlainString() : "",
                    p.getEmployeeShareAnnual() != null ? p.getEmployeeShareAnnual().toPlainString() : "",
                    escapeCsv(nullSafe(p.getDeductionFrequency())),
                    p.getDeductionAmountPerPeriod() != null ? p.getDeductionAmountPerPeriod().toPlainString() : "",
                    p.getEffectiveFrom() != null ? p.getEffectiveFrom().format(DATE_FORMAT) : "",
                    status, "Regular");
                w.write(line);
                w.write("\n");
                if (p.getEmployerShareAnnual() != null) totalEmployer = totalEmployer.add(p.getEmployerShareAnnual());
                if (p.getEmployeeShareAnnual() != null) totalEmployee = totalEmployee.add(p.getEmployeeShareAnnual());
                String freq = p.getDeductionFrequency() != null ? p.getDeductionFrequency() : "MONTHLY";
                byFrequency.merge(freq, p.getEmployeeShareAnnual() != null ? p.getEmployeeShareAnnual() : BigDecimal.ZERO, BigDecimal::add);
            }

            long employeeCount = rows.stream().map(PayrollDeductionSchedule::getEmployeeId).distinct().count();
            w.write("\n");
            w.write("Summary\n");
            w.write("Total Employer Cost," + totalEmployer.toPlainString() + "\n");
            w.write("Total Employee Cost," + totalEmployee.toPlainString() + "\n");
            w.write("Employee Count," + employeeCount + "\n");
            for (Map.Entry<String, BigDecimal> e : byFrequency.entrySet()) {
                w.write("By Frequency (" + e.getKey() + ") - Employee Share," + e.getValue().toPlainString() + "\n");
            }
            w.flush();
        } catch (IOException e) {
            log.error("buildCsv failed", e);
            throw new RuntimeException("Failed to generate CSV report", e);
        }
        return out.toByteArray();
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
