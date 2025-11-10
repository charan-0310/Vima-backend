package com.vimainsurance.vimaadmin.util;

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
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;

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
    
    // Column search keys (pre-computed for performance)
    private static final String KEY_EMPLOYEE_ID = "employee id";
    private static final String KEY_FIRST_NAME = "first name";
    private static final String KEY_LAST_NAME = "last name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_DESIGNATION = "designation";
    private static final String KEY_DATE_OF_JOINING = "date of joining";
    private static final String KEY_STATUS = "status";
    
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
        List<String[]> allRows = null;
        
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
            
            // Read data rows
            allRows = csvReader.readAll();
            int rowNumber = 2; // Start from 2 (1 is header, 2 is first data row)
            
            // Pre-allocate list size for better performance
            dealsList = new ArrayList<>(allRows.size());
            
            for (String[] row : allRows) {
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
        result.setTotalRows(allRows != null ? allRows.size() + 1 : 1); // +1 for header
        result.setSuccessCount(dealsList.size());
        result.setErrorCount(errors.size());
        
        return result;
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
    }
}

