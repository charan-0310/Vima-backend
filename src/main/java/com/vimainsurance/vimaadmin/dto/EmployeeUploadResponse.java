package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CSV upload response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUploadResponse {
    private int totalRows;
    private int successCount;
    private int errorCount;
    private List<String> errors;
    private String message;
    private int totalEmployees;
    private int totalDependents;
    private List<EndorsementSplitSummaryDto> endorsements;

    public EmployeeUploadResponse(int totalRows, int successCount, int errorCount, List<String> errors, String message, int totalEmployees, int totalDependents) {
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.errors = errors;
        this.message = message;
        this.totalEmployees = totalEmployees;
        this.totalDependents = totalDependents;
    }
}

