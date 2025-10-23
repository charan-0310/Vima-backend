package com.vimainsurance.vimaadmin.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.vimainsurance.vimaadmin.dto.ManagerDashboardResponseDto;

/**
 * Utility class to provide placeholder data for manager dashboard when backend is not ready
 * or for development/testing purposes
 */
public class ManagerDashboardPlaceholderData {
    
    public static ManagerDashboardResponseDto getPlaceholderData() {
        ManagerDashboardResponseDto responseDto = new ManagerDashboardResponseDto();
        
        // Set team metrics
        ManagerDashboardResponseDto.TeamMetrics teamMetrics = new ManagerDashboardResponseDto.TeamMetrics();
        teamMetrics.setTotalLeads(245);
        teamMetrics.setTotalQuotes(187);
        teamMetrics.setTotalPolicies(89);
        teamMetrics.setTotalBusinessAmount(new BigDecimal("12500000"));
        teamMetrics.setLeadsGrowth(18.5);
        teamMetrics.setQuotesGrowth(22.3);
        teamMetrics.setPoliciesGrowth(15.8);
        teamMetrics.setBusinessGrowth(28.4);
        teamMetrics.setConversionRate(76.3);
        teamMetrics.setClosingRate(47.6);
        teamMetrics.setAvgPolicyValue(new BigDecimal("140449"));
        teamMetrics.setActiveAgents(5);
        
        // Set agent metrics
        List<ManagerDashboardResponseDto.AgentMetrics> agentMetrics = Arrays.asList(
            createAgentMetric("Rajesh Kumar", 65, 52, 28, new BigDecimal("3800000"), 80.0, 53.8),
            createAgentMetric("Priya Sharma", 58, 48, 24, new BigDecimal("3200000"), 82.8, 50.0),
            createAgentMetric("Amit Patel", 52, 42, 19, new BigDecimal("2600000"), 80.8, 45.2),
            createAgentMetric("Sneha Reddy", 45, 35, 13, new BigDecimal("1900000"), 77.8, 37.1),
            createAgentMetric("Vikram Singh", 25, 10, 5, new BigDecimal("1000000"), 40.0, 50.0)
        );
        
        responseDto.setTeamMetrics(teamMetrics);
        responseDto.setAgentMetrics(agentMetrics);
        
        return responseDto;
    }
    
    private static ManagerDashboardResponseDto.AgentMetrics createAgentMetric(
            String agentName, int leads, int quotes, int policies, 
            BigDecimal businessAmount, double conversionRate, double closingRate) {
        
        ManagerDashboardResponseDto.AgentMetrics agentMetric = new ManagerDashboardResponseDto.AgentMetrics();
        agentMetric.setAgentName(agentName);
        agentMetric.setLeads(leads);
        agentMetric.setQuotes(quotes);
        agentMetric.setPolicies(policies);
        agentMetric.setBusinessAmount(businessAmount);
        agentMetric.setConversionRate(conversionRate);
        agentMetric.setClosingRate(closingRate);
        
        return agentMetric;
    }
}
