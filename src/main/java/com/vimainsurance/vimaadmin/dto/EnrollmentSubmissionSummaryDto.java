package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot of the enrollment submission for the token context (same row as {@code submissionId}).
 * Lets clients read workflow status and UI step without a separate GET .../submissions call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentSubmissionSummaryDto {

    private UUID submissionId;
    /** Same as {@code enrollment_submissions.status} (e.g. DRAFT, SUBMITTED, APPROVED). */
    private String workflowStatus;
    /** Wizard step persisted by the portal (e.g. verify, dependents, review, success). */
    private String currentStep;
    private LocalDateTime submittedAt;
    /**
     * Derived convenience: NOT_STARTED / IN_PROGRESS for drafts; otherwise aligns with workflow terminal states.
     */
    private String derivedLifecycle;
}
