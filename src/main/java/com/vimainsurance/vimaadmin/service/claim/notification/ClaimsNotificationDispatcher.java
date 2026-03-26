package com.vimainsurance.vimaadmin.service.claim.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.claim.ClaimNotification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClaimsNotificationDispatcher {

    @Async
    public void dispatch(List<ClaimsNotificationChannel> channels, List<ClaimNotification> notifications, UUID employeeIdForEnabled) {
        try {
            for (ClaimNotification notification : notifications) {
                for (ClaimsNotificationChannel channel : channels) {
                    try {
                        if (employeeIdForEnabled == null || channel.isEnabled(employeeIdForEnabled)) {
                            channel.send(notification);
                        }
                    } catch (Exception e) {
                        log.warn("Claim notification channel failed for claim {}: {}", notification.getClaimNumber(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Claim notification send failed: {}", e.getMessage(), e);
        }
    }
}
