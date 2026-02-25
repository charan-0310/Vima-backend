package com.vimainsurance.vimaadmin.dto.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result from insurer adapter (submitToInsurer).
 * When requiresManualSubmission is true, claim is set to APPROVED_FOR_SUBMISSION;
 * otherwise SUBMITTED_TO_INSURER.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsurerSubmissionResult {

    private boolean success;
    /** If true, claim must be submitted manually to insurer; status set to APPROVED_FOR_SUBMISSION */
    private boolean requiresManualSubmission;
    private String insurerClaimRef;
    private String message;
    private String errorCode;
}
