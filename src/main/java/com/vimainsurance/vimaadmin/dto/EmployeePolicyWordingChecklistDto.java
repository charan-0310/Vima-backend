package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Employee-facing payload for the policy wording / claim checklist screen.
 * Returns the admin-authored rich text content directly. Uploaded PDFs are
 * intentionally NOT exposed to employees; they remain admin-only references
 * via the {@code POST/DELETE /policies/{id}/wording-document} (and
 * checklist-document) endpoints. See V79.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePolicyWordingChecklistDto {
    private Long policyId;
    private String policyNumber;
    private String productType;

    /** Admin-authored condensed wording (HTML) shown in place of the wording PDF. Null/blank when not yet authored. */
    private String policyWordingSummary;

    /** Admin-authored extra-documents notes (HTML) appended to the default claim checklist. Null/blank when not yet authored. */
    private String claimChecklistAdditionalDocs;

    private LocalDateTime updatedAt;
}
