package com.vimainsurance.vimaadmin.service.claim.notification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.claim.ClaimNotification;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.ClaimQuery;
import com.vimainsurance.vimaadmin.entity.ClaimSettlement;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimsNotificationService {

    private final List<ClaimsNotificationChannel> channels;
    private final IClaimRepository claimRepository;
    private final IAdminUserRepository adminUserRepository;
    private final ClaimsNotificationDispatcher notificationDispatcher;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public void notifyStatusChange(Claim claim, ClaimStatus oldStatus, ClaimStatus newStatus) {
        Claim loaded = claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claim.getId()).orElse(claim);
        Deals employee = loaded.getEmployee();
        UUID employeeId = employee != null ? employee.getIndividualId() : null;

        List<ClaimNotification> notifications = new ArrayList<>();
        String templateId = resolveTemplateId(oldStatus, newStatus);
        String displayStatus = getDisplayStatus(newStatus);
        String message = buildStatusChangeMessage(templateId, loaded.getClaimNumber(), newStatus);

        if (employee != null && employee.getEmail() != null && !employee.getEmail().isBlank()) {
            String actionUrl = baseUrl + "/employee/claims/" + loaded.getId();
            notifications.add(ClaimNotification.builder()
                    .claimNumber(loaded.getClaimNumber())
                    .memberName(loaded.getMemberName())
                    .status(newStatus)
                    .displayStatus(displayStatus)
                    .message(message)
                    .recipientEmail(employee.getEmail())
                    .recipientName(employee.getFullName() != null ? employee.getFullName() : employee.getEmail())
                    .actionUrl(actionUrl)
                    .templateId(templateId)
                    .rejectionReason(loaded.getRejectionReason())
                    .claimAmount(loaded.getClaimAmount())
                    .hospitalName(loaded.getHospitalName())
                    .build());
        }

        if (!notifications.isEmpty()) {
            notificationDispatcher.dispatch(channels, notifications, employeeId);
        }
    }

    public void notifyQueryRaised(Claim claim, ClaimQuery query) {
        Claim loaded = claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claim.getId()).orElse(claim);
        Deals employee = loaded.getEmployee();
        if (employee == null || employee.getEmail() == null || employee.getEmail().isBlank()) return;

        String actionUrl = baseUrl + "/employee/claims/" + loaded.getId();
        ClaimNotification n = ClaimNotification.builder()
                .claimNumber(loaded.getClaimNumber())
                .memberName(loaded.getMemberName())
                .status(loaded.getInternalStatus())
                .displayStatus(getDisplayStatus(loaded.getInternalStatus()))
                .message("Insurer has raised a query on your claim. Please provide the requested information.")
                .recipientEmail(employee.getEmail())
                .recipientName(employee.getFullName() != null ? employee.getFullName() : employee.getEmail())
                .actionUrl(actionUrl)
                .templateId("claim_query_raised")
                .queryText(query.getQueryText())
                .claimAmount(loaded.getClaimAmount())
                .hospitalName(loaded.getHospitalName())
                .build();
        notificationDispatcher.dispatch(channels, List.of(n), employee.getIndividualId());
    }

    public void notifyQueryResponded(Claim claim, ClaimQuery query) {
        Claim loaded = claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claim.getId()).orElse(claim);
        Deals employee = loaded.getEmployee();
        if (employee == null || employee.getEmail() == null || employee.getEmail().isBlank()) return;

        String actionUrl = baseUrl + "/employee/claims/" + loaded.getId();
        ClaimNotification n = ClaimNotification.builder()
                .claimNumber(loaded.getClaimNumber())
                .memberName(loaded.getMemberName())
                .status(loaded.getInternalStatus())
                .displayStatus(getDisplayStatus(loaded.getInternalStatus()))
                .message("A query response has been submitted for your claim.")
                .recipientEmail(employee.getEmail())
                .recipientName(employee.getFullName() != null ? employee.getFullName() : employee.getEmail())
                .actionUrl(actionUrl)
                .templateId("claim_query_responded")
                .claimAmount(loaded.getClaimAmount())
                .hospitalName(loaded.getHospitalName())
                .build();
        notificationDispatcher.dispatch(channels, List.of(n), employee.getIndividualId());
    }

    public void notifySettlement(Claim claim, ClaimSettlement settlement) {
        Claim loaded = claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claim.getId()).orElse(claim);
        Deals employee = loaded.getEmployee();
        if (employee == null || employee.getEmail() == null || employee.getEmail().isBlank()) return;

        BigDecimal amountPaid = settlement.getAmountPaid() != null ? settlement.getAmountPaid() : BigDecimal.ZERO;
        String actionUrl = baseUrl + "/employee/claims/" + loaded.getId();
        ClaimNotification n = ClaimNotification.builder()
                .claimNumber(loaded.getClaimNumber())
                .memberName(loaded.getMemberName())
                .status(ClaimStatus.SETTLED)
                .displayStatus(getDisplayStatus(ClaimStatus.SETTLED))
                .message("Your claim has been settled. Amount paid: " + amountPaid)
                .recipientEmail(employee.getEmail())
                .recipientName(employee.getFullName() != null ? employee.getFullName() : employee.getEmail())
                .actionUrl(actionUrl)
                .templateId("claim_settled")
                .amountPaid(amountPaid)
                .claimAmount(loaded.getClaimAmount())
                .hospitalName(loaded.getHospitalName())
                .build();
        notificationDispatcher.dispatch(channels, List.of(n), employee.getIndividualId());
    }

    public void notifyAdminManualSubmission(Claim claim) {
        Claim loaded = claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claim.getId()).orElse(claim);
        UUID employeeId = loaded.getEmployee() != null ? loaded.getEmployee().getIndividualId() : null;

        List<ClaimNotification> notifications = new ArrayList<>();
        adminUserRepository.findByIsActiveTrue().stream()
                .filter(admin -> admin.getEmail() != null && !admin.getEmail().isBlank())
                .forEach(admin -> notifications.add(ClaimNotification.builder()
                        .claimNumber(loaded.getClaimNumber())
                        .memberName(loaded.getMemberName())
                        .status(loaded.getInternalStatus())
                        .displayStatus(getDisplayStatus(loaded.getInternalStatus()))
                        .message("Claim " + loaded.getClaimNumber() + " is ready for manual submission to the insurer.")
                        .recipientEmail(admin.getEmail())
                        .recipientName(admin.getFullName())
                        .actionUrl(baseUrl + "/admin/claims/" + loaded.getId())
                        .templateId("claim_manual_submission_admin")
                        .claimAmount(loaded.getClaimAmount())
                        .hospitalName(loaded.getHospitalName())
                        .build()));

        if (!notifications.isEmpty()) {
            notificationDispatcher.dispatch(channels, notifications, employeeId);
        }
    }

    private String resolveTemplateId(ClaimStatus oldStatus, ClaimStatus newStatus) {
        if (oldStatus == ClaimStatus.DRAFT && newStatus == ClaimStatus.PENDING_REVIEW) return "claim_submitted";
        if (oldStatus == ClaimStatus.PENDING_REVIEW && newStatus == ClaimStatus.INFO_REQUESTED) return "claim_info_requested";
        if (oldStatus == ClaimStatus.PENDING_REVIEW && newStatus == ClaimStatus.APPROVED_FOR_SUBMISSION) return "claim_approved_for_submission";
        if (oldStatus == ClaimStatus.PENDING_REVIEW && newStatus == ClaimStatus.REJECTED_BY_ADMIN) return "claim_rejected_by_admin";
        if (oldStatus == ClaimStatus.APPROVED_FOR_SUBMISSION && newStatus == ClaimStatus.SUBMITTED_TO_INSURER) return "claim_submitted_to_insurer";
        if (newStatus == ClaimStatus.APPROVED) return "claim_approved";
        if (newStatus == ClaimStatus.REJECTED) return "claim_rejected";
        return "claim_status_changed";
    }

    private String buildStatusChangeMessage(String templateId, String claimNumber, ClaimStatus newStatus) {
        return switch (templateId) {
            case "claim_submitted" -> "Your claim " + (claimNumber != null ? claimNumber : "") + " has been submitted.";
            case "claim_approved_for_submission" -> "Your claim has been approved and will be sent to the insurer.";
            case "claim_info_requested" -> "Additional information is needed for claim " + (claimNumber != null ? claimNumber : "") + ".";
            case "claim_rejected_by_admin" -> "Your claim " + (claimNumber != null ? claimNumber : "") + " has been rejected.";
            case "claim_submitted_to_insurer" -> "Your claim has been submitted to the insurer.";
            case "claim_approved" -> "Your claim has been approved by the insurer.";
            case "claim_rejected" -> "Your claim has been rejected by the insurer.";
            default -> "Your claim status has been updated to " + getDisplayStatus(newStatus) + ".";
        };
    }

    private static String getDisplayStatus(ClaimStatus status) {
        if (status == null) return "";
        return switch (status) {
            case DRAFT -> "Draft";
            case PENDING_REVIEW -> "Pending Review";
            case INFO_REQUESTED -> "Info Requested";
            case APPROVED_FOR_SUBMISSION -> "Approved for Submission";
            case SUBMITTED_TO_INSURER -> "Submitted to Insurer";
            case SUBMISSION_FAILED -> "Submission Failed";
            case INTIMATION_REJECTED -> "Intimation Rejected";
            case IN_PROGRESS -> "In Progress";
            case QUERY_RAISED -> "Query Raised";
            case QUERY_RESPONDED -> "Query Responded";
            case APPROVED -> "Approved";
            case PAYMENT_PENDING -> "Payment Pending";
            case SETTLED -> "Settled";
            case REJECTED -> "Rejected";
            case REJECTED_BY_ADMIN -> "Rejected by Admin";
            case CLOSED -> "Closed";
        };
    }
}
