package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.util.UUID;

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
}

