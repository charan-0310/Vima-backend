package com.vimainsurance.vimaadmin.exception;

import java.util.Set;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;

/**
 * Thrown when an invalid claim status transition is attempted.
 * Includes the allowed target statuses for the current status.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final ClaimStatus currentStatus;
    private final ClaimStatus requestedStatus;
    private final Set<ClaimStatus> allowedTransitions;

    public InvalidStatusTransitionException(ClaimStatus currentStatus, ClaimStatus requestedStatus,
            Set<ClaimStatus> allowedTransitions) {
        super(String.format(
                "Invalid claim status transition from %s to %s. Allowed transitions from %s: %s",
                currentStatus, requestedStatus, currentStatus,
                allowedTransitions == null ? "none" : allowedTransitions.stream()
                        .map(ClaimStatus::getValue)
                        .collect(Collectors.joining(", "))));
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
        this.allowedTransitions = allowedTransitions;
    }

    public InvalidStatusTransitionException(String message) {
        super(message);
        this.currentStatus = null;
        this.requestedStatus = null;
        this.allowedTransitions = null;
    }

    public ClaimStatus getCurrentStatus() {
        return currentStatus;
    }

    public ClaimStatus getRequestedStatus() {
        return requestedStatus;
    }

    public Set<ClaimStatus> getAllowedTransitions() {
        return allowedTransitions;
    }
}
