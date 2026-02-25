package com.vimainsurance.vimaadmin.service.claim;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.exception.InvalidStatusTransitionException;

/**
 * Validates claim status transitions per PRD §15.2.
 * Terminal statuses (no outgoing transitions): CLOSED, REJECTED_BY_ADMIN.
 * SETTLED can transition to CLOSED.
 */
@Component
public class ClaimStatusTransitionValidator {

    private static final Set<ClaimStatus> TERMINAL_STATUSES = EnumSet.of(
            ClaimStatus.CLOSED,
            ClaimStatus.REJECTED_BY_ADMIN
    );

    private static final Map<ClaimStatus, Set<ClaimStatus>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry(ClaimStatus.DRAFT, EnumSet.of(ClaimStatus.PENDING_REVIEW, ClaimStatus.CLOSED)),
            Map.entry(ClaimStatus.PENDING_REVIEW, EnumSet.of(ClaimStatus.DRAFT, ClaimStatus.INFO_REQUESTED,
                    ClaimStatus.APPROVED_FOR_SUBMISSION, ClaimStatus.REJECTED_BY_ADMIN)),
            Map.entry(ClaimStatus.INFO_REQUESTED, EnumSet.of(ClaimStatus.DRAFT, ClaimStatus.PENDING_REVIEW)),
            Map.entry(ClaimStatus.APPROVED_FOR_SUBMISSION, EnumSet.of(ClaimStatus.SUBMITTED_TO_INSURER, ClaimStatus.SUBMISSION_FAILED)),
            Map.entry(ClaimStatus.SUBMITTED_TO_INSURER, EnumSet.of(ClaimStatus.IN_PROGRESS, ClaimStatus.QUERY_RAISED,
                    ClaimStatus.APPROVED, ClaimStatus.REJECTED, ClaimStatus.INTIMATION_REJECTED)),
            Map.entry(ClaimStatus.SUBMISSION_FAILED, EnumSet.of(ClaimStatus.APPROVED_FOR_SUBMISSION, ClaimStatus.DRAFT)),
            Map.entry(ClaimStatus.INTIMATION_REJECTED, EnumSet.of(ClaimStatus.CLOSED)),
            Map.entry(ClaimStatus.IN_PROGRESS, EnumSet.of(ClaimStatus.QUERY_RAISED, ClaimStatus.APPROVED, ClaimStatus.REJECTED, ClaimStatus.SETTLED)),
            Map.entry(ClaimStatus.QUERY_RAISED, EnumSet.of(ClaimStatus.QUERY_RESPONDED, ClaimStatus.CLOSED)),
            Map.entry(ClaimStatus.QUERY_RESPONDED, EnumSet.of(ClaimStatus.IN_PROGRESS, ClaimStatus.QUERY_RAISED, ClaimStatus.APPROVED, ClaimStatus.REJECTED)),
            Map.entry(ClaimStatus.APPROVED, EnumSet.of(ClaimStatus.PAYMENT_PENDING, ClaimStatus.SETTLED)),
            Map.entry(ClaimStatus.PAYMENT_PENDING, EnumSet.of(ClaimStatus.SETTLED)),
            Map.entry(ClaimStatus.SETTLED, EnumSet.of(ClaimStatus.CLOSED)),
            Map.entry(ClaimStatus.REJECTED, EnumSet.of(ClaimStatus.CLOSED)),
            Map.entry(ClaimStatus.REJECTED_BY_ADMIN, EnumSet.noneOf(ClaimStatus.class)),
            Map.entry(ClaimStatus.CLOSED, EnumSet.noneOf(ClaimStatus.class))
    );

    public void validateTransition(ClaimStatus currentStatus, ClaimStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            throw new InvalidStatusTransitionException("Current status and new status must not be null");
        }
        if (currentStatus == newStatus) {
            throw new InvalidStatusTransitionException("New status must be different from current status");
        }
        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus, Set.of());
        }
        Set<ClaimStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus, allowed == null ? Set.of() : allowed);
        }
    }

    public boolean isTerminal(ClaimStatus status) {
        return status != null && TERMINAL_STATUSES.contains(status);
    }

    public Set<ClaimStatus> getAllowedTransitions(ClaimStatus currentStatus) {
        Set<ClaimStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        return allowed == null ? Set.of() : EnumSet.copyOf(allowed);
    }

    /**
     * Status-specific field requirements (e.g. SUBMITTED_TO_INSURER requires insurer_claim_ref when not manual).
     * Caller can use this to validate before transition.
     */
    public void validateTransitionRequirements(ClaimStatus newStatus, boolean hasInsurerClaimRef, boolean isManualSubmission) {
        if (newStatus == ClaimStatus.SUBMITTED_TO_INSURER && !isManualSubmission && (hasInsurerClaimRef == false)) {
            throw new InvalidStatusTransitionException(
                    "SUBMITTED_TO_INSURER requires insurer_claim_ref when submission is via API");
        }
    }
}
