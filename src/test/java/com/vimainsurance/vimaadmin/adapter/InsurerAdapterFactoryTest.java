package com.vimainsurance.vimaadmin.adapter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vimainsurance.vimaadmin.entity.Claim;

class InsurerAdapterFactoryTest {

    @Test
    void getAdapter_claimWithNullInsurerId_returnsManualAdapter() {
        ManualInsurerAdapter manual = new ManualInsurerAdapter();
        InsurerAdapterFactory factory = new InsurerAdapterFactory(List.of(manual));

        Claim claim = new Claim();
        claim.setInsurerId(null);

        InsurerAdapter adapter = factory.getAdapter(claim);
        assertNotNull(adapter);
        assertSame(manual, adapter);
    }

    @Test
    void getAdapter_claimWithUnknownInsurerId_returnsManualAdapter() {
        ManualInsurerAdapter manual = new ManualInsurerAdapter();
        InsurerAdapterFactory factory = new InsurerAdapterFactory(List.of(manual));

        Claim claim = new Claim();
        claim.setInsurerId(UUID.randomUUID());

        InsurerAdapter adapter = factory.getAdapter(claim);
        assertNotNull(adapter);
        assertSame(manual, adapter);
    }

    @Test
    void getAdapter_stringNull_returnsManualAdapter() {
        ManualInsurerAdapter manual = new ManualInsurerAdapter();
        InsurerAdapterFactory factory = new InsurerAdapterFactory(List.of(manual));

        InsurerAdapter adapter = factory.getAdapter((String) null);
        assertNotNull(adapter);
        assertSame(manual, adapter);
    }

    @Test
    void getAdapter_stringBlank_returnsManualAdapter() {
        ManualInsurerAdapter manual = new ManualInsurerAdapter();
        InsurerAdapterFactory factory = new InsurerAdapterFactory(List.of(manual));

        InsurerAdapter adapter = factory.getAdapter("   ");
        assertNotNull(adapter);
        assertSame(manual, adapter);
    }

    @Test
    void getAdapter_stringUnknown_returnsManualAdapter() {
        ManualInsurerAdapter manual = new ManualInsurerAdapter();
        InsurerAdapterFactory factory = new InsurerAdapterFactory(List.of(manual));

        InsurerAdapter adapter = factory.getAdapter("ICICI_LOMBARD");
        assertNotNull(adapter);
        assertSame(manual, adapter);
    }

    @Test
    void getAdapter_stringManual_returnsManualAdapter() {
        ManualInsurerAdapter manual = new ManualInsurerAdapter();
        InsurerAdapterFactory factory = new InsurerAdapterFactory(List.of(manual));

        InsurerAdapter adapter = factory.getAdapter("MANUAL");
        assertNotNull(adapter);
        assertSame(manual, adapter);
    }
}
