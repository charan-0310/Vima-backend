package com.vimainsurance.vimaadmin.util;

/**
 * Constants for quote status values
 */
public final class QuoteStatusConstants {
    
    private QuoteStatusConstants() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Initial status when a quote is first created
     */
    public static final String ISSUED = "Shared";
    
    /**
     * Status when the quote is under review
     */
    public static final String IN_REVIEW = "Under Review";
    
    /**
     * Status when the quote has been accepted
     */
    public static final String ACCEPTED = "Accepted";
    
    /**
     * Status when the quote has been declined
     */
    public static final String DECLINED = "Declined";
    
    /**
     * Legacy status value - maps to ISSUED
     */
    public static final String INITIATED = "Initiated";
    
    /**
     * Get the normalized status value, handling legacy values
     * @param status the status string to normalize
     * @return the normalized status or ISSUED as default
     */
    public static String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ISSUED;
        }
        
        String normalizedStatus = status.trim();
        
        // Handle legacy status values
        if (INITIATED.equalsIgnoreCase(normalizedStatus) || 
            INITIATED.equals(normalizedStatus)) {
            return ISSUED;
        }
        
        // Return the status if it's one of our valid constants
        if (ISSUED.equals(normalizedStatus) || 
            IN_REVIEW.equals(normalizedStatus) || 
            ACCEPTED.equals(normalizedStatus) || 
            DECLINED.equals(normalizedStatus)) {
            return normalizedStatus;
        }
        
        // Default to ISSUED for unknown statuses
        return ISSUED;
    }
    
    /**
     * Check if a status transition is valid
     * @param fromStatus the current status
     * @param toStatus the target status
     * @return true if the transition is valid
     */
    public static boolean isValidTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            return false;
        }
        
        String normalizedFrom = normalizeStatus(fromStatus);
        String normalizedTo = normalizeStatus(toStatus);
        
        // Define valid transitions
        switch (normalizedFrom) {
            case ISSUED:
                return IN_REVIEW.equals(normalizedTo);
            case IN_REVIEW:
                return ACCEPTED.equals(normalizedTo) || DECLINED.equals(normalizedTo);
            case ACCEPTED:
            case DECLINED:
                // Final states - no further transitions allowed
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Get all valid status values
     * @return array of valid status strings
     */
    public static String[] getValidStatuses() {
        return new String[]{ISSUED, IN_REVIEW, ACCEPTED, DECLINED};
    }
}
