package com.vimainsurance.vimaadmin.service.claim.notification;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.claim.ClaimNotification;
import com.vimainsurance.vimaadmin.service.IEmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements ClaimsNotificationChannel {

    private static final String COMPANY_NAME = "Vima Insurance";

    private final IEmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public void send(ClaimNotification notification) {
        if (notification == null || notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            log.warn("Claim notification skipped: missing recipient email");
            return;
        }
        try {
            String subject = buildSubject(notification);
            Map<String, Object> variables = buildTemplateVariables(notification);
            variables.put("companyName", COMPANY_NAME);
            variables.put("baseUrl", baseUrl);

            EmailRequest req = EmailRequest.builder()
                    .to(notification.getRecipientEmail())
                    .subject(subject)
                    .templateName(notification.getTemplateId())
                    .templateVariables(variables)
                    .isHtml(true)
                    .build();

            var response = emailService.sendTemplateEmail(req);
            if (!response.isSuccess()) {
                log.warn("Claim notification email failed for claim {} to {}: {}", 
                        notification.getClaimNumber(), notification.getRecipientEmail(), response.getMessage());
            }
        } catch (Exception e) {
            log.warn("Failed to send claim notification for claim {} to {}: {}", 
                    notification.getClaimNumber(), notification.getRecipientEmail(), e.getMessage(), e);
        }
    }

    @Override
    public boolean isEnabled(UUID employeeId) {
        return true;
    }

    private String buildSubject(ClaimNotification n) {
        String cn = n.getClaimNumber() != null ? n.getClaimNumber() : "Claim";
        return switch (n.getTemplateId() != null ? n.getTemplateId() : "") {
            case "claim_submitted" -> "Your claim " + cn + " has been submitted";
            case "claim_approved_for_submission" -> "Your claim has been approved for submission to insurer";
            case "claim_info_requested" -> "Additional information needed for claim " + cn;
            case "claim_rejected_by_admin" -> "Your claim " + cn + " has been rejected";
            case "claim_submitted_to_insurer" -> "Your claim has been submitted to the insurer";
            case "claim_status_changed" -> "Claim " + cn + " – status update";
            case "claim_query_raised" -> "Insurer has raised a query on your claim " + cn;
            case "claim_query_responded" -> "Query response submitted for your claim " + cn;
            case "claim_approved" -> "Your claim " + cn + " has been approved by the insurer";
            case "claim_rejected" -> "Your claim " + cn + " has been rejected by the insurer";
            case "claim_settled" -> "Your claim " + cn + " has been settled";
            case "claim_manual_submission_admin" -> "Claim " + cn + " ready for manual submission to insurer";
            default -> "Claim " + cn + " – update";
        };
    }

    private Map<String, Object> buildTemplateVariables(ClaimNotification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("claimNumber", n.getClaimNumber());
        map.put("memberName", n.getMemberName());
        map.put("status", n.getStatus() != null ? n.getStatus().getValue() : null);
        map.put("displayStatus", n.getDisplayStatus());
        map.put("message", n.getMessage());
        map.put("recipientName", n.getRecipientName());
        map.put("actionUrl", n.getActionUrl());
        map.put("rejectionReason", n.getRejectionReason());
        map.put("queryText", n.getQueryText());
        map.put("amountPaid", n.getAmountPaid());
        map.put("claimAmount", n.getClaimAmount());
        map.put("hospitalName", n.getHospitalName());
        if (n.getAmountPaid() != null) {
            map.put("amountPaidFormatted", n.getAmountPaid().toString());
        }
        if (n.getClaimAmount() != null) {
            map.put("claimAmountFormatted", n.getClaimAmount().toString());
        }
        map.put("statusBadgeColor", getStatusBadgeColor(n.getStatus()));
        return map;
    }

    private static String getStatusBadgeColor(com.vimainsurance.vimaadmin.enums.ClaimStatus status) {
        if (status == null) return "#e2e8f0";
        return switch (status) {
            case APPROVED, SETTLED -> "#dcfce7";
            case REJECTED, REJECTED_BY_ADMIN, INTIMATION_REJECTED -> "#fee2e2";
            case PENDING_REVIEW, INFO_REQUESTED, QUERY_RAISED, QUERY_RESPONDED, IN_PROGRESS, PAYMENT_PENDING -> "#fef3c7";
            case APPROVED_FOR_SUBMISSION, SUBMITTED_TO_INSURER -> "#dbeafe";
            default -> "#e2e8f0";
        };
    }
}
