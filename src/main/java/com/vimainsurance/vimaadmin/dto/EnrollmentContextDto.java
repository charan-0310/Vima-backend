package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context returned after validating an enrollment token.
 * Includes the matched enrollment_windows record and employee/deals record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentContextDto {

    private UUID enrollmentWindowId;
    private UUID employeeId;
    /** Matched enrollment_windows record (retrieved via IEnrollmentWindowsRepository). */
    private EnrollmentWindowResponseDto enrollmentWindow;
    /** Matched employee/deals record (retrieved via IDealsRepository). */
    private DealsResponseDto employee;
}
