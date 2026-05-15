package com.vimainsurance.vimaadmin.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parse and pair Top-Up / Super Top-Up sum-insured tiers with admin-entered annual premiums (index-aligned JSON arrays on {@code policy}).
 */
public final class TopupPremiumOptionsUtil {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final TypeReference<List<BigDecimal>> LIST_BIG_DECIMAL = new TypeReference<>() {};

    private TopupPremiumOptionsUtil() {
    }

    /** Parse JSON array string or comma / semicolon separated numbers. */
    public static List<BigDecimal> parseDecimalList(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        String t = input.trim();
        if (t.startsWith("[")) {
            try {
                List<BigDecimal> fromJson = OM.readValue(t, LIST_BIG_DECIMAL);
                return fromJson != null ? new ArrayList<>(fromJson) : new ArrayList<>();
            } catch (Exception e) {
                return List.of();
            }
        }
        return Arrays.stream(t.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(BigDecimal::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Find premium for the given sum insured when both policy JSON arrays exist and have equal length.
     */
    public static Optional<BigDecimal> findPremiumForSumInsured(
            String sumInsuredOptionsJson,
            String topupPremiumOptionsJson,
            BigDecimal sumInsured) {
        if (sumInsured == null || topupPremiumOptionsJson == null || topupPremiumOptionsJson.isBlank()) {
            return Optional.empty();
        }
        List<BigDecimal> si = parseDecimalList(sumInsuredOptionsJson);
        List<BigDecimal> pr = parseDecimalList(topupPremiumOptionsJson);
        if (si.isEmpty() || pr.size() != si.size()) {
            return Optional.empty();
        }
        for (int i = 0; i < si.size(); i++) {
            if (si.get(i).compareTo(sumInsured) == 0) {
                return Optional.of(pr.get(i));
            }
        }
        return Optional.empty();
    }

    public static List<BigDecimal> safeParseOrEmpty(String json) {
        List<BigDecimal> list = parseDecimalList(json);
        return list != null ? list : Collections.emptyList();
    }

    /** Build ordered SI->premium map only when both arrays are non-empty and equal-sized. */
    public static Map<BigDecimal, BigDecimal> buildPreviewMap(String sumInsuredOptionsJson, String topupPremiumOptionsJson) {
        List<BigDecimal> si = parseDecimalList(sumInsuredOptionsJson);
        List<BigDecimal> pr = parseDecimalList(topupPremiumOptionsJson);
        if (si.isEmpty() || pr.isEmpty() || si.size() != pr.size()) {
            return Map.of();
        }
        Map<BigDecimal, BigDecimal> out = new LinkedHashMap<>();
        for (int i = 0; i < si.size(); i++) {
            out.put(si.get(i), pr.get(i));
        }
        return out;
    }

    /**
     * Smallest strictly positive annual premium from configured top-up tiers ({@code topup_premium_options}).
     * Used when {@code premium_amount} is zero so policy overview can show a meaningful inception baseline.
     */
    public static Optional<BigDecimal> minPositiveTierPremium(String topupPremiumOptionsJson) {
        return parseDecimalList(topupPremiumOptionsJson).stream()
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo);
    }
}
