package com.vimainsurance.vimaadmin.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vimainsurance.vimaadmin.dto.claim.ClaimLetter;
import com.vimainsurance.vimaadmin.dto.claim.ClaimStatusResult;
import com.vimainsurance.vimaadmin.dto.claim.DocumentForwardResult;
import com.vimainsurance.vimaadmin.dto.claim.InsurerSubmissionResult;
import com.vimainsurance.vimaadmin.dto.claim.MemberCoverageResult;
import com.vimainsurance.vimaadmin.dto.claim.QueryResponseResult;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimQuery;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;

class ManualInsurerAdapterTest {

    private ManualInsurerAdapter adapter;
    private Claim claim;

    @BeforeEach
    void setUp() {
        adapter = new ManualInsurerAdapter();
        claim = new Claim();
        claim.setInternalStatus(ClaimStatus.PENDING_REVIEW);
    }

    @Test
    void submitToInsurer_returnsSuccessWithRequiresManualSubmission() {
        InsurerSubmissionResult result = adapter.submitToInsurer(claim);
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.isRequiresManualSubmission());
        assertNotNull(result.getMessage());
    }

    @Test
    void forwardDocuments_returnsSuccessWithRequiresManualForward() {
        List<Document> documents = Collections.emptyList();
        DocumentForwardResult result = adapter.forwardDocuments(claim, documents);
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.isRequiresManualForward());
        assertNotNull(result.getMessage());
    }

    @Test
    void fetchClaimStatus_returnsVimaStoredStatus() {
        ClaimStatusResult result = adapter.fetchClaimStatus(claim);
        assertNotNull(result);
        assertEquals("VIMA", result.getSource());
        assertEquals(ClaimStatus.PENDING_REVIEW.getValue(), result.getStatus());
        assertNotNull(result.getMessage());
    }

    @Test
    void fetchClaimStatus_whenStatusNull_returnsNullStatus() {
        claim.setInternalStatus(null);
        ClaimStatusResult result = adapter.fetchClaimStatus(claim);
        assertNotNull(result);
        assertEquals("VIMA", result.getSource());
        assertEquals(null, result.getStatus());
    }

    @Test
    void respondToQuery_returnsSuccessWithRequiresManualForward() {
        ClaimQuery query = new ClaimQuery();
        QueryResponseResult result = adapter.respondToQuery(claim, query);
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.isRequiresManualForward());
        assertNotNull(result.getMessage());
    }

    @Test
    void fetchClaimLetters_returnsEmptyList() {
        List<ClaimLetter> result = adapter.fetchClaimLetters(claim, "DISCHARGE_SUMMARY");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchMemberCoverage_returnsVimaSourceStub() {
        MemberCoverageResult result = adapter.fetchMemberCoverage("POL-001", "UHID123");
        assertNotNull(result);
        assertEquals("VIMA", result.getSource());
        assertNotNull(result.getMemberDetails());
        assertTrue(result.getMemberDetails().isEmpty());
    }

    @Test
    void getInsurerId_returnsManual() {
        assertEquals("MANUAL", adapter.getInsurerId());
    }

    @Test
    void isApiIntegrated_returnsFalse() {
        assertFalse(adapter.isApiIntegrated());
    }

    @Test
    void supports_returnsFalseForAnyInsurerId() {
        assertFalse(adapter.supports(UUID.randomUUID()));
        assertFalse(adapter.supports(null));
    }
}
