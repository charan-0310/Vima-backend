package com.vimainsurance.vimaadmin.adapter;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

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
 * Adapter for manual path: no insurer API calls; returns requiresManualSubmission/requiresManualForward.
 */
@Component
@Primary
@Order(100)
public class ManualInsurerAdapter implements InsurerAdapter {

    private static final String MANUAL = "MANUAL";

    @Override
    public InsurerSubmissionResult submitToInsurer(Claim claim) {
        return InsurerSubmissionResult.builder()
                .success(true)
                .requiresManualSubmission(true)
                .message("Claim approved for manual submission to insurer")
                .build();
    }

    @Override
    public DocumentForwardResult forwardDocuments(Claim claim, List<Document> documents) {
        return DocumentForwardResult.builder()
                .success(true)
                .requiresManualForward(true)
                .message("Documents to be forwarded manually to insurer")
                .build();
    }

    @Override
    public ClaimStatusResult fetchClaimStatus(Claim claim) {
        String status = claim.getInternalStatus() != null ? claim.getInternalStatus().getValue() : null;
        return ClaimStatusResult.builder()
                .status(status)
                .source("VIMA")
                .message("Status from VIMA records")
                .rawInsurerStatus(null)
                .build();
    }

    @Override
    public QueryResponseResult respondToQuery(Claim claim, ClaimQuery query) {
        return QueryResponseResult.builder()
                .success(true)
                .requiresManualForward(true)
                .message("Query response to be forwarded manually to insurer")
                .build();
    }

    @Override
    public List<ClaimLetter> fetchClaimLetters(Claim claim, String letterType) {
        return Collections.emptyList();
    }

    @Override
    public MemberCoverageResult fetchMemberCoverage(String policyNumber, String uhid) {
        return MemberCoverageResult.builder()
                .source("VIMA")
                .sumInsured(null)
                .balanceSumInsured(null)
                .memberDetails(Collections.emptyMap())
                .build();
    }

    @Override
    public String getInsurerId() {
        return MANUAL;
    }

    @Override
    public boolean isApiIntegrated() {
        return false;
    }

    @Override
    public boolean supports(UUID insurerId) {
        return false; // used as fallback when no insurer-specific adapter matches
    }
}
