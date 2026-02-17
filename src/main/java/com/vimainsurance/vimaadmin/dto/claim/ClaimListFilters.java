package com.vimainsurance.vimaadmin.dto.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;

import lombok.Data;

@Data
public class ClaimListFilters {

    private UUID organizationId;
    /** When set, filter by organization in this list (e.g. HR with multiple orgs). Takes precedence over organizationId when non-empty. */
    private List<UUID> organizationIds;
    private UUID employeeId;
    private Long policyId;
    private ClaimStatus internalStatus;
    private ClaimType claimType;
    private String claimNumber;
    /** Search by claim number or member name (partial match). */
    private String search;
    private LocalDate dateOfSubmissionFrom;
    private LocalDate dateOfSubmissionTo;
    private UUID insurerId;
    private String insurerClaimRef;
    private Boolean isDeleted;
}
