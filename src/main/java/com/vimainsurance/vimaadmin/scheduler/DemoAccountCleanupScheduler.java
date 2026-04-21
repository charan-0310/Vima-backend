package com.vimainsurance.vimaadmin.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.ActionSource;
import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.util.KeyCloakUtil;

@Component
@ConditionalOnProperty(
        prefix = "demo.cleanup-scheduler",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class DemoAccountCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DemoAccountCleanupScheduler.class);

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Autowired
    private KeyCloakUtil keyCloakUtil;

    @Autowired
    private IEmailService emailService;

    @Value("${demo.internal-notification-email:team@vimainsurance.com}")
    private String internalNotificationEmail;

    @Value("${app.base-url:https://staging.app.vimainsurance.com}")
    private String appBaseUrl;

    @Scheduled(cron = "${demo.cleanup-scheduler.cron-expression:0 0 0 * * *}", zone = "UTC")
    @Transactional
    public void cleanupExpiredDemoAccounts() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            AuditContextSupplier.setActionSource(ActionSource.SYSTEM);
            List<AdminUser> expiredUsers = adminUserRepository.findByIsDemoUserTrueAndIsActiveTrueAndDemoExpiresAtBefore(LocalDateTime.now());
            if (expiredUsers.isEmpty()) {
                logger.info("[correlationId:{}] No expired demo users found", correlationId);
                return;
            }

            for (AdminUser user : expiredUsers) {
                try {
                    keyCloakUtil.disableUserByEmail(user.getEmail());
                    user.setIsActive(false);
                    adminUserRepository.save(user);
                    if (user.getOrganization() != null && Boolean.TRUE.equals(user.getOrganization().getIsDemoOrg())) {
                        user.getOrganization().setDemoExpiresAt(LocalDateTime.now());
                        organizationRepository.save(user.getOrganization());
                    }
                } catch (Exception e) {
                    logger.error("[correlationId:{}] Failed to disable demo user {}: {}", correlationId, user.getEmail(), e.getMessage(), e);
                }
            }

            emailService.sendSimpleEmail(EmailRequest.builder()
                    .to(internalNotificationEmail)
                    .subject("Demo Accounts Expired: " + expiredUsers.size() + " revoked")
                    .body("Expired demo users were disabled by scheduler.")
                    .build());
        } finally {
            AuditContextSupplier.clearActionSource();
            MDC.clear();
        }
    }

    @Scheduled(cron = "${demo.expiry-warning.cron-expression:0 30 3 * * *}", zone = "UTC")
    public void sendExpiryWarnings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusHours(24);
        List<AdminUser> expiringUsers = adminUserRepository
                .findByIsDemoUserTrueAndIsActiveTrueAndDemoExpiresAtBetween(now, tomorrow);

        for (AdminUser user : expiringUsers) {
            try {
                emailService.sendTemplateEmail(EmailRequest.builder()
                        .to(user.getEmail())
                        .subject("Your Vima demo access expires tomorrow")
                        .templateName("demo-expiry-warning")
                        .templateVariables(java.util.Map.of(
                                "userFullName", user.getFullName() != null ? user.getFullName() : "User",
                                "expiresAt", user.getDemoExpiresAt() != null ? user.getDemoExpiresAt().toString() : "",
                                "baseUrl", appBaseUrl))
                        .build());
            } catch (Exception e) {
                logger.error("Failed to send expiry warning to {}: {}", user.getEmail(), e.getMessage(), e);
            }
        }
    }
}
