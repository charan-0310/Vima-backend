package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid mobile number")
    public String mobile;
    @NotBlank(message = "Date of Joining is required")
    public String dateOfJoining;
    @NotBlank(message = "Designation is required")
    public String designation;
    @NotBlank(message = "Department is required")
    public String department;
    public String maritalStatus;
    public String sumInsured;
    public String remarks;
}
