package com.vimainsurance.vimaadmin.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.entity.CostSharingRule;
import com.vimainsurance.vimaadmin.repository.ICostSharingRuleRepository;

import lombok.RequiredArgsConstructor;

/**
 * In-memory cache for cost-sharing rules keyed by company ID. TTL 1 hour; invalidate on mutation.
 */
@Service
@RequiredArgsConstructor
public class CostSharingRuleCacheService {

    private static final Logger log = LoggerFactory.getLogger(CostSharingRuleCacheService.class);
    private static final long TTL_MILLIS = 60 * 60 * 1000L; // 1 hour

    private final ICostSharingRuleRepository repository;

    private final ConcurrentHashMap<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    private static final class CacheEntry {
        final List<CostSharingRule> rules;
        final long loadedAtMillis;

        CacheEntry(List<CostSharingRule> rules) {
            this.rules = rules;
            this.loadedAtMillis = System.currentTimeMillis();
        }

        boolean isStale(long ttlMillis) {
            return System.currentTimeMillis() - loadedAtMillis >= ttlMillis;
        }
    }

    public List<CostSharingRule> getRulesForCompany(UUID companyId) {
        CacheEntry entry = cache.get(companyId);
        if (entry != null && !entry.isStale(TTL_MILLIS)) {
            return entry.rules;
        }
        List<CostSharingRule> rules = repository.findByCompanyId(companyId);
        cache.put(companyId, new CacheEntry(rules));
        return rules;
    }

    public void invalidate(UUID companyId) {
        cache.remove(companyId);
        log.debug("Cost-sharing rule cache invalidated for company {}", companyId);
    }

    @Scheduled(fixedRate = 3600000, initialDelay = 3600000)
    public void refreshAll() {
        cache.clear();
        log.debug("Cost-sharing rule cache cleared (hourly refresh)");
    }
}
