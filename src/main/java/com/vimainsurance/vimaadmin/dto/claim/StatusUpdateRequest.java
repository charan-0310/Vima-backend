package com.vimainsurance.vimaadmin.dto.claim;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotNull(message = "New status is required")
    private ClaimStatus newStatus;

    /** Optional remark (e.g. rejection reason when transitioning to REJECTED / REJECTED_BY_ADMIN) */
    private String remark;

    /** Optional notes for audit (e.g. "Submitted via ICICI portal") */
    private String notes;

    /** When transitioning to SUBMITTED_TO_INSURER, optional insurer reference from admin */
    private String insurerClaimRef;

    /** When transitioning to SUBMITTED_TO_INSURER or APPROVED, optional insurer claim number */
    private String insurerClaimNumber;
}
