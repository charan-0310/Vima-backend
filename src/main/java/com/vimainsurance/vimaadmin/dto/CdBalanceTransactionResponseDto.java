package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class CdBalanceTransactionResponseDto {

    private UUID transactionId;
    private UUID cdAccountId;
    private Long policyId;
    private UUID organizationId;
    private UUID endorsementId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal runningBalance;
    private String description;
    private String notes;
    private String referenceNumber;
    private String source;
    private String performedBy;
    private List<UUID> documentIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
