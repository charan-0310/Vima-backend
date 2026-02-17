package com.vimainsurance.vimaadmin.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.entity.Claim;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InsurerAdapterFactory {

    private final List<InsurerAdapter> adapters;

    /**
     * Returns the adapter for the claim's insurer, or ManualAdapter when insurerId is null.
     */
    public InsurerAdapter getAdapter(Claim claim) {
        UUID insurerId = claim.getInsurerId();
        if (insurerId != null) {
            for (InsurerAdapter adapter : adapters) {
                if (adapter.supports(insurerId)) {
                    return adapter;
                }
            }
        }
        return getManualAdapter();
    }

    private InsurerAdapter getManualAdapter() {
        for (InsurerAdapter adapter : adapters) {
            if (adapter instanceof ManualInsurerAdapter) {
                return adapter;
            }
        }
        throw new IllegalStateException("ManualInsurerAdapter not found");
    }
}
