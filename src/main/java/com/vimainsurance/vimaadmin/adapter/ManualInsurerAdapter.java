package com.vimainsurance.vimaadmin.adapter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.dto.claim.InsurerSubmissionResult;
import com.vimainsurance.vimaadmin.entity.Claim;

import java.util.UUID;

/**
 * Adapter for manual submission path: no API call, returns requiresManualSubmission=true.
 */
@Component
@Order(100)
public class ManualInsurerAdapter implements InsurerAdapter {

    @Override
    public InsurerSubmissionResult submitToInsurer(Claim claim) {
        return InsurerSubmissionResult.builder()
                .success(true)
                .requiresManualSubmission(true)
                .message("Claim approved for manual submission to insurer")
                .build();
    }

    @Override
    public boolean supports(UUID insurerId) {
        return false; // used as fallback when no insurer-specific adapter matches
    }
}
