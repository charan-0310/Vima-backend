package com.vimainsurance.vimaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.vimainsurance.vimaadmin.util.ZohoSyncStrategy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "zoho.sync")
public class ZohoSyncConfig {
    
    private ZohoSyncStrategy conflictStrategy = ZohoSyncStrategy.ZOHO_FIRST;
    private boolean enableBidirectionalSync = false;
    private int batchSize = 100;
    private int retryAttempts = 3;
    private long retryDelayMs = 1000;
} 