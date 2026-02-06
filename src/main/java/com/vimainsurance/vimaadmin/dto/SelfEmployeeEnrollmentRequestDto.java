package com.vimainsurance.vimaadmin.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelfEmployeeEnrollmentRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Date of Birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Employee ID is required")
    private String employeeId;
}