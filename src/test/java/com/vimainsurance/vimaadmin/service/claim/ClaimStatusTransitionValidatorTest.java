package com.vimainsurance.vimaadmin.service.claim;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.exception.InvalidStatusTransitionException;

@ExtendWith(MockitoExtension.class)
class ClaimStatusTransitionValidatorTest {

    private ClaimStatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ClaimStatusTransitionValidator();
    }

    @Test
    void validateTransition_validFromDraftToPendingReview_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validateTransition(ClaimStatus.DRAFT, ClaimStatus.PENDING_REVIEW));
    }

    @Test
    void validateTransition_invalidFromDraftToClosed_throws() {
        InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(ClaimStatus.DRAFT, ClaimStatus.CLOSED));
        Set<ClaimStatus> allowed = ex.getAllowedTransitions();
        assertTrue(allowed != null && allowed.contains(ClaimStatus.PENDING_REVIEW) && !allowed.contains(ClaimStatus.CLOSED));
    }

    @Test
    void validateTransition_invalidFromDraftToSubmitted_throwsWithAllowedList() {
        InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(ClaimStatus.DRAFT, ClaimStatus.SUBMITTED_TO_INSURER));
        assertTrue(ex.getMessage().contains("PENDING_REVIEW"));
        Set<ClaimStatus> allowed = ex.getAllowedTransitions();
        assertTrue(allowed != null && allowed.contains(ClaimStatus.PENDING_REVIEW));
    }

    @Test
    void validateTransition_nullCurrent_throws() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(null, ClaimStatus.PENDING_REVIEW));
    }

    @Test
    void validateTransition_nullNew_throws() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(ClaimStatus.DRAFT, null));
    }

    @Test
    void validateTransition_sameStatus_throws() {
        InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(ClaimStatus.DRAFT, ClaimStatus.DRAFT));
        assertTrue(ex.getMessage().contains("different from current"));
    }

    @Test
    void validateTransition_fromSettledToClosed_valid_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validateTransition(ClaimStatus.SETTLED, ClaimStatus.CLOSED));
    }

    @Test
    void validateTransition_fromSettledToDraft_throws() {
        InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(ClaimStatus.SETTLED, ClaimStatus.DRAFT));
        assertTrue(ex.getCurrentStatus() == ClaimStatus.SETTLED);
        assertTrue(ex.getAllowedTransitions() != null && ex.getAllowedTransitions().contains(ClaimStatus.CLOSED));
    }

    @Test
    void validateTransition_fromTerminalClosed_throws() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(ClaimStatus.CLOSED, ClaimStatus.DRAFT));
    }

    @Test
    void validateTransition_fromTerminalRejectedByAdmin_throws() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(ClaimStatus.REJECTED_BY_ADMIN, ClaimStatus.DRAFT));
    }

    @Test
    void validateTransition_approvedForSubmissionToSubmittedToInsurer_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validateTransition(ClaimStatus.APPROVED_FOR_SUBMISSION, ClaimStatus.SUBMITTED_TO_INSURER));
    }

    @Test
    void validateTransition_pendingReviewToApprovedForSubmission_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validateTransition(ClaimStatus.PENDING_REVIEW, ClaimStatus.APPROVED_FOR_SUBMISSION));
    }

    @Test
    void isTerminal_settled_returnsFalse() {
        assertFalse(validator.isTerminal(ClaimStatus.SETTLED));
    }

    @Test
    void isTerminal_closed_returnsTrue() {
        assertTrue(validator.isTerminal(ClaimStatus.CLOSED));
    }

    @Test
    void isTerminal_draft_returnsFalse() {
        assertFalse(validator.isTerminal(ClaimStatus.DRAFT));
    }

    @Test
    void isTerminal_null_returnsFalse() {
        assertFalse(validator.isTerminal(null));
    }

    @Test
    void getAllowedTransitions_draft_returnsOnlyPendingReview() {
        Set<ClaimStatus> allowed = validator.getAllowedTransitions(ClaimStatus.DRAFT);
        assertTrue(allowed.contains(ClaimStatus.PENDING_REVIEW));
        assertFalse(allowed.contains(ClaimStatus.CLOSED));
        assertTrue(allowed.size() == 1);
    }

    @Test
    void validateTransitionRequirements_submittedToInsurerNoRefNotManual_throws() {
        assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransitionRequirements(ClaimStatus.SUBMITTED_TO_INSURER, false, false));
    }

    @Test
    void validateTransitionRequirements_submittedToInsurerWithRefNotManual_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validateTransitionRequirements(ClaimStatus.SUBMITTED_TO_INSURER, true, false));
    }

    @Test
    void validateTransitionRequirements_submittedToInsurerManual_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validateTransitionRequirements(ClaimStatus.SUBMITTED_TO_INSURER, false, true));
    }

    @Test
    void validateTransitionRequirements_otherStatus_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validateTransitionRequirements(ClaimStatus.DRAFT, false, false));
    }

    @Test
    void validateTransition_fromQueryRaisedToQueryResponded_valid_doesNotThrow() {
        assertDoesNotThrow(() -> validator.validateTransition(ClaimStatus.QUERY_RAISED, ClaimStatus.QUERY_RESPONDED));
    }

    @Test
    void validateTransition_fromQueryRaisedToClosed_invalid_throws() {
        InvalidStatusTransitionException ex = assertThrows(InvalidStatusTransitionException.class,
                () -> validator.validateTransition(ClaimStatus.QUERY_RAISED, ClaimStatus.CLOSED));
        Set<ClaimStatus> allowed = ex.getAllowedTransitions();
        assertTrue(allowed != null && allowed.contains(ClaimStatus.QUERY_RESPONDED) && !allowed.contains(ClaimStatus.CLOSED));
    }

    @Test
    void getAllowedTransitions_queryRaised_returnsOnlyQueryResponded() {
        Set<ClaimStatus> allowed = validator.getAllowedTransitions(ClaimStatus.QUERY_RAISED);
        assertTrue(allowed.contains(ClaimStatus.QUERY_RESPONDED));
        assertFalse(allowed.contains(ClaimStatus.CLOSED));
        assertTrue(allowed.size() == 1);
    }
}
