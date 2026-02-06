package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for GET /api/v1/admin/enrollments/windows/{windowId}/progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentProgressResponseDto {

    private UUID windowId;
    private String windowStatus;
    private int totalEmployees;
    private int invitedCount;
    private int openedCount;
    private int submittedCount;
    private int approvedCount;
    private int reviewedCount;
    private double completionRate;
    private List<EmployeeProgressDetailDto> employeeDetails;
}
