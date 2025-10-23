package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManagerDashboardResponseDto {
    
    private TeamMetrics teamMetrics;
    private List<AgentMetrics> agentMetrics;
    
    @Getter
    @Setter
    public static class TeamMetrics {
        private Integer totalLeads;
        private Integer totalQuotes;
        private Integer totalPolicies;
        private BigDecimal totalBusinessAmount;
        private Double leadsGrowth;
        private Double quotesGrowth;
        private Double policiesGrowth;
        private Double businessGrowth;
        private Double conversionRate;
        private Double closingRate;
        private BigDecimal avgPolicyValue;
        private Integer activeAgents;
    }
    
    @Getter
    @Setter
    public static class AgentMetrics {
        private String agentName;
        private Integer leads;
        private Integer quotes;
        private Integer policies;
        private BigDecimal businessAmount;
        private Double conversionRate;
        private Double closingRate;
    }
}
