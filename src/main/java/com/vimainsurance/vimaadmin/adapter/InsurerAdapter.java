package com.vimainsurance.vimaadmin.adapter;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.claim.ClaimLetter;
import com.vimainsurance.vimaadmin.dto.claim.ClaimStatusResult;
import com.vimainsurance.vimaadmin.dto.claim.DocumentForwardResult;
import com.vimainsurance.vimaadmin.dto.claim.InsurerSubmissionResult;
import com.vimainsurance.vimaadmin.dto.claim.MemberCoverageResult;
import com.vimainsurance.vimaadmin.dto.claim.QueryResponseResult;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimQuery;
import com.vimainsurance.vimaadmin.entity.Document;

/**
 * Adapter for insurer integration (API or manual).
 * ManualAdapter returns requiresManualSubmission/requiresManualForward=true; API adapters perform real calls.
 */
public interface InsurerAdapter {

    /** Submit claim to insurer. For manual flow, returns success with requiresManualSubmission=true. */
    InsurerSubmissionResult submitToInsurer(Claim claim);

    /** Forward documents to insurer. Manual: requiresManualForward=true. */
    DocumentForwardResult forwardDocuments(Claim claim, List<Document> documents);

    /** Fetch claim status from insurer. Manual: returns VIMA-stored status. */
    ClaimStatusResult fetchClaimStatus(Claim claim);

    /** Record response to query (and optionally forward to insurer). Manual: requiresManualForward=true. */
    QueryResponseResult respondToQuery(Claim claim, ClaimQuery query);

    /** Fetch claim letters from insurer. Manual: returns empty list. */
    List<ClaimLetter> fetchClaimLetters(Claim claim, String letterType);

    /** Fetch member coverage (e.g. balance SI). Manual: returns VIMA-stored policy data. */
    MemberCoverageResult fetchMemberCoverage(String policyNumber, String uhid);

    /** Insurer identifier, e.g. "MANUAL" or "ICICI_LOMBARD". */
    String getInsurerId();

    /** True if this adapter uses insurer API; false for manual. */
    boolean isApiIntegrated();

    /** Whether this adapter supports the given insurer (null = generic/manual). */
    boolean supports(UUID insurerId);
}
