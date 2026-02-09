package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for approving an enrollment submission (optional comment).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    /** Optional comment/notes for the approval. */
    private String comment;
}
