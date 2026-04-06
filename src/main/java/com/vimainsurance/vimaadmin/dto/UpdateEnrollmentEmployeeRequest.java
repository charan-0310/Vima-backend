package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HR update to primary employee (Deals), personal_details, and optional dependents JSON on the submission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEnrollmentEmployeeRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Phone is required")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Employee number is required")
    @Size(max = 100)
    private String employeeNumber;

    /** ISO date yyyy-MM-dd */
    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;

    @Size(max = 20)
    private String gender;

    /** ISO date yyyy-MM-dd */
    private String dateOfJoining;

    @Size(max = 200)
    private String department;

    @Size(max = 50)
    private String maritalStatus;

    /** Grade / designation shown in enrollment UI */
    @Size(max = 200)
    private String designation;

    /**
     * When non-null, replaces submission dependents JSON (use empty list to clear).
     * When null, dependents column is left unchanged.
     */
    @Valid
    private List<DependentEnrollmentUpdateDto> dependents;
}
