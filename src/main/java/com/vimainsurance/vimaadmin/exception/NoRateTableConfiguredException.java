package com.vimainsurance.vimaadmin.exception;

import java.util.UUID;

/**
 * Thrown when no effective premium rate row exists for the requested company and plan.
 * Mapped to HTTP 422 with {@code errorKey = NO_RATE_TABLE_CONFIGURED} for API clients.
 */
public class NoRateTableConfiguredException extends RuntimeException {

    private final UUID companyId;
    private final String planType;

    public NoRateTableConfiguredException(UUID companyId, String planType) {
        super("No effective rate card row for company " + companyId + ", plan " + planType);
        this.companyId = companyId;
        this.planType = planType;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getPlanType() {
        return planType;
    }
}
