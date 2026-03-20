package com.vimainsurance.vimaadmin.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TopupPremiumOptionsUtilTest {

    @Test
    void buildPreviewMap_withAlignedArrays_preservesIndexMapping() {
        Map<BigDecimal, BigDecimal> map = TopupPremiumOptionsUtil.buildPreviewMap(
                "[5000,6000,7000]",
                "[300,400,500]");

        assertEquals(3, map.size());
        assertEquals(BigDecimal.valueOf(300), map.get(BigDecimal.valueOf(5000)));
        assertEquals(BigDecimal.valueOf(400), map.get(BigDecimal.valueOf(6000)));
        assertEquals(BigDecimal.valueOf(500), map.get(BigDecimal.valueOf(7000)));
    }

    @Test
    void findPremiumForSumInsured_returnsMatchedPremiumFromIndexPair() {
        Optional<BigDecimal> premium = TopupPremiumOptionsUtil.findPremiumForSumInsured(
                "[5000,6000,7000]",
                "[300,400,500]",
                BigDecimal.valueOf(6000));

        assertTrue(premium.isPresent());
        assertEquals(BigDecimal.valueOf(400), premium.get());
    }

    @Test
    void buildPreviewMap_withMismatchedCounts_returnsEmptyMap() {
        Map<BigDecimal, BigDecimal> map = TopupPremiumOptionsUtil.buildPreviewMap(
                "[5000,6000,7000]",
                "[300,400]");

        assertTrue(map.isEmpty());
    }
}
