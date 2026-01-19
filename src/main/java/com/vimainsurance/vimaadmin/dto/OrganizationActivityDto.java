package com.vimainsurance.vimaadmin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.UUID;
import com.vimainsurance.vimaadmin.dto.EndorsementActivityDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationActivityDto {
    private Long activeLives;
    private Long employees;
    private Long dependents;
    private long totalAdditions;
    private long totalDeletions;
    private long totalInactives;
    private EndorsementActivityDto endorsementActivity;
    private List<MonthlyEndorsementActivityDto> monthlyEndorsementActivity;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyEndorsementActivityDto {
        private String month; // Format: "YYYY-MM" or "MMM YYYY"
        private Long additions;
        private Long deletions;
    }
}
