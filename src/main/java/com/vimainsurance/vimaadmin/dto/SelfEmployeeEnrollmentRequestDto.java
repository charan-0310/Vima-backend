package com.vimainsurance.vimaadmin.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelfEmployeeEnrollmentRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    /** Optional; when provided must not be in the future. */
    private LocalDate dateOfBirth;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    /** Optional extra fields for prefill / Deals mapping */
    private String phone;
    private String gender;
    private LocalDate dateOfJoining;
    private String designation;
    private String department;
    private BigDecimal ctc;
}