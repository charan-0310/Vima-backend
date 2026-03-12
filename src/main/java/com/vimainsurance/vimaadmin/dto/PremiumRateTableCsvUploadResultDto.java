package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumRateTableCsvUploadResultDto {

    private int totalRows;
    private int insertedCount;
    private int errorCount;
    private List<RowError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int rowIndex;
        private String message;
    }
}
