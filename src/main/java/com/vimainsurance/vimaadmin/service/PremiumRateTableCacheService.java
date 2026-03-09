package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.entity.PremiumRateTable;
import com.vimainsurance.vimaadmin.repository.IPremiumRateTableRepository;

import lombok.RequiredArgsConstructor;

/**
 * In-memory cache for premium rate tables keyed by company (organization) ID.
 * TTL 1 hour; invalidate on mutation; optional hourly refresh.
 */
@Service
@RequiredArgsConstructor
public class PremiumRateTableCacheService {

    private static final Logger log = LoggerFactory.getLogger(PremiumRateTableCacheService.class);
    private static final long TTL_MILLIS = 60 * 60 * 1000L; // 1 hour

    private final IPremiumRateTableRepository premiumRateTableRepository;

    private final ConcurrentHashMap<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    private static final class CacheEntry {
        final List<PremiumRateTable> rates;
        final long loadedAtMillis;

        CacheEntry(List<PremiumRateTable> rates) {
            this.rates = rates;
            this.loadedAtMillis = System.currentTimeMillis();
        }

        boolean isStale(long ttlMillis) {
            return System.currentTimeMillis() - loadedAtMillis >= ttlMillis;
        }
    }

    /**
     * Returns rate tables for the company. Loads from DB if absent or stale (older than TTL).
     */
    public List<PremiumRateTable> getRatesForCompany(UUID companyId) {
        CacheEntry entry = cache.get(companyId);
        if (entry != null && !entry.isStale(TTL_MILLIS)) {
            return entry.rates;
        }
        List<PremiumRateTable> rates = premiumRateTableRepository.findByOrganizationId(companyId);
        cache.put(companyId, new CacheEntry(rates));
        return rates;
    }

    /**
     * Invalidates cache for the given company so next get refetches from DB.
     */
    public void invalidate(UUID companyId) {
        cache.remove(companyId);
        log.debug("Premium rate cache invalidated for company {}", companyId);
    }

    /**
     * Hourly refresh: clear cache so subsequent gets reload from DB.
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 3600000)
    public void refreshAll() {
        cache.clear();
        log.debug("Premium rate cache cleared (hourly refresh)");
    }
}
