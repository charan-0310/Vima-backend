package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for claim notifications (email, future WhatsApp). Contains all data needed by templates and channels.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimNotification {

    private String claimNumber;
    private String memberName;
    private ClaimStatus status;
    private String displayStatus;
    private String message;

    private String recipientEmail;
    private String recipientName;

    private String actionUrl;
    private String templateId;

    private String rejectionReason;
    private String queryText;
    private BigDecimal amountPaid;
    private BigDecimal claimAmount;
    private String hospitalName;
}
