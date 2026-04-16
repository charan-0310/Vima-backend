package com.vimainsurance.vimaadmin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUploadDto {
    // /employee_id	relationship	name	date_of_birth	gender	email	mobile	date_of_joining	designation	department	marital_status	sum_insured	date_of_exit	deletion_reason	remarks
    //employee_id	relationship	name	date_of_birth	gender	email	mobile	date_of_joining	designation	department	marital_status	sum_insured	date_of_exit	deletion_reason	remarks
    @NotBlank(message = "Employee ID is required")
    public String employeeId;
    @NotBlank(message = "Relationship is required")
    public String relationship;
    @NotBlank(message = "Name is required")
    public String name;
    @NotBlank(message = "Date of Birth is required")
    public String dateOfBirth;
    @NotBlank(message = "Gender is required")
    public String gender;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    public String email;
    public String mobile;
    public String dateOfJoining;
    public String designation;
    public String department;
    public String maritalStatus;
    public String sumInsured;
    public String deletionReason;
    public String healthId;

    /** Original relationship (e.g. Son, Daughter) before frontend normalizes to CHILD1–CHILD4; optional. */
    public String actualRelationship;

    /** Optional cover SI (numeric rupees). Present only on SELF row when Top-Up selected. */
    @JsonProperty("topup_sum_insured")
    public String topupSumInsured;

    /** Optional cover SI (numeric rupees). Present only on SELF row when Super Top-Up selected. */
    @JsonProperty("super_topup_sum_insured")
    public String superTopupSumInsured;

    public String getComparisonString() {
        return this.employeeId + this.relationship + this.name + this.dateOfBirth + this.gender + this.email + this.mobile + this.dateOfJoining + this.designation + this.department + this.maritalStatus + this.sumInsured;
    }
}
