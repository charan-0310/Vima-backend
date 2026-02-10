package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProgressDetailDto {

    private UUID employeeId;
    /** Enrollment submission ID when employee has a submission (e.g. SUBMITTED, APPROVED, REJECTED). */
    private UUID submissionId;
    private String name;
    private String email;
    private String employeeNumber;
    private String enrollmentStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
}
