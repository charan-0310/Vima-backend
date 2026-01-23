package com.vimainsurance.vimaadmin.dto;

import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for employee insurance details including policy and covered members
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeInsuranceResponseDto {

    // Employee basic info
    private UUID employeeId;
    private String employeeNumber;
    private String employeeName;
    private String email;
    private String phone;

    // Policy summary
    private String cardType;
    private String insuranceType;
    private String coverageType;
    private String insuranceProviderLogo;
    private Long policyId;
    private String policyNumber;
    private LocalDate validUntil; // policy end date
    private PolicyStatus policyStatus;
    private BigDecimal sumInsured;
    private BigDecimal premiumAmount;
    private LocalDate policyStartDate;
    private String healthId;

    // Covered members (employee + dependents)
    private List<CoveredMemberDto> coveredMembers;

    /**
     * DTO for covered member details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoveredMemberDto {
        private String healthId;
        private Long policyId;
        private UUID individualId;
        private String fullName;
        private String relationship;
        private LocalDate dateOfBirth;
        private String gender;
        private String email;
        private String phone;
        private String status;
        private BigDecimal sumInsured;
    }
}
