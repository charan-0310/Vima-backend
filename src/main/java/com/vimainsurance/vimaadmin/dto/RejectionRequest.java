package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for rejecting an enrollment submission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectionRequest {

    /** Reason for rejection (stored in submission). */
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    /** If true, invitation status is reset so employee can resubmit. */
    private boolean reopenInvitation = false;
}
