package com.vimainsurance.vimaadmin.util;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.vimainsurance.vimaadmin.dto.SelfEmployeeEnrollmentRequestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parses enrollment bulk upload CSV/Excel file.
 * - Source of truth: file. Supports relationship column (SELF vs dependents).
 * - SELF: 3 mandatory (employeeId, name, email); dateOfBirth optional.
 * - Son/Daughter/Child normalized to CHILD1, CHILD2, CHILD3, CHILD4 by order per employee.
 * - Duplicate email enforced only among SELF rows.
 */
public final class EnrollmentUploadParserUtil {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentUploadParserUtil.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<String> CHILD_KEYWORDS = Set.of("son", "daughter", "child");
    private static final String[] CHILD_RELATIONSHIPS = { "CHILD1", "CHILD2", "CHILD3", "CHILD4" };

    /** Lowercase English month name / abbreviation → month number (1–12). */
    private static final Map<String, Integer> MONTH_NAME_TO_INDEX = buildMonthNameIndexMap();

    private static Map<String, Integer> buildMonthNameIndexMap() {
        Map<String, Integer> m = new HashMap<>();
        m.put("jan", 1);
        m.put("january", 1);
        m.put("feb", 2);
        m.put("february", 2);
        m.put("mar", 3);
        m.put("march", 3);
        m.put("apr", 4);
        m.put("april", 4);
        m.put("may", 5);
        m.put("jun", 6);
        m.put("june", 6);
        m.put("jul", 7);
        m.put("july", 7);
        m.put("aug", 8);
        m.put("august", 8);
        m.put("sep", 9);
        m.put("sept", 9);
        m.put("september", 9);
        m.put("oct", 10);
        m.put("october", 10);
        m.put("nov", 11);
        m.put("november", 11);
        m.put("dec", 12);
        m.put("december", 12);
        return m;
    }

    private EnrollmentUploadParserUtil() {}

    /**
     * Parse the upload file. Supports CSV and XLSX.
     * If no relationship column or all rows are treated as SELF (backward compatibility).
     */
    public static EnrollmentParseResult parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            EnrollmentParseResult empty = new EnrollmentParseResult();
            empty.getErrors().add("File is empty or missing");
            return empty;
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            name = "";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return parseExcel(file);
        }
        return parseCsv(file);
    }

    private static EnrollmentParseResult parseCsv(MultipartFile file) {
        EnrollmentParseResult result = new EnrollmentParseResult();
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {
            String[] headerRow = csvReader.readNext();
            if (headerRow == null) {
                result.getErrors().add("Invalid CSV: header row missing");
                return result;
            }
            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            List<String[]> dataRows = csvReader.readAll();
            parseRows(headerMap, dataRows, 2, result);
        } catch (CsvException e) {
            result.getErrors().add("CSV parse error: " + e.getMessage());
            logger.warn("CSV parse error", e);
        } catch (Exception e) {
            result.getErrors().add("File read error: " + e.getMessage());
            logger.warn("File read error", e);
        }
        return result;
    }

    private static EnrollmentParseResult parseExcel(MultipartFile file) {
        EnrollmentParseResult result = new EnrollmentParseResult();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.getErrors().add("Excel sheet missing");
                return result;
            }
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.getErrors().add("Invalid Excel: header row missing");
                return result;
            }
            Map<String, Integer> headerMap = new LinkedHashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String cell = getCellString(headerRow.getCell(i));
                if (cell != null && !cell.isBlank()) {
                    headerMap.put(normalizeHeader(cell), i);
                }
            }
            int startRow = 1;
            for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String[] values = new String[headerRow.getLastCellNum()];
                for (int c = 0; c < values.length; c++) {
                    values[c] = getCellString(row.getCell(c));
                }
                parseRow(headerMap, values, r + 2, result);
            }
        } catch (Exception e) {
            result.getErrors().add("Excel read error: " + e.getMessage());
            logger.warn("Excel read error", e);
        }
        return result;
    }

    private static void parseRows(Map<String, Integer> headerMap, List<String[]> dataRows, int startRowNum, EnrollmentParseResult result) {
        for (int i = 0; i < dataRows.size(); i++) {
            String[] row = dataRows.get(i);
            int rowNum = startRowNum + i;
            parseRow(headerMap, row, rowNum, result);
        }
    }

    private static void parseRow(Map<String, Integer> headerMap, String[] row, int rowNumber, EnrollmentParseResult result) {
        String employeeId = getVal(row, headerMap, "employee_id", "employeeid", "employee id");
        String relationship = getVal(row, headerMap, "relationship");
        String name = getVal(row, headerMap, "name", "employee name", "fullname", "full name");
        String email = getVal(row, headerMap, "email", "email address");
        String dateOfBirth = getVal(row, headerMap, "date_of_birth", "dateofbirth", "dob", "date of birth");
        String gender = getVal(row, headerMap, "gender");
        String phone = getVal(row, headerMap, "phone", "mobile", "contact");
        String dateOfJoining = getVal(row, headerMap, "date_of_joining", "dateofjoining", "doj");
        String designation = getVal(row, headerMap, "designation");
        String department = getVal(row, headerMap, "department");

        if (employeeId == null || employeeId.isBlank()) {
            result.getErrors().add("Row " + rowNumber + ": Employee ID is required");
            return;
        }
        employeeId = employeeId.trim();

        boolean isSelf = isSelfRow(relationship);
        if (isSelf) {
            SelfEmployeeEnrollmentRequestDto dto = new SelfEmployeeEnrollmentRequestDto();
            dto.setEmployeeId(employeeId);
            dto.setName(name != null ? name.trim() : null);
            dto.setEmail(email != null ? email.trim() : null);
            if (dateOfBirth != null && !dateOfBirth.isBlank()) {
                try {
                    dto.setDateOfBirth(LocalDate.parse(dateOfBirth.trim(), DATE_FORMAT));
                } catch (DateTimeParseException e) {
                    String normalized = normalizeDateToIsoString(dateOfBirth.trim());
                    if (normalized != null) {
                        dto.setDateOfBirth(LocalDate.parse(normalized, DATE_FORMAT));
                    }
                }
            }
            dto.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
            dto.setGender(gender != null && !gender.isBlank() ? gender.trim() : null);
            dto.setDesignation(designation != null && !designation.isBlank() ? designation.trim() : null);
            dto.setDepartment(department != null && !department.isBlank() ? department.trim() : null);
            if (dateOfJoining != null && !dateOfJoining.isBlank()) {
                try {
                    dto.setDateOfJoining(LocalDate.parse(dateOfJoining.trim(), DATE_FORMAT));
                } catch (DateTimeParseException e) {
                    String normalized = normalizeDateString(dateOfJoining.trim());
                    if (normalized != null) {
                        dto.setDateOfJoining(LocalDate.parse(normalized, DATE_FORMAT));
                    }
                }
            }
            result.getSelfRows().add(dto);
            result.getRowNumbersByEmployeeId().computeIfAbsent(employeeId, k -> new ArrayList<>()).add(rowNumber);
        } else {
            DependentRow dep = new DependentRow();
            dep.setRowNumber(rowNumber);
            dep.setEmployeeId(employeeId);
            dep.setRelationship(normalizeInLawRelationship(relationship));
            dep.setName(name != null ? name.trim() : null);
            String dobIso = (dateOfBirth != null && !dateOfBirth.isBlank())
                ? normalizeDateToIsoString(dateOfBirth.trim()) : null;
            dep.setDateOfBirth(dobIso != null ? dobIso : (dateOfBirth != null ? dateOfBirth.trim() : null));
            dep.setGender(gender != null ? gender.trim() : null);
            dep.setEmail(email != null && !email.isBlank() ? email.trim() : null);
            result.getDependentRowsByEmployeeId().computeIfAbsent(employeeId, k -> new ArrayList<>()).add(dep);
        }
    }

    /**
     * Normalize Father-in-law/Mother-in-law variants to canonical enum-style values.
     * Accepts: "Mother in law", "MotherInLaw", "MOTHER_IN_LAW", "mother-in-law", etc.
     */
    private static String normalizeInLawRelationship(String relationship) {
        if (relationship == null) return null;
        String compact = relationship
                .trim()
                .replaceAll("[\\s_-]+", "")
                .toUpperCase(Locale.ROOT);

        if ("FATHERINLAW".equals(compact)) return "FATHER_IN_LAW";
        if ("MOTHERINLAW".equals(compact)) return "MOTHER_IN_LAW";

        return relationship.trim();
    }

    private static boolean isSelfRow(String relationship) {
        if (relationship == null || relationship.isBlank()) return true;
        return "self".equals(relationship.trim().toLowerCase(Locale.ROOT))
            || "employee".equals(relationship.trim().toLowerCase(Locale.ROOT));
    }

    /** Normalize Son/Daughter/Child to CHILD1–CHILD4 by order per employee. */
    public static void normalizeChildRelationships(EnrollmentParseResult result) {
        for (Map.Entry<String, List<DependentRow>> entry : result.getDependentRowsByEmployeeId().entrySet()) {
            List<DependentRow> rows = entry.getValue();
            int childIndex = 0;
            for (DependentRow row : rows) {
                String rel = row.getRelationship();
                if (rel != null && CHILD_KEYWORDS.contains(rel.trim().toLowerCase(Locale.ROOT))) {
                    String raw = rel.trim();
                    String lower = raw.toLowerCase(Locale.ROOT);
                    if ("son".equals(lower)) {
                        row.setActualRelationship("Son");
                    } else if ("daughter".equals(lower)) {
                        row.setActualRelationship("Daughter");
                    } else {
                        row.setActualRelationship("Child");
                    }
                    if (childIndex < CHILD_RELATIONSHIPS.length) {
                        row.setRelationship(CHILD_RELATIONSHIPS[childIndex]);
                    } else {
                        row.setRelationship("CHILD4");
                    }
                    childIndex++;
                }
            }
        }
    }

    /**
     * Validate SELF rows: 3 mandatory (employeeId, name, email), at most one SELF per employeeId, unique email among SELF only.
     */
    public static void validateSelfRows(EnrollmentParseResult result) {
        Set<String> seenEmployeeIds = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        for (SelfEmployeeEnrollmentRequestDto dto : result.getSelfRows()) {
            String eid = dto.getEmployeeId();
            if (eid == null || eid.isBlank()) continue;
            eid = eid.trim();
            if (seenEmployeeIds.contains(eid)) {
                result.getErrors().add("Employee " + eid + ": Multiple SELF rows. Only one employee row per Employee ID.");
                continue;
            }
            seenEmployeeIds.add(eid);

            if (dto.getName() == null || dto.getName().isBlank()) {
                result.getErrors().add("Employee " + eid + " (Self): Name is required");
            }
            if (dto.getEmail() == null || dto.getEmail().isBlank()) {
                result.getErrors().add("Employee " + eid + " (Self): Email is required");
            } else {
                String emailLower = dto.getEmail().trim().toLowerCase(Locale.ROOT);
                if (seenEmails.contains(emailLower)) {
                    result.getErrors().add("Duplicate email in employee rows: " + dto.getEmail());
                } else {
                    seenEmails.add(emailLower);
                }
            }
        }
    }

    private static String getVal(String[] row, Map<String, Integer> headerMap, String... keys) {
        for (String key : keys) {
            Integer idx = headerMap.get(normalizeHeader(key));
            if (idx != null && idx < row.length) {
                String cell = row[idx];
                if (cell != null) {
                    String v = cell.trim();
                    if (!v.isEmpty()) return v;
                }
            }
        }
        return null;
    }

    private static String normalizeHeader(String h) {
        if (h == null) return "";
        String t = h.trim();
        // UTF-8 BOM (U+FEFF) on the first cell is common for Excel/Notepad exports; without this,
        // the key becomes "\uFEFFname" and does not match "name", so Name column is ignored.
        while (!t.isEmpty() && t.charAt(0) == '\uFEFF') {
            t = t.substring(1).trim();
        }
        return t.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
    }

    private static Map<String, Integer> buildHeaderMap(String[] headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.length; i++) {
            String h = headerRow[i] != null ? headerRow[i].trim() : "";
            if (!h.isEmpty()) {
                map.put(normalizeHeader(h), i);
            }
        }
        return map;
    }

    private static String getCellString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    try {
                        return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMAT);
                    } catch (Exception e) {
                        return String.valueOf((long) cell.getNumericCellValue());
                    }
                }
                double n = cell.getNumericCellValue();
                if (n == (long) n) return String.valueOf((long) n);
                return String.valueOf(n);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default: return null;
        }
    }

    /**
     * Normalize a date string to ISO (yyyy-MM-dd). Supports:
     * - Already ISO (yyyy-MM-dd)
     * - dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy
     * - dd/MM/yy, dd-MM-yy (2-digit year: 00-29 → 2000-2029, 30-99 → 1930-1999)
     * - dd-MMM-yyyy, dd MMM yyyy, etc. (English month names, case-insensitive; 2- or 4-digit year)
     * Used so enrollment submission JSON (personalDetails, dependents) always stores DOB in a format
     * the frontend can parse (e.g. magic link displays correctly).
     */
    public static String normalizeDateToIsoString(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return s;
        String normalized = normalizeDateString(s);
        if (normalized != null) {
            return normalized;
        }
        normalized = normalizeDateStringTwoDigitYear(s);
        if (normalized != null) {
            return normalized;
        }
        return normalizeDateStringMonthName(s);
    }

    private static String normalizeDateString(String s) {
        if (s == null || s.length() < 8) return null;
        s = s.trim();
        if (s.matches("\\d{4}-\\d{2}-\\d{2}")) return s;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})");
        java.util.regex.Matcher m = p.matcher(s);
        if (m.find()) {
            int d = Integer.parseInt(m.group(1));
            int mo = Integer.parseInt(m.group(2));
            String y = m.group(3);
            return y + "-" + String.format("%02d", mo) + "-" + String.format("%02d", d);
        }
        return null;
    }

    /** dd/MM/yy, dd-MM-yy: 00-29 → 2000-2029, 30-99 → 1930-1999 */
    private static String normalizeDateStringTwoDigitYear(String s) {
        if (s == null || s.length() < 6) return null;
        s = s.trim();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2})");
        java.util.regex.Matcher m = p.matcher(s);
        if (!m.find()) return null;
        int d = Integer.parseInt(m.group(1));
        int mo = Integer.parseInt(m.group(2));
        int yy = Integer.parseInt(m.group(3));
        int yyyy = yy <= 29 ? 2000 + yy : 1900 + yy;
        return yyyy + "-" + String.format("%02d", mo) + "-" + String.format("%02d", d);
    }

    /**
     * e.g. 15-JAN-1980, 15 Jan 1980, 15/Jan/80 — common in insurer CSV exports where month is alphabetic.
     */
    private static String normalizeDateStringMonthName(String s) {
        if (s == null) {
            return null;
        }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "^(\\d{1,2})[\\s./\\-]+([A-Za-z]+)[\\s./\\-]+(\\d{2}|\\d{4})$");
        java.util.regex.Matcher m = p.matcher(s.trim());
        if (!m.matches()) {
            return null;
        }
        Integer mo = MONTH_NAME_TO_INDEX.get(m.group(2).toLowerCase(Locale.ROOT));
        if (mo == null) {
            return null;
        }
        int day = Integer.parseInt(m.group(1));
        String yRaw = m.group(3);
        int year = yRaw.length() == 2
                ? (Integer.parseInt(yRaw) <= 29 ? 2000 + Integer.parseInt(yRaw) : 1900 + Integer.parseInt(yRaw))
                : Integer.parseInt(yRaw);
        if (day < 1 || day > 31) {
            return null;
        }
        try {
            LocalDate ld = LocalDate.of(year, mo, day);
            return ld.format(DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependentRow {
        private int rowNumber;
        private String employeeId;
        private String relationship;
        private String name;
        private String dateOfBirth;
        private String gender;
        private String email;
        /** Original label (Son, Daughter, Child) when relationship is normalized to CHILD1–CHILD4 */
        private String actualRelationship;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentParseResult {
        private List<SelfEmployeeEnrollmentRequestDto> selfRows = new ArrayList<>();
        private Map<String, List<DependentRow>> dependentRowsByEmployeeId = new LinkedHashMap<>();
        private List<String> errors = new ArrayList<>();
        private List<String> dependentErrors = new ArrayList<>();
        private Map<String, List<Integer>> rowNumbersByEmployeeId = new LinkedHashMap<>();

        public boolean hasFatalErrors() {
            return !errors.isEmpty();
        }

        public boolean hasSelfRows() {
            return selfRows != null && !selfRows.isEmpty();
        }
    }
}
