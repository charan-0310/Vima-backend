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
     * Global backend gate: feature flag row must exist and {@code is_active = true}.
     * Per-user/org flag resolution is not applied for system-driven emits (scheduler, employee submit).
     */
    public boolean isNotificationsEnabled() {
        return featureFlagRepository.findByFlagKey(FLAG_KEY)
                .map(f -> Boolean.TRUE.equals(f.getIsActive()))
                .orElse(false);
    }
}
