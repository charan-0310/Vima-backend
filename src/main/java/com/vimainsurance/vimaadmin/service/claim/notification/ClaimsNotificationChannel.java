package com.vimainsurance.vimaadmin.service.claim.notification;

import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.claim.ClaimNotification;

/**
 * Phase 2-ready channel for claim notifications (email, future WhatsApp, etc.).
 * Implementations handle their own transport; the service does not depend on email.
 */
public interface ClaimsNotificationChannel {

    /**
     * Send the notification to the recipient(s) implied by the DTO.
     */
    void send(ClaimNotification notification);

    /**
     * Whether this channel is enabled for the given employee (e.g. Phase 1: email always true; Phase 2: WhatsApp per-employee).
     */
    boolean isEnabled(UUID employeeId);
}
