package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a row in the export report
 * Uses standard headers that cover 90% of insurer needs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportExportRowDto {

    // Member Information
    private String employeeNumber;
    private String fullName;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String relationship;
    private Boolean isPrimaryMember;
    private LocalDate exitDate;
    private LocalDate coverageEndDate;
    private Integer daysCovered;

    // Contact Information
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;

    // Identity Documents
    private String panNumber;
    private String aadhaarNumber;

    // Employment Information
    private String designation;
    private String department;
    private LocalDate dateOfJoining;
    private LocalDate dateOfExit;

    // Organization Information
    private UUID organizationId;
    private String organizationName;

    // Status Information
    private String status;
    private String accountType;

    //
    private String ecardStatus;
    private String tpa;

    // Insurance/Premium Information (for payroll reports)
    private BigDecimal premiumAmount;
    private BigDecimal sumInsured;
    private String policyNumber;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;

    // Endorsement Information (for enrollment reports)
    private UUID endorsementId;
    private String endorsementType;
    private String endorsementStatus;
    private LocalDate approvedAt;
    private String approvedBy;
    private Integer totalEmployees;
    private Integer totalDependents;
    private Integer totalEmployeesRemoved;
    private Integer totalDependentsRemoved;
    private Integer totalLivesChanged;
    private String  submissionDate;
    private String premiumChangeType;
    private String insurerRefNumber;
    private LocalDateTime createdAt;
    private LocalDate completionDate;
    private String submittedBy;
    private String notes;
    private String exitReason;
    private String exitNotes;


    // Marital Status
    private String maritalStatus;

    // Employees changes
    private String changeDate;
    private String changeType;
    private String reason;
}

