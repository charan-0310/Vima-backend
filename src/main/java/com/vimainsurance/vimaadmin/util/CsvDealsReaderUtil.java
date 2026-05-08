package com.vimainsurance.vimaadmin.util;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.NomineeRelationship;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Utility class for reading CSV files and converting them to Deals entities
 * Optimized for performance and maintainability
 */
public class CsvDealsReaderUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(CsvDealsReaderUtil.class);
    
    // Private constructor to prevent instantiation
    private CsvDealsReaderUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // Pre-compiled patterns for better performance
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern PARENTHESES_PATTERN = Pattern.compile("[()]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    // Email validation pattern: basic email format
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    // Mobile validation pattern: 10 digits (Indian format), optional +91 or 0 prefix
    // Matches: 9876543210, +919876543210, 09876543210
    // Pattern: (optional +91) or (optional 0) followed by 10-digit number starting with 6-9
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^((\\+91)|(0))?[6-9]\\d{9}$");
    
// Column search keys (configurable via application properties)
    private static volatile ColumnHeaderKeys columnHeaderKeys = ColumnHeaderKeys.defaults();

    /**
     * Allows Spring configuration properties to override the default column headers.
     *
     * @param properties injected configuration bean with header names
     */
    public static void configureHeaderKeys(List<String> orderedHeaders) {
        if (orderedHeaders == null || orderedHeaders.isEmpty()) {
            columnHeaderKeys = ColumnHeaderKeys.defaults();
        } else {
            columnHeaderKeys = ColumnHeaderKeys.fromOrderedHeaders(orderedHeaders);
        }
        logger.info("CSV header keys configured: {}", columnHeaderKeys);
    }
    
    /**
     * Reads CSV file and converts rows to Deals entities
     * 
     * @param file MultipartFile containing the CSV
     * @return CsvParseResult containing list of Deals and any errors
     */
    public static CsvParseResult parseCsvToDeals(MultipartFile file) {
        CsvParseResult result = new CsvParseResult();
        List<Deals> dealsList = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRowsRead = 0; // Declare at method scope for access after try block
        
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {
            
            // Read header row first (don't skip it)
            String[] headerRow = csvReader.readNext();
            if (headerRow == null) {
                errors.add("Invalid CSV format: Header row is missing");
                result.setErrors(errors);
                return result;
            }
            
            // Build header map with cleaned keys (optimized)
            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            
            // Log header for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("CSV Header found: {}", String.join(", ", headerRow));
            }
            
            // Find column indices dynamically (optimized batch lookup)
            ColumnIndices indices = findColumnIndices(headerMap);
            
            // Validate critical columns
            if (indices.dateOfJoiningIdx == indices.designationIdx) {
                logger.error("ERROR: Date of Joining index ({}) matches Designation index ({}). Column mapping may be incorrect!", 
                    indices.dateOfJoiningIdx, indices.designationIdx);
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug("Column indices - Employee ID: {}, First Name: {}, Last Name: {}, Email: {}, Phone: {}, Designation: {}, Date of Joining: {}, Status: {}", 
                    indices.empIdIdx, indices.firstNameIdx, indices.lastNameIdx, indices.emailIdx, 
                    indices.phoneIdx, indices.designationIdx, indices.dateOfJoiningIdx, indices.statusIdx);
            }
            
            // Read data rows using streaming to avoid loading entire file into memory
            int rowNumber = 2; // Start from 2 (1 is header, 2 is first data row)
            
            // Process rows one by one instead of loading all into memory
            String[] row;
            while ((row = csvReader.readNext()) != null) {
                totalRowsRead++;
                try {
                    Deals deal = parseRowToDeal(row, indices);
                    if (deal != null) {
                        dealsList.add(deal);
                    } else {
                        errors.add("Row " + rowNumber + ": Failed to parse row - insufficient data");
                    }
                } catch (Exception e) {
                    String errorMsg = "Row " + rowNumber + ": " + e.getMessage();
                    errors.add(errorMsg);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Error parsing row {}: {}", rowNumber, e.getMessage());
                    }
                }
                rowNumber++;
                
                // Log progress for large files (every 1000 rows)
                if (totalRowsRead % 1000 == 0) {
                    logger.debug("Processed {} rows so far", totalRowsRead);
                }
            }
            
        } catch (CsvException e) {
            errors.add("CSV parsing error: " + e.getMessage());
            logger.error("CSV parsing error", e);
        } catch (Exception e) {
            errors.add("File reading error: " + e.getMessage());
            logger.error("Error reading CSV file", e);
        }
        
        result.setDeals(dealsList);
        result.setErrors(errors);
        // ✅ FIX: Exclude header row from total count - only count data rows
        result.setTotalRows(totalRowsRead);
        result.setSuccessCount(dealsList.size());
        result.setErrorCount(errors.size());
        
        return result;
    }
    
    /**
     * Parses CSV file with grouped employee structure (employee_id with Self + dependents)
     * 
     * Expected CSV format:
     * employee_id,relationship,name,date_of_birth,gender,email,mobile,date_of_joining,designation,department,marital_status,sum_insured
     * 
     * - First row for each employee_id: relationship = "Self" (all fields filled)
     * - Subsequent rows: relationship = "Spouse/Child/Father/Mother" (basic fields only)
     * 
     * @param file MultipartFile containing the CSV
     * @return CsvParseResult containing list of Deals and any errors
     */
    public static CsvParseResult parseGroupedCsvToDeals(MultipartFile file) {
        CsvParseResult result = new CsvParseResult();
        List<Deals> dealsList = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalRowsRead = 0; // Declare at method scope for access after try block
        int totalEmployees = 0; // Count of valid employees (Self rows)
        int totalDependents = 0; // Count of valid dependents
        
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {
            
            // Read header row
            String[] headerRow = csvReader.readNext();
            if (headerRow == null) {
                errors.add("Invalid CSV format: Header row is missing");
                result.setErrors(errors);
                return result;
            }
            
            // Build header map
            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            
            // Find column indices
            GroupedColumnIndices indices = findGroupedColumnIndices(headerMap);
            
            // Validate required columns
            if (indices.employeeIdIdx == -1 || indices.relationshipIdx == -1 || 
                indices.nameIdx == -1 || indices.dateOfBirthIdx == -1 || 
                indices.genderIdx == -1 || indices.emailIdx == -1 || indices.mobileIdx == -1) {
                errors.add("Missing required columns. Required: employee_id, relationship, name, date_of_birth, gender, email, mobile");
                result.setErrors(errors);
                return result;
            }
            
            // Read data rows using streaming (optimized for large files)
            int rowNumber = 2; // Start from 2 (1 is header)
            
            // Step 1: Group rows by employee_id (streaming approach)
            Map<String, List<CsvRow>> groupedRows = new LinkedHashMap<>();
            Set<String> allEmployeeIds = new LinkedHashSet<>();
            Set<String> allSelfEmails = new LinkedHashSet<>();
            
            // Read rows one by one instead of loading all into memory
            String[] row;
            while ((row = csvReader.readNext()) != null) {
                totalRowsRead++;
                try {
                    String employeeId = getValue(row, indices.employeeIdIdx);
                    if (employeeId == null || employeeId.trim().isEmpty()) {
                        errors.add("Row " + rowNumber + ": Employee ID is required");
                        rowNumber++;
                        continue;
                    }
                    
                    employeeId = employeeId.trim();
                    allEmployeeIds.add(employeeId);
                    
                    CsvRow csvRow = new CsvRow(rowNumber, row, indices);
                    groupedRows.computeIfAbsent(employeeId, k -> new ArrayList<>()).add(csvRow);
                    
                    // Track Self emails for duplicate check
                    if ("Self".equalsIgnoreCase(csvRow.relationship)) {
                        if (csvRow.email != null && !csvRow.email.trim().isEmpty()) {
                            if (allSelfEmails.contains(csvRow.email.trim().toLowerCase())) {
                                errors.add("Row " + rowNumber + ": Duplicate email '" + csvRow.email + "' found in Self rows");
                            } else {
                                allSelfEmails.add(csvRow.email.trim().toLowerCase());
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": Error parsing row - " + e.getMessage());
                }
                rowNumber++;
                
                // Log progress for large files (every 1000 rows)
                if (totalRowsRead % 1000 == 0) {
                    logger.debug("Processed {} rows, {} employee groups so far", totalRowsRead, groupedRows.size());
                }
            }
            
            logger.info("CSV parsing complete: {} rows processed, {} employee groups found", totalRowsRead, groupedRows.size());
            
            // Step 2: Validate each employee group and create Deals entities
            for (Map.Entry<String, List<CsvRow>> entry : groupedRows.entrySet()) {
                String employeeId = entry.getKey();
                List<CsvRow> rows = entry.getValue();
                
                try {
                    // Validate employee group
                    EmployeeGroupValidation validation = validateEmployeeGroup(employeeId, rows, errors);
                    
                    if (!validation.isValid()) {
                        // Skip this group, errors already added
                        continue;
                    }
                    
                    // Create Self (primary) Deals entity
                    CsvRow selfRow = validation.getSelfRow();
                    Deals primaryDeal = createDealFromSelfRow(selfRow, employeeId);
                    dealsList.add(primaryDeal);
                    totalEmployees++;
                    
                    // Create dependent Deals entities
                    List<CsvRow> dependentRows = validation.getDependentRows();
                    for (CsvRow dependentRow : dependentRows) {
                        Deals dependentDeal = createDealFromDependentRow(dependentRow, primaryDeal);
                        dealsList.add(dependentDeal);
                        totalDependents++;
                    }
                    
                } catch (Exception e) {
                    errors.add("Employee " + employeeId + ": Error processing - " + e.getMessage());
                }
            }
            
        } catch (CsvException e) {
            errors.add("CSV parsing error: " + e.getMessage());
            logger.error("CSV parsing error", e);
        } catch (Exception e) {
            errors.add("File reading error: " + e.getMessage());
            logger.error("Error reading CSV file", e);
        }
        
        result.setDeals(dealsList);
        result.setErrors(errors);
        // ✅ FIX: Exclude header row from total count - only count data rows
        result.setTotalRows(totalRowsRead);
        result.setSuccessCount(dealsList.size());
        result.setErrorCount(errors.size());
        result.setTotalEmployees(totalEmployees);
        result.setTotalDependents(totalDependents);
        
        return result;
    }
    
    /**
     * Validates an employee group (Self + dependents)
     */
    private static EmployeeGroupValidation validateEmployeeGroup(String employeeId, List<CsvRow> rows, List<String> errors) {
        EmployeeGroupValidation validation = new EmployeeGroupValidation();
        
        // Find Self row(s)
        List<CsvRow> selfRows = rows.stream()
            .filter(row -> "Self".equalsIgnoreCase(row.relationship))
            .collect(Collectors.toList());
        
        // Validate Self row exists and is unique
        if (selfRows.isEmpty()) {
            errors.add("Employee " + employeeId + ": No Self row found");
            return validation;
        }
        
        if (selfRows.size() > 1) {
            errors.add("Employee " + employeeId + ": Multiple Self rows found (" + selfRows.size() + ")");
            return validation;
        }
        
        CsvRow selfRow = selfRows.get(0);
        validation.setSelfRow(selfRow);
        
        // Validate Self row has all required fields
        List<String> selfErrors = validateSelfRow(selfRow, employeeId);
        if (!selfErrors.isEmpty()) {
            errors.addAll(selfErrors);
            return validation;
        }
        
        // Get dependent rows
        List<CsvRow> dependentRows = rows.stream()
            .filter(row -> !"Self".equalsIgnoreCase(row.relationship))
            .collect(Collectors.toList());
        
        // Validate dependent rows
        List<String> dependentErrors = validateDependentRows(dependentRows, employeeId, selfRow.email);
        if (!dependentErrors.isEmpty()) {
            errors.addAll(dependentErrors);
            return validation;
        }
        
        validation.setDependentRows(dependentRows);
        validation.setValid(true);
        return validation;
    }
    
    /**
     * Validates Self row (employee) - all fields required
     */
    private static List<String> validateSelfRow(CsvRow row, String employeeId) {
        List<String> errors = new ArrayList<>();
        
        if (row.name == null || row.name.trim().isEmpty()) {
            errors.add("Employee " + employeeId + " (Self): Name is required");
        }
        
        if (row.dateOfBirth == null || row.dateOfBirth.trim().isEmpty()) {
            errors.add("Employee " + employeeId + " (Self): Date of birth is required");
        } else if (!isValidDate(row.dateOfBirth)) {
            errors.add("Employee " + employeeId + " (Self): Invalid date format for date_of_birth. Expected YYYY-MM-DD");
        }
        
        if (row.gender == null || row.gender.trim().isEmpty()) {
            errors.add("Employee " + employeeId + " (Self): Gender is required");
        }
        
        if (row.email == null || row.email.trim().isEmpty()) {
            errors.add("Employee " + employeeId + " (Self): Email is required");
        } else if (!isValidEmail(row.email)) {
            errors.add("Employee " + employeeId + " (Self): Invalid email format");
        }
        
        if (!isMobileAbsentOrPlaceholder(row.mobile) && !isValidMobile(row.mobile)) {
            errors.add("Employee " + employeeId + " (Self): Invalid mobile number format. Expected 10-digit Indian mobile number (e.g., 9876543210)");
        }
        
        if (row.dateOfJoining != null && !row.dateOfJoining.trim().isEmpty() && !isValidDate(row.dateOfJoining)) {
            errors.add("Employee " + employeeId + " (Self): Invalid date format for date_of_joining. Expected YYYY-MM-DD");
        }
        
        // Sum insured optional for bulk CSV (resolved from policy / defaults downstream)
        
        return errors;
    }
    
    /**
     * Validates dependent rows - only basic fields required
     */
    private static List<String> validateDependentRows(List<CsvRow> dependentRows, String employeeId, String selfEmail) {
        List<String> errors = new ArrayList<>();
        int spouseCount = 0;
        
        for (CsvRow row : dependentRows) {
            String relationship = row.relationship;
            
            // Validate relationship
            if (relationship == null || relationship.trim().isEmpty()) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + "): Relationship is required for dependent");
                continue;
            }
            
            String relLower = relationship.trim().toLowerCase();
            if (!relLower.equals("spouse") && !relLower.equals("wife") && !relLower.equals("husband") && !relLower.equals("child") &&
                !relLower.equals("father") && !relLower.equals("mother") && 
                !relLower.equals("parent-in-law")) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + "): Invalid relationship '" + relationship + 
                    "'. Allowed: Spouse, Wife, Husband, Child, Father, Mother, Parent-in-law");
                continue;
            }
            
            // Count spouses
            if (relLower.equals("spouse") || relLower.equals("wife") || relLower.equals("husband")) {
                spouseCount++;
            }
            
            // Validate required fields
            if (row.name == null || row.name.trim().isEmpty()) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Name is required");
            }
            
            if (row.dateOfBirth == null || row.dateOfBirth.trim().isEmpty()) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Date of birth is required");
            } else if (!isValidDate(row.dateOfBirth)) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Invalid date format. Expected YYYY-MM-DD");
            } else {
                // Validate child age - should not be greater than 25
                if (relLower.equals("child")) {
                    LocalDate dob = parseDate(row.dateOfBirth);
                    if (dob != null) {
                        int age = calculateAge(dob);
                        if (age > 25) {
                            errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Child age cannot be greater than 25 years. Current age: " + age);
                        }
                    }
                }
            }
            
            if (row.gender == null || row.gender.trim().isEmpty()) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Gender is required");
            }
            
            // Validate email format if provided (optional for dependents)
            if (row.email != null && !row.email.trim().isEmpty()) {
                if (!isValidEmail(row.email)) {
                    errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Invalid email format");
                }
            }
            
            // Validate mobile format if provided (optional for dependents)
            if (!isMobileAbsentOrPlaceholder(row.mobile) && !isValidMobile(row.mobile)) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Invalid mobile number format. Expected 10-digit Indian mobile number (e.g., 9876543210)");
            }
            
            // Validate dependent fields should be empty
            if (row.dateOfJoining != null && !row.dateOfJoining.trim().isEmpty()) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Date of joining should be empty for dependents");
            }
            
            if (row.designation != null && !row.designation.trim().isEmpty()) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Designation should be empty for dependents");
            }
            
            if (row.sumInsured != null && !row.sumInsured.trim().isEmpty()) {
                errors.add("Employee " + employeeId + " (Row " + row.rowNumber + ", " + relationship + "): Sum insured should be empty for dependents");
            }
        }
        
        // Validate max 1 spouse
        if (spouseCount > 1) {
            errors.add("Employee " + employeeId + ": Multiple Spouse relationships found. Only one spouse allowed per employee");
        }
        
        return errors;
    }
    
    /**
     * Creates Deals entity from Self row (primary employee)
     */
    private static Deals createDealFromSelfRow(CsvRow row, String employeeId) {
        Deals deal = new Deals();
        
        deal.setEmployeeNumber(employeeId);
        deal.setIsPrimaryMember(true);
        deal.setAccountType(AccountType.CORPORATE_EMPLOYEE);
        
        // Check if this is a deletion (date_of_exit is present)
        if (row.dateOfExit != null && !row.dateOfExit.trim().isEmpty()) {
            deal.setStatus(AccountStatus.INACTIVE);
            logger.info("Employee {} marked for deletion. Date of exit: {}, Reason: {}", 
                employeeId, row.dateOfExit, row.deletionReason);
        } else {
            deal.setStatus(AccountStatus.ACTIVE);
        }
        
        // Parse name (assume first name, last name optional)
        String[] nameParts = row.name.trim().split("\\s+", 2);
        deal.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            deal.setLastName(nameParts[1]);
        }
        deal.setFullName(row.name != null ? row.name.trim() : null);
        
        deal.setDateOfBirth(parseDate(row.dateOfBirth));
        deal.setGender(row.gender.trim());
        deal.setEmail(row.email.trim());
        deal.setPhone(normalizePhoneForStorage(row.mobile));
        deal.setDateOfJoining(parseDate(row.dateOfJoining));
        deal.setDesignation(row.designation != null ? row.designation.trim() : null);
        deal.setRelationship("Self");
        deal.setPreferredLanguage("en");
        
        return deal;
    }
    
    /**
     * Creates Deals entity from dependent row
     */
    private static Deals createDealFromDependentRow(CsvRow row, Deals primaryDeal) {
        Deals deal = new Deals();
        
        deal.setEmployeeNumber(primaryDeal.getEmployeeNumber());
        deal.setIsPrimaryMember(false);
        deal.setAccountType(AccountType.CORPORATE_DEPENDENT);
        
        // Check if this dependent has date_of_exit (individual deletion)
        // Note: If primary has date_of_exit, all dependents will be deleted in service layer
        if (row.dateOfExit != null && !row.dateOfExit.trim().isEmpty()) {
            deal.setStatus(AccountStatus.INACTIVE);
            logger.info("Dependent {} ({}) marked for deletion. Date of exit: {}, Reason: {}", 
                row.name, row.relationship, row.dateOfExit, row.deletionReason);
        } else {
            // If primary is being deleted, dependents will be marked INACTIVE in service layer
            // Otherwise, keep dependent active
            deal.setStatus(AccountStatus.ACTIVE);
        }
        
        deal.setPrimaryIndividual(primaryDeal);
        
        // Parse name
        String[] nameParts = row.name.trim().split("\\s+", 2);
        deal.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            deal.setLastName(nameParts[1]);
        }
        deal.setFullName(row.name != null ? row.name.trim() : null);
        
        deal.setDateOfBirth(parseDate(row.dateOfBirth));
        deal.setGender(row.gender.trim());
        String relationship = row.relationship != null ? row.relationship.trim() : "";
        if ("wife".equalsIgnoreCase(relationship) || "husband".equalsIgnoreCase(relationship)
                || "spouse".equalsIgnoreCase(relationship)) {
            relationship = NomineeRelationship.SPOUSE.getValue();
        }
        deal.setRelationship(relationship);
        deal.setEmail(row.email != null && !row.email.trim().isEmpty() ? row.email.trim() : primaryDeal.getEmail());
        // Phone is required - use mobile from CSV or fallback to primary's phone
        deal.setPhone(row.mobile != null && !row.mobile.trim().isEmpty() ? row.mobile.trim() : primaryDeal.getPhone());
        deal.setPreferredLanguage("en");
        
        return deal;
    }
    
    /**
     * Helper class for CSV row data
     */
    private static class CsvRow {
        int rowNumber;
        String employeeId;
        String relationship;
        String name;
        String dateOfBirth;
        String gender;
        String email;
        String mobile;
        String dateOfJoining;
        String designation;
        String department;
        String maritalStatus;
        String sumInsured;
        String dateOfExit;
        String deletionReason;
        String remarks;
        
        CsvRow(int rowNumber, String[] row, GroupedColumnIndices indices) {
            this.rowNumber = rowNumber;
            this.employeeId = getValue(row, indices.employeeIdIdx);
            this.relationship = getValue(row, indices.relationshipIdx);
            this.name = getValue(row, indices.nameIdx);
            this.dateOfBirth = getValue(row, indices.dateOfBirthIdx);
            this.gender = getValue(row, indices.genderIdx);
            this.email = getValue(row, indices.emailIdx);
            this.mobile = getValue(row, indices.mobileIdx);
            this.dateOfJoining = getValue(row, indices.dateOfJoiningIdx);
            this.designation = getValue(row, indices.designationIdx);
            this.department = getValue(row, indices.departmentIdx);
            this.maritalStatus = getValue(row, indices.maritalStatusIdx);
            this.sumInsured = getValue(row, indices.sumInsuredIdx);
            this.dateOfExit = getValue(row, indices.dateOfExitIdx);
            this.deletionReason = getValue(row, indices.deletionReasonIdx);
            this.remarks = getValue(row, indices.remarksIdx);
        }
    }
    
    /**
     * Helper class for employee group validation
     */
    private static class EmployeeGroupValidation {
        private boolean valid = false;
        private CsvRow selfRow;
        private List<CsvRow> dependentRows = new ArrayList<>();
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public CsvRow getSelfRow() { return selfRow; }
        public void setSelfRow(CsvRow selfRow) { this.selfRow = selfRow; }
        public List<CsvRow> getDependentRows() { return dependentRows; }
        public void setDependentRows(List<CsvRow> dependentRows) { this.dependentRows = dependentRows; }
    }
    
    /**
     * Finds column indices for grouped CSV format
     */
    private static GroupedColumnIndices findGroupedColumnIndices(Map<String, Integer> headerMap) {
        GroupedColumnIndices indices = new GroupedColumnIndices();
        
        indices.employeeIdIdx = findColumnIndex(headerMap, "employee_id", -1);
        indices.relationshipIdx = findColumnIndex(headerMap, "relationship", -1);
        indices.nameIdx = findColumnIndex(headerMap, "name", -1);
        indices.dateOfBirthIdx = findColumnIndex(headerMap, "date_of_birth", -1);
        indices.genderIdx = findColumnIndex(headerMap, "gender", -1);
        indices.emailIdx = findColumnIndex(headerMap, "email", -1);
        indices.mobileIdx = findColumnIndex(headerMap, "mobile", -1);
        indices.dateOfJoiningIdx = findColumnIndex(headerMap, "date_of_joining", -1);
        indices.designationIdx = findColumnIndex(headerMap, "designation", -1);
        indices.departmentIdx = findColumnIndex(headerMap, "department", -1);
        indices.maritalStatusIdx = findColumnIndex(headerMap, "marital_status", -1);
        indices.sumInsuredIdx = findColumnIndex(headerMap, "sum_insured", -1);
        indices.dateOfExitIdx = findColumnIndex(headerMap, "date_of_exit", -1);
        indices.deletionReasonIdx = findColumnIndex(headerMap, "deletion_reason", -1);
        indices.remarksIdx = findColumnIndex(headerMap, "remarks", -1);
        
        return indices;
    }
    
    /**
     * Helper class for grouped column indices
     */
    private static class GroupedColumnIndices {
        int employeeIdIdx = -1;
        int relationshipIdx = -1;
        int nameIdx = -1;
        int dateOfBirthIdx = -1;
        int genderIdx = -1;
        int emailIdx = -1;
        int mobileIdx = -1;
        int dateOfJoiningIdx = -1;
        int designationIdx = -1;
        int departmentIdx = -1;
        int maritalStatusIdx = -1;
        int sumInsuredIdx = -1;
        int dateOfExitIdx = -1;
        int deletionReasonIdx = -1;
        int remarksIdx = -1;
    }
    
    /**
     * Validates date format (YYYY-MM-DD)
     */
    private static boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        return DATE_PATTERN.matcher(dateStr.trim()).matches();
    }
    
    /**
     * Parses date string to LocalDate
     */
    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    /**
     * Validates email format
     * Checks for valid email format: user@domain.com
     */
    private static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String trimmedEmail = email.trim().toLowerCase();
        return EMAIL_PATTERN.matcher(trimmedEmail).matches();
    }
    
    /**
     * Validates mobile/phone number format
     * Accepts: 10-digit Indian mobile numbers (starting with 6-9)
     * Optional prefixes: +91 or 0
     * Examples: 9876543210, +919876543210, 09876543210
     */
    private static boolean isMobileAbsentOrPlaceholder(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return true;
        }
        String t = mobile.trim();
        return "-".equals(t)
                || "\u2014".equals(t)
                || "--".equals(t)
                || "NA".equalsIgnoreCase(t)
                || "N/A".equalsIgnoreCase(t);
    }

    /** Store empty string when CSV used "-" / NA instead of a real number. */
    private static String normalizePhoneForStorage(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return "";
        }
        if (isMobileAbsentOrPlaceholder(mobile)) {
            return "";
        }
        return mobile.trim();
    }

    private static boolean isValidMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return false;
        }
        // Remove spaces, hyphens, and parentheses for validation
        String cleaned = mobile.trim().replaceAll("[\\s()-]", "");
        return MOBILE_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * Calculates age from date of birth
     * @param dateOfBirth LocalDate representing date of birth
     * @return age in years, or -1 if dateOfBirth is null or in the future
     */
    private static int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return -1;
        }
        LocalDate today = LocalDate.now();
        if (dateOfBirth.isAfter(today)) {
            return -1; // Invalid date - future date
        }
        return Period.between(dateOfBirth, today).getYears();
    }
    
    /**
     * Builds optimized header map with cleaned keys
     */
    private static Map<String, Integer> buildHeaderMap(String[] headerRow) {
        // Pre-size for performance (2x for cleaned + original keys)
        @SuppressWarnings("java:S1640")
        Map<String, Integer> headerMap = new HashMap<>(headerRow.length * 2);
        for (int i = 0; i < headerRow.length; i++) {
            if (headerRow[i] != null) {
                String cleaned = PARENTHESES_PATTERN.matcher(headerRow[i].trim().toLowerCase()).replaceAll("").trim();
                headerMap.put(cleaned, i);
                // Also store original (trimmed, lowercased) for exact matches
                headerMap.put(headerRow[i].trim().toLowerCase(), i);
            }
        }
        return headerMap;
    }
    
    /**
     * Finds all column indices in one pass (optimized)
     */
    private static ColumnIndices findColumnIndices(Map<String, Integer> headerMap) {
        ColumnIndices indices = new ColumnIndices();
        ColumnHeaderKeys keys = columnHeaderKeys;
        indices.empIdIdx = findColumnIndex(headerMap, keys.employeeId, 0);
        indices.firstNameIdx = findColumnIndex(headerMap, keys.firstName, 1);
        indices.lastNameIdx = findColumnIndex(headerMap, keys.lastName, 2);
        indices.emailIdx = findColumnIndex(headerMap, keys.email, 3);
        indices.phoneIdx = findColumnIndex(headerMap, keys.phone, 4);
        indices.designationIdx = findColumnIndex(headerMap, keys.designation, 6);
        indices.dateOfJoiningIdx = findColumnIndex(headerMap, keys.dateOfJoining, 7);
        indices.statusIdx = findColumnIndex(headerMap, keys.status, 9);
        return indices;
    }
    
    private static int findColumnIndex(Map<String, Integer> headerMap, List<String> searchKeys, int defaultIndex) {
        if (searchKeys != null) {
            for (String key : searchKeys) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                int idx = findColumnIndex(headerMap, key, Integer.MIN_VALUE);
                if (idx != Integer.MIN_VALUE) {
                    return idx;
                }
            }
        }
        return defaultIndex;
    }

    /**
     * Finds column index by header name (optimized matching)
     */
    private static int findColumnIndex(Map<String, Integer> headerMap, String searchKey, int defaultIndex) {
        String lowerKey = searchKey.toLowerCase();
        
        // Try exact match first (fastest)
        Integer exactMatch = headerMap.get(lowerKey);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // Try cleaned key match
        String cleanKey = PARENTHESES_PATTERN.matcher(lowerKey).replaceAll("").trim();
        Integer cleanMatch = headerMap.get(cleanKey);
        if (cleanMatch != null) {
            return cleanMatch;
        }
        
        // For multi-word keys, match all words
        String[] searchWords = WHITESPACE_PATTERN.split(lowerKey);
        if (searchWords.length > 1) {
            int bestMatch = -1;
            int bestScore = 0;
            
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                String header = entry.getKey();
                boolean allMatch = true;
                int score = 0;
                
                for (String word : searchWords) {
                    if (header.contains(word)) {
                        score += word.length();
                    } else {
                        allMatch = false;
                        break;
                    }
                }
                
                if (allMatch && score > bestScore) {
                    bestMatch = entry.getValue();
                    bestScore = score;
                }
            }
            
            if (bestMatch != -1) {
                return bestMatch;
            }
        }
        
        // Fallback: simple contains
        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            if (entry.getKey().contains(cleanKey) || cleanKey.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return defaultIndex;
    }

    private static final class ColumnHeaderKeys {
        private final List<String> employeeId;
        private final List<String> firstName;
        private final List<String> lastName;
        private final List<String> email;
        private final List<String> phone;
        private final List<String> designation;
        private final List<String> dateOfJoining;
        private final List<String> status;

        private ColumnHeaderKeys(List<String> employeeId,
                                List<String> firstName,
                                List<String> lastName,
                                List<String> email,
                                List<String> phone,
                                List<String> designation,
                                List<String> dateOfJoining,
                                List<String> status) {
            this.employeeId = employeeId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
            this.designation = designation;
            this.dateOfJoining = dateOfJoining;
            this.status = status;
        }

        private static ColumnHeaderKeys defaults() {
            return new ColumnHeaderKeys(
                    List.of("employee id"),
                    List.of("first name"),
                    List.of("last name"),
                    List.of("email"),
                    List.of("phone"),
                    List.of("designation"),
                    List.of("date of joining"),
                    List.of("status")
            );
        }

        private static ColumnHeaderKeys fromOrderedHeaders(List<String> orderedHeaders) {
            List<String> sanitized = sanitizeOrderedHeaders(orderedHeaders);
            return new ColumnHeaderKeys(
                    aliasesAtIndex(sanitized, 0, "employee id"),
                    aliasesAtIndex(sanitized, 1, "first name"),
                    aliasesAtIndex(sanitized, 2, "last name"),
                    aliasesAtIndex(sanitized, 3, "email"),
                    aliasesAtIndex(sanitized, 4, "phone"),
                    aliasesAtIndex(sanitized, 5, "designation"),
                    aliasesAtIndex(sanitized, 6, "date of joining"),
                    aliasesAtIndex(sanitized, 7, "status")
            );
        }

        private static List<String> sanitizeOrderedHeaders(List<String> orderedHeaders) {
            List<String> result = new ArrayList<>();
            for (String value : orderedHeaders) {
                if (value != null && !value.isBlank()) {
                    result.add(value.trim().toLowerCase());
                }
            }
            return result;
        }

        private static List<String> aliasesAtIndex(List<String> headers, int index, String defaultValue) {
            if (headers.size() > index) {
                return List.of(headers.get(index));
            }
            return List.of(defaultValue);
        }

        @Override
        public String toString() {
            return "ColumnHeaderKeys{" +
                    "employeeId=" + employeeId +
                    ", firstName=" + firstName +
                    ", lastName=" + lastName +
                    ", email=" + email +
                    ", phone=" + phone +
                    ", designation=" + designation +
                    ", dateOfJoining=" + dateOfJoining +
                    ", status=" + status +
                    '}';
        }
    }
    
    /**
     * Holds column indices for better code organization
     */
    private static class ColumnIndices {
        int empIdIdx;
        int firstNameIdx;
        int lastNameIdx;
        int emailIdx;
        int phoneIdx;
        int designationIdx;
        int dateOfJoiningIdx;
        int statusIdx;
    }
    
    /**
     * Parses a single CSV row to Deals entity (optimized)
     */
    private static Deals parseRowToDeal(String[] row, ColumnIndices indices) throws Exception {
        Deals deal = new Deals();
        
        // Employee ID (required)
        String employeeId = getValue(row, indices.empIdIdx);
        if (employeeId == null || employeeId.isEmpty()) {
            throw new Exception("Employee ID is required");
        }
        deal.setEmployeeNumber(employeeId);
        
        // First Name (required)
        String firstName = getValue(row, indices.firstNameIdx);
        if (firstName == null || firstName.isEmpty()) {
            throw new Exception("First Name is required");
        }
        deal.setFirstName(firstName);
        
        // Last Name (optional)
        String lastName = getValue(row, indices.lastNameIdx);
        if (lastName != null && !lastName.isEmpty()) {
            deal.setLastName(lastName);
        }
        
        // Email (optional)
        String email = getValue(row, indices.emailIdx);
        if (email != null && !email.isEmpty()) {
            if (!isValidEmail(email)) {
                throw new Exception("Invalid email format");
            }
            deal.setEmail(email);
        }
        
        // Phone (required)
        String phone = getValue(row, indices.phoneIdx);
        if (phone == null || phone.isEmpty()) {
            throw new Exception("Phone is required");
        }
        if (!isValidMobile(phone)) {
            throw new Exception("Invalid mobile number format. Expected 10-digit Indian mobile number (e.g., 9876543210)");
        }
        deal.setPhone(phone);
        
        // Designation (optional)
        String designation = getValue(row, indices.designationIdx);
        if (designation != null && !designation.isEmpty()) {
            deal.setDesignation(designation);
        }
        
        // Date of Joining (optional, but validated if present)
        String dateOfJoining = getValue(row, indices.dateOfJoiningIdx);
        if (dateOfJoining != null && !dateOfJoining.isEmpty()) {
            if (!DATE_PATTERN.matcher(dateOfJoining).matches()) {
                throw new Exception("Invalid date format for Date of Joining. Expected YYYY-MM-DD, got: " + dateOfJoining);
            }
            try {
                deal.setDateOfJoining(LocalDate.parse(dateOfJoining, DATE_FORMATTER));
            } catch (DateTimeParseException e) {
                throw new Exception("Invalid date format for Date of Joining. Expected YYYY-MM-DD, got: " + dateOfJoining);
            }
        }
        
        // Status (optional, defaults to ACTIVE)
        String status = getValue(row, indices.statusIdx);
        if (status != null && !status.isEmpty()) {
            String statusUpper = status.toUpperCase();
            if ("ACTIVE".equals(statusUpper)) {
                deal.setStatus(AccountStatus.ACTIVE);
            } else if ("INACTIVE".equals(statusUpper)) {
                deal.setStatus(AccountStatus.INACTIVE);
            } else {
                throw new Exception("Invalid status value. Expected Active or Inactive, got: " + status);
            }
        } else {
            deal.setStatus(AccountStatus.ACTIVE);
        }
        
        // Set default values
        deal.setAccountType(AccountType.CORPORATE_EMPLOYEE);
        deal.setIsPrimaryMember(true);
        deal.setPreferredLanguage("en");
        
        return deal;
    }
    
    /**
     * Gets value from array with null safety (optimized - returns trimmed or null)
     */
    private static String getValue(String[] row, int index) {
        if (index < 0 || index >= row.length) {
            return null;
        }
        String value = row[index];
        if (value == null) {
            return null;
        }
        // Fast trim check - avoid creating new string if not needed
        int len = value.length();
        int start = 0;
        int end = len;
        
        while (start < len && value.charAt(start) <= ' ') {
            start++;
        }
        while (end > start && value.charAt(end - 1) <= ' ') {
            end--;
        }
        
        if (start == 0 && end == len) {
            return value.isEmpty() ? null : value;
        }
        
        String trimmed = (start < end) ? value.substring(start, end) : "";
        return trimmed.isEmpty() ? null : trimmed;
    }
    
    /**
     * Result class for CSV parsing
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CsvParseResult {
        private List<Deals> deals = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
        private int totalRows;
        private int successCount;
        private int errorCount;
        private int totalEmployees;
        private int totalDependents;
        
        // Explicit getters for linter compatibility (Lombok will skip generating these)
        public List<Deals> getDeals() {
            return deals;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public int getTotalRows() {
            return totalRows;
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public int getErrorCount() {
            return errorCount;
        }
        
        public int getTotalEmployees() {
            return totalEmployees;
        }
        
        public int getTotalDependents() {
            return totalDependents;
        }
    }
}

