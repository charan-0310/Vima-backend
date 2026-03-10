package com.vimainsurance.vimaadmin.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.service.PremiumRateTableCacheService;

import lombok.extern.slf4j.Slf4j;

/**
 * Hourly job: refreshes premium rate table cache so subsequent requests get fresh data from DB.
 * Delegates to PremiumRateTableCacheService.refreshAll().
 */
@Component
@Slf4j
public class PremiumRateTableCacheRefresherJob {

    @Autowired
    private PremiumRateTableCacheService premiumRateTableCacheService;

    @Scheduled(fixedRate = 3600000, initialDelay = 3600000)
    public void run() {
        try {
            premiumRateTableCacheService.refreshAll();
            log.debug("PremiumRateTableCacheRefresherJob: cache refreshed");
        } catch (Exception e) {
            log.warn("PremiumRateTableCacheRefresherJob: refresh failed: {}", e.getMessage());
        }
    }
}
