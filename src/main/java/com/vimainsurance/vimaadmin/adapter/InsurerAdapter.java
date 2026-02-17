package com.vimainsurance.vimaadmin.adapter;

import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.claim.InsurerSubmissionResult;
import com.vimainsurance.vimaadmin.entity.Claim;

/**
 * Adapter for insurer integration (API or manual).
 * ManualAdapter returns requiresManualSubmission=true; API adapters submit and return result.
 */
public interface InsurerAdapter {

    /**
     * Submit claim to insurer. For manual flow, returns success with requiresManualSubmission=true.
     */
    InsurerSubmissionResult submitToInsurer(Claim claim);

    /** Whether this adapter supports the given insurer (null = generic/manual). */
    boolean supports(UUID insurerId);
}
