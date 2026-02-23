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
     * Returns the adapter for the claim's insurer, or ManualAdapter when insurerId is null or no match.
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

    /**
     * Returns the adapter for the given insurer ID string (e.g. "ICICI_LOMBARD"), or ManualAdapter if none matches.
     */
    public InsurerAdapter getAdapter(String insurerId) {
        if (insurerId != null && !insurerId.isBlank()) {
            for (InsurerAdapter adapter : adapters) {
                if (insurerId.equals(adapter.getInsurerId())) {
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
        if (!adapters.isEmpty()) {
            return adapters.get(0);
        }
        throw new IllegalStateException("No InsurerAdapter beans found. Ensure ManualInsurerAdapter is registered.");
    }
}
