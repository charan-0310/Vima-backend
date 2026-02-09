package com.vimainsurance.vimaadmin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for bulk-approving enrollment submissions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkApprovalRequest {

    /** List of enrollment submission IDs to approve. */
    @NotEmpty(message = "At least one submission ID is required")
    private List<UUID> submissionIds;
}
