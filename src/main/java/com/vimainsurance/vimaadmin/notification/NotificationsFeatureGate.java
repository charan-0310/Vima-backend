package com.vimainsurance.vimaadmin.notification;

import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.repository.IFeatureFlagRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationsFeatureGate {

    public static final String FLAG_KEY = "notifications.enabled";

    private final IFeatureFlagRepository featureFlagRepository;

    /**
     * Notifications are enabled by default.
     * If a feature-flag row is present, its value can still explicitly disable notifications.
     */
    public boolean isNotificationsEnabled() {
        return featureFlagRepository.findByFlagKey(FLAG_KEY)
                .map(f -> Boolean.TRUE.equals(f.getIsActive()))
                .orElse(true);
    }
}
