package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for organization employee with only necessary fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationEmployeeDto {
    
    private UUID individualId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String employeeNumber;
    private String sumInsured;
    private String designation;
    private String department;
    private String maritalStatus;
    private BigDecimal ctc;
    private LocalDate dateOfJoining;
    private String status; // AccountStatus as string
    private LocalDate dateOfBirth;
    private String gender;
    private Boolean isPrimaryMember;
    private String relationship; // Relationship to primary member (for dependents)
    private String actualRelationship; // Original label e.g. Son, Daughter when relationship is CHILD1–CHILD4
    private String organizationName;
    private Integer dependentCount; // Number of dependents for this employee
    private String healthId;
    private String enrollementStatus;
    private UUID primaryIndividualId;
    private LocalDate dateOfExit;
    private String reasonForExit;
    private String accountType;
    private String custId;
    private LocalDateTime customerCreatedAt;
    private LocalDateTime customerUpdatedAt;
    private String username;
    private UUID enrollmentWindowId;
    private UUID enrollmentSubmissionId;
    private Integer exportSchemaVersion;
    private UUID endorsementId;
    private String endorsementType;
    private String endorsementStatus;
    private String endorsementSource;
    private Long endorsementPolicyId;
    private String endorsementPolicyNumber;
    private UUID splitGroupId;
    private String insurerRefNumber;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private List<EndorsementPolicyCoverDto> policyCovers;
}

