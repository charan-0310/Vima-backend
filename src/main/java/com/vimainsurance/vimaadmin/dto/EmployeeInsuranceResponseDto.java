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
    /** For GPA/GTL multiplier mode: 1–5. When set, display as "N× CTC". */
    private Integer sumInsuredMultiplier;
    private BigDecimal premiumAmount;
    private LocalDate policyStartDate;
    private String healthId;

    // TPA Details
    private String tpaOrganizationName;
    private String tpaContactInfo;
    private String networkHospitalsUrl;
    private String blacklistedHospitalsUrl;

    private String companyName;

    /**
     * Organization primary contact — from {@code organizations.primary_contact_email} / {@code primary_contact_phone}
     * for employee-facing "Contact HR" actions.
     */
    private String primaryContactEmail;
    private String primaryContactPhone;

    // Covered members (employee + dependents) for the primary/first policy (backward compatibility)
    private List<CoveredMemberDto> coveredMembers;

    /**
     * All policies for the organization (GMC, GTL, GPA, etc.) with covered members per policy.
     * Employee portal can render one card per policy.
     */
    private List<PolicyDetailDto> policies;

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
        private String policyNumber;
        private String insuranceProviderLogo;
        private UUID individualId;
        private String fullName;
        private String relationship;
        private String actualRelationship;
        private LocalDate dateOfBirth;
        private String gender;
        private String email;
        private String phone;
        private String status;
        private BigDecimal sumInsured;
        // TPA Details
        private String tpaOrganizationName;
        private String tpaContactInfo;
        private String companyName;
    }

    /**
     * One policy (GMC, GTL, GPA, etc.). GMC has coveredMembers; GTL/GPA have nominees.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PolicyDetailDto {
        private String insuranceType;
        private String coverageType;
        private String insuranceProviderLogo;
        private Long policyId;
        private String policyNumber;
        private LocalDate validUntil;
        private PolicyStatus policyStatus;
        private BigDecimal sumInsured;
        /** For GPA/GTL multiplier mode: 1–5. When set, display as "N× CTC". */
        private Integer sumInsuredMultiplier;
        private BigDecimal premiumAmount;
        private LocalDate policyStartDate;
        private String tpaOrganizationName;
        private String tpaContactInfo;
        /** For GMC: employee + dependents. Empty for GTL/GPA. */
        private List<CoveredMemberDto> coveredMembers;
        /** For GTL/GPA: nominees for this employee. Empty for GMC. */
        private List<NomineeDto> nominees;
    }

    /**
     * DTO for nominee details (GTL/GPA policies).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NomineeDto {
        private UUID nomineeId;
        private String firstName;
        private String lastName;
        private String fullName;
        private LocalDate dateOfBirth;
        private String gender;
        private String relationship;
        private BigDecimal nomineePercentage;
    }
}
