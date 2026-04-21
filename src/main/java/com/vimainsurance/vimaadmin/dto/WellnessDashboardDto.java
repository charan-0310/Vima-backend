package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessDashboardDto {
    private List<PartnerAccessStat> partners;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnerAccessStat {
        private UUID partnerId;
        private String partnerName;
        private String partnerSlug;
        private Long totalAccesses;
        private Long uniqueEmployees;
        private Double successRate;
    }
}
