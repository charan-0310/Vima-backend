package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CSV validation response
 * Used for validating CSV files before upload or deletion operations
 */
@Data
@NoArgsConstructor
public class CsvValidationResponseDto {
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<String> errors;
    private List<String> warnings;
    private String message;
    private boolean isValid;
    private int totalEmployees;
    private int totalDependents;
    
    public CsvValidationResponseDto(int totalRows, int validRows, int invalidRows, 
                                   List<String> errors, String message, boolean isValid) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.errors = errors;
        this.warnings = List.of();
        this.message = message;
        this.isValid = isValid;
        this.totalEmployees = 0;
        this.totalDependents = 0;
    }
    
    public CsvValidationResponseDto(int totalRows, int validRows, int invalidRows, 
                                   List<String> errors, List<String> warnings, String message, boolean isValid,
                                   int totalEmployees, int totalDependents) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.errors = errors;
        this.warnings = warnings;
        this.message = message;
        this.isValid = isValid;
        this.totalEmployees = totalEmployees;
        this.totalDependents = totalDependents;
    }
}


