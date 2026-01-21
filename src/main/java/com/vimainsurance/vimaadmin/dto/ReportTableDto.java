package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic tabular response for reports: headers + row values aligned to those headers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportTableDto {
    private List<String> headers;
    private List<List<Object>> rows;
}
