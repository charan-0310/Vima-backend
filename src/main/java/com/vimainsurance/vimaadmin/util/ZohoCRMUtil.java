package com.vimainsurance.vimaadmin.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.zoho.crm.api.record.Record;

@Component
public class ZohoCRMUtil {

    /**
     * Maps a Zoho CRM record to CustomerResponseDto
     * @param record The Zoho CRM record
     * @return Mapped CustomerResponseDto
     */
    public CustomerResponseDto mapRecordToCustomerDto(Record record) {
        CustomerResponseDto dto = new CustomerResponseDto();
        try {
            dto.setZohoCrmId(record.getId().toString());
            dto.setFullName(getRecordValue(record, "Full_Name"));
            dto.setEmail(getRecordValue(record, "Email"));
            dto.setPhoneNumber(getRecordValue(record, "Mobile"));
            dto.setCity(getRecordValue(record, "City"));
            dto.setState(getRecordValue(record, "State"));
            dto.setOccupation(getRecordValue(record, "Occupation"));
            dto.setGender(getRecordValue(record, "Gender"));
            // Handle annual income
            String annualIncome = getRecordValue(record, "Annual_Income");
            if (annualIncome != null && !annualIncome.isEmpty()) {
                dto.setAnnualIncome(new BigDecimal(annualIncome));
            }
            
            // Handle dates
            String createdTime = getRecordValue(record, "Created_Time");
            String modifiedTime = getRecordValue(record, "Modified_Time");
            
            dto.setCreatedAt(parseDateTimeWithFallback(createdTime, "created time"));
            dto.setUpdatedAt(parseDateTimeWithFallback(modifiedTime, "modified time"));
            
            // Map status
            String leadStatus = getRecordValue(record, "Lead_Status");
            dto.setStatus(mapZohoStatusToCustomerStatus(leadStatus));
            
        } catch (Exception e) {
            System.err.println("Error mapping Zoho record to CustomerDto: " + e.getMessage());
        }
        return dto;
    }

    /**
     * Parses a datetime string from Zoho CRM format to LocalDateTime
     * Handles various formats and provides fallback to current time if parsing fails
     * 
     * @param dateTimeStr The datetime string to parse
     * @param fieldName The name of the field (for error logging)
     * @return LocalDateTime parsed from the string, or current time if parsing fails
     */
    private LocalDateTime parseDateTimeWithFallback(String dateTimeStr, String fieldName) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // Remove timezone offset if present (e.g., +05:30)
            dateTimeStr = dateTimeStr.replaceAll("[+-]\\d{2}:?\\d{2}$", "");
            // Ensure the format is correct for parsing
            dateTimeStr = dateTimeStr.replace(" ", "T");
            if (dateTimeStr.length() > 19) {
                dateTimeStr = dateTimeStr.substring(0, 19);
            }
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            System.err.println("Error parsing " + fieldName + ": " + dateTimeStr + " - " + e.getMessage());
            return LocalDateTime.now();
        }
    }

    /**
     * Gets a value from a Zoho CRM record safely
     * @param record The Zoho CRM record
     * @param fieldName The field name to retrieve
     * @return The value as a string, or empty string if not found
     */
    public String getRecordValue(Record record, String fieldName) {
        try {
            Object value = record.getKeyValue(fieldName);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            System.err.println("Error getting field " + fieldName + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Maps Zoho CRM lead status to customer status
     * @param zohoStatus The Zoho CRM lead status
     * @return Mapped customer status
     */
    public String mapZohoStatusToCustomerStatus(String zohoStatus) {
        if (zohoStatus == null || zohoStatus.isEmpty()) {
            return "PENDING";
        }
        return switch (zohoStatus.toUpperCase()) {
            case "QUALIFIED" -> "ACTIVE";
            case "NOT QUALIFIED" -> "INACTIVE";
            default -> "PENDING";
        };
    }
} 