package com.vimainsurance.vimaadmin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;

/**
 * Standalone main class for importing Deals from CSV file
 * For dev testing - update CSV_FILE_PATH constant with your file path
 * Optimized for performance and maintainability
 * 
 * Usage: Run this class directly or update the CSV_FILE_PATH constant
 * Supports both relative and absolute paths
 */
public class CsvDealsImportMain {

    private static final Logger logger = LoggerFactory.getLogger(CsvDealsImportMain.class);
    
    // Update this constant with your CSV file path (relative or absolute)
    // Examples:
    //   "employees.csv" (relative to project root)
    //   "data/employees.csv" (relative path)
    //   "C:/Users/YourName/Desktop/employees.csv" (Windows absolute path)
    //   "/home/user/data/employees.csv" (Linux/Mac absolute path)
    private static final String CSV_FILE_PATH = "C:\\Users\\CharansundarMoorthy\\Downloads\\employee_upload_template_Linear_Polymer.csv";
    
    // Pre-compiled patterns for better performance
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern PARENTHESES_PATTERN = Pattern.compile("[()]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    // Column search keys (pre-computed for performance)
    private static final String KEY_EMPLOYEE_ID = "employee id";
    private static final String KEY_FIRST_NAME = "first name";
    private static final String KEY_LAST_NAME = "last name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_DESIGNATION = "designation";
    private static final String KEY_DATE_OF_JOINING = "date of joining";
    private static final String KEY_STATUS = "status";
    
    // Note: This requires Spring Boot context to access the repository
    // For standalone execution, you'll need to initialize Spring context
    // Or use the API endpoint instead
    
    public static void main(String[] args) {
        // Use command line argument if provided, otherwise use constant
        String csvPath = (args.length > 0) ? args[0] : CSV_FILE_PATH;
        
        System.out.println("==========================================");
        System.out.println("CSV Deals Import Tool");
        System.out.println("==========================================");
        System.out.println("File path: " + csvPath);
        System.out.println();
        
        // Resolve file path (handles both relative and absolute)
        File csvFile = resolveFile(csvPath);
        
        if (csvFile == null) {
            System.err.println("ERROR: Could not resolve file path: " + csvPath);
            System.exit(1);
        }
        
        // Validate file
        if (!csvFile.exists()) {
            System.err.println("ERROR: CSV file not found: " + csvFile.getAbsolutePath());
            System.err.println("Please check the file path and try again.");
            System.exit(1);
        }
        
        if (!csvFile.canRead()) {
            System.err.println("ERROR: CSV file is not readable: " + csvFile.getAbsolutePath());
            System.exit(1);
        }
        
        if (!csvFile.getName().toLowerCase().endsWith(".csv")) {
            System.err.println("ERROR: File is not a CSV file: " + csvFile.getAbsolutePath());
            System.exit(1);
        }
        
        System.out.println("Reading CSV file: " + csvFile.getAbsolutePath());
        System.out.println();
        
        // Parse CSV
        List<Deals> dealsList = parseCsvFile(csvFile);
        
        if (dealsList.isEmpty()) {
            System.err.println("ERROR: No valid records found in CSV file");
            System.exit(1);
        }
        
        System.out.println("Parsed " + dealsList.size() + " records from CSV");
        System.out.println();
        System.out.println("NOTE: This tool only parses the CSV file.");
        System.out.println("To save to database, use the API endpoint:");
        System.out.println("  POST /api/v1/deals/upload/csv");
        System.out.println();
        System.out.println("Or use Spring Boot CommandLineRunner with:");
        System.out.println("  java -jar app.jar --csv.import.path=" + csvFile.getAbsolutePath());
        System.out.println();
        
        // Print sample of parsed data (optimized string building)
        System.out.println("Sample of parsed records (first 5):");
        System.out.println("----------------------------------------");
        int sampleSize = Math.min(5, dealsList.size());
        for (int i = 0; i < sampleSize; i++) {
            Deals deal = dealsList.get(i);
            StringBuilder sb = new StringBuilder(100);
            sb.append(i + 1).append(". Employee ID: ").append(deal.getEmployeeNumber())
              .append(", Name: ").append(deal.getFirstName());
            if (deal.getLastName() != null && !deal.getLastName().isEmpty()) {
                sb.append(" ").append(deal.getLastName());
            }
            sb.append(", Email: ").append(deal.getEmail() != null ? deal.getEmail() : "N/A")
              .append(", Phone: ").append(deal.getPhone());
            System.out.println(sb.toString());
        }
        if (dealsList.size() > 5) {
            System.out.println("... and " + (dealsList.size() - 5) + " more records");
        }
        System.out.println();
        System.out.println("==========================================");
        System.out.println("Parsing completed successfully!");
        System.out.println("==========================================");
    }
    
    /**
     * Resolves file path (handles both relative and absolute paths)
     */
    private static File resolveFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        
        File file = new File(filePath.trim());
        
        // If absolute path, return as is
        if (file.isAbsolute()) {
            return file;
        }
        
        // If relative path, try to resolve it
        // First try current directory
        File currentDirFile = new File(System.getProperty("user.dir"), filePath);
        if (currentDirFile.exists()) {
            return currentDirFile;
        }
        
        // Try project root (assuming we're in target/classes)
        File projectRoot = new File(System.getProperty("user.dir")).getParentFile();
        if (projectRoot != null) {
            File projectRootFile = new File(projectRoot, filePath);
            if (projectRootFile.exists()) {
                return projectRootFile;
            }
        }
        
        // Return the file even if it doesn't exist yet (validation will catch it)
        return currentDirFile;
    }
    
    /**
     * Parses CSV file and returns list of Deals entities
     */
    private static List<Deals> parseCsvFile(File csvFile) {
        List<Deals> dealsList = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String[]> allRows = null;
        
        try (Reader reader = new InputStreamReader(new FileInputStream(csvFile));
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {
            
            // Read header row first (don't skip it)
            String[] headerRow = csvReader.readNext();
            if (headerRow == null) {
                errors.add("Invalid CSV format: Header row is missing");
                logger.error("Invalid CSV format: Header row is missing");
                printErrors(errors);
                return dealsList;
            }
            
            // Build header map with cleaned keys (optimized)
            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            
            // Log header for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("CSV Header found: {}", String.join(", ", headerRow));
            }
            System.out.println("CSV Header: " + String.join(", ", headerRow));
            System.out.println();
            
            // Find column indices dynamically (optimized batch lookup)
            ColumnIndices indices = findColumnIndices(headerMap);
            
            System.out.println("Column indices detected:");
            System.out.println("  Employee ID: " + indices.empIdIdx);
            System.out.println("  First Name: " + indices.firstNameIdx);
            System.out.println("  Last Name: " + indices.lastNameIdx);
            System.out.println("  Email: " + indices.emailIdx);
            System.out.println("  Phone: " + indices.phoneIdx);
            System.out.println("  Designation: " + indices.designationIdx);
            System.out.println("  Date of Joining: " + indices.dateOfJoiningIdx);
            System.out.println("  Status: " + indices.statusIdx);
            System.out.println();
            
            // Validate critical indices
            if (indices.dateOfJoiningIdx == indices.designationIdx) {
                System.err.println("WARNING: Date of Joining index matches Designation index! This may cause parsing errors.");
            }
            
            // Read data rows
            allRows = csvReader.readAll();
            int rowNumber = 2; // Start from 2 (1 is header, 2 is first data row)
            
            logger.info("Processing {} data rows from CSV file", allRows.size());
            System.out.println("Found " + allRows.size() + " data rows to process");
            System.out.println();
            
            // Pre-allocate list size for better performance
            dealsList = new ArrayList<>(allRows.size());
            
            for (String[] row : allRows) {
                // Debug: Print raw row data (only for first few rows)
                if (rowNumber <= 4) {
                    System.out.println("Row " + rowNumber + " raw data: " + String.join(" | ", row));
                    System.out.println("Row " + rowNumber + " column count: " + row.length);
                }
                
                try {
                    Deals deal = parseRowToDeal(row, indices);
                    if (deal != null) {
                        dealsList.add(deal);
                        if (rowNumber <= 4) {
                            System.out.println("Row " + rowNumber + " parsed successfully: " + 
                                deal.getEmployeeNumber() + " - " + deal.getFirstName());
                        }
                    } else {
                        errors.add("Row " + rowNumber + ": Failed to parse row - insufficient data");
                    }
                } catch (Exception e) {
                    String errorMsg = "Row " + rowNumber + ": " + e.getMessage();
                    errors.add(errorMsg);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Error parsing row {}: {}", rowNumber, e.getMessage());
                    }
                    if (rowNumber <= 4) {
                        System.err.println("Row " + rowNumber + " error: " + e.getMessage());
                    }
                }
                rowNumber++;
            }
            
        } catch (CsvException e) {
            errors.add("CSV parsing error: " + e.getMessage());
            logger.error("CSV parsing error", e);
        } catch (Exception e) {
            errors.add("File reading error: " + e.getMessage());
            logger.error("Error reading CSV file", e);
        }
        
        if (!errors.isEmpty()) {
            System.out.println("Warnings/Errors encountered during parsing:");
            printErrors(errors);
        }
        
        return dealsList;
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
        indices.empIdIdx = findColumnIndex(headerMap, KEY_EMPLOYEE_ID, 0);
        indices.firstNameIdx = findColumnIndex(headerMap, KEY_FIRST_NAME, 1);
        indices.lastNameIdx = findColumnIndex(headerMap, KEY_LAST_NAME, 2);
        indices.emailIdx = findColumnIndex(headerMap, KEY_EMAIL, 3);
        indices.phoneIdx = findColumnIndex(headerMap, KEY_PHONE, 4);
        indices.designationIdx = findColumnIndex(headerMap, KEY_DESIGNATION, 6);
        indices.dateOfJoiningIdx = findColumnIndex(headerMap, KEY_DATE_OF_JOINING, 7);
        indices.statusIdx = findColumnIndex(headerMap, KEY_STATUS, 9);
        return indices;
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
            deal.setEmail(email);
        }
        
        // Phone (required)
        String phone = getValue(row, indices.phoneIdx);
        if (phone == null || phone.isEmpty()) {
            throw new Exception("Phone is required");
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
     * Prints errors to console
     */
    private static void printErrors(List<String> errors) {
        for (int i = 0; i < Math.min(errors.size(), 20); i++) {
            System.err.println("  - " + errors.get(i));
        }
        if (errors.size() > 20) {
            System.err.println("  ... and " + (errors.size() - 20) + " more errors");
        }
        System.out.println();
    }
}

