package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.service.IEmailService;

@Service
public class DemoSetupEmailNotifier {

    private static final Logger logger = LoggerFactory.getLogger(DemoSetupEmailNotifier.class);

    @Autowired
    private IEmailService emailService;

    @Value("${app.base-url:https://staging.app.vimainsurance.com}")
    private String appBaseUrl;

    @Async
    public void sendDemoWelcomeEmailAsync(String email, String orgName, String tempPassword, LocalDateTime expiresAt) {
        try {
            emailService.sendTemplateEmail(EmailRequest.builder()
                    .to(email)
                    .subject("Your Vima Insurance Demo Access")
                    .templateName("demo-welcome")
                    .templateVariables(Map.of(
                            "userFullName", "Demo User",
                            "username", email,
                            "temporaryPassword", tempPassword,
                            "orgName", orgName,
                            "expiresAt", expiresAt.toString(),
                            "baseUrl", appBaseUrl))
                    .build());
        } catch (Exception e) {
            logger.warn("Failed to send demo welcome email asynchronously to {}: {}", email, e.getMessage());
        }
    }
}
