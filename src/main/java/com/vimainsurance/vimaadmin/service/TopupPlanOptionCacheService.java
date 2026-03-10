package com.vimainsurance.vimaadmin.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.entity.TopupPlanOption;
import com.vimainsurance.vimaadmin.repository.ITopupPlanOptionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopupPlanOptionCacheService {

    private static final long TTL_SECONDS = 3600; // 1 hour

    private final ITopupPlanOptionRepository repository;
    private final ConcurrentHashMap<UUID, CacheEntry<List<TopupPlanOption>>> cache = new ConcurrentHashMap<>();

    public List<TopupPlanOption> getActiveOptionsForCompany(UUID companyId) {
        CacheEntry<List<TopupPlanOption>> entry = cache.get(companyId);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        List<TopupPlanOption> list = repository.findByCompanyIdAndIsActiveTrue(companyId);
        cache.put(companyId, new CacheEntry<>(list, Instant.now().plusSeconds(TTL_SECONDS)));
        return list;
    }

    public void invalidate(UUID companyId) {
        cache.remove(companyId);
        log.debug("Invalidated topup options cache for company {}", companyId);
    }

    @Scheduled(fixedRate = 3600000)
    public void refreshAll() {
        cache.clear();
        log.debug("Cleared topup options cache");
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
