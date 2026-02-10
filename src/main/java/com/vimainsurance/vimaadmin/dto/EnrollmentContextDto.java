package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context returned after validating an enrollment token.
 * Includes the matched enrollment_windows record, employee/deals record,
 * and the draft submission that was created or found.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentContextDto {

    private UUID enrollmentWindowId;
    private UUID employeeId;
    /** Draft submission ID — created on first token validation if not already present. */
    private UUID submissionId;
    /** Invitation ID for this enrollment. */
    private UUID invitationId;
    /** Matched enrollment_windows record (retrieved via IEnrollmentWindowsRepository). */
    private EnrollmentWindowResponseDto enrollmentWindow;
    /** Matched employee/deals record (retrieved via IDealsRepository). */
    private DealsResponseDto employee;
}
