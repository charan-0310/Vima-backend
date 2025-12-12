package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
/**
 * DTO for bulk employee deletion request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmployeeDeletionRequestDto {
    //employee_id	relationship	name	date_of_birth	gender	email	mobile	date_of_joining	designation	department	marital_status	sum_insured	date_of_exit	deletion_reason	remarks
    @NotBlank(message = "Employee ID is required")
    private String employeeId;
    @NotBlank(message = "Relationship is required")
    private String relationship;
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Date of Birth is required")
    private String dateOfBirth;
    @NotBlank(message = "Gender is required")
    private String gender;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;
    @NotBlank(message = "Mobile is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile must be 10 digits")
    private String mobile;
    private String dateOfJoining;
    private String designation;
    private String department;
    private String maritalStatus;
    private String sumInsured;
    @NotBlank(message = "Date of Exit is required")
    private String dateOfExit;
    @NotBlank(message = "Deletion Reason is required")
    private String deletionReason;
    private String remarks;
}




