package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.EnrollementStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context object returned after successful enrollment token validation.
 * Contains all information needed for the employee enrollment flow.
 * 
 * The rawToken field is returned to the frontend to be used for all
 * subsequent API calls during the enrollment process (token-based auth).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentContext {
    
    // Token info - raw token returned for subsequent API calls
    private String rawToken;
    private UUID invitationId;
    private LocalDateTime expiresAt;
    private Long daysRemaining;
    
    // Employee info
    private UUID employeeId;
    private String employeeName;
    private String employeeEmail;
    private LocalDate dateOfBirth;
    private String grade;
    
    // Window info
    private UUID windowId;
    private String windowName;
    private LocalDate windowStartDate;
    private LocalDate windowEndDate;
    private EnrollementStatus windowStatus;
    private String windowConfig; // JSONB config
    
    // Submission info (nullable if not started)
    private UUID submissionId;
    private EnrollementStatus submissionStatus;
    private String planSelections; // JSONB
    private String nomineeData; // JSONB
}
