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
public class CsvUploadResponseDto {
    private int totalRows;
    private int successCount;
    private int errorCount;
    private List<String> errors;
    private String message;
    private int totalEmployees;
    private int totalDependents;
}

