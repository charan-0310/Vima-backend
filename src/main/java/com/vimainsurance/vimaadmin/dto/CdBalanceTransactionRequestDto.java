package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CdBalanceTransactionRequestDto {

    @NotNull(message = "CD account ID is required")
    private UUID cdAccountId;

    private Long policyId;

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    private UUID endorsementId;

    @NotBlank(message = "Transaction type is required")
    private String transactionType;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String description;

    private String notes;

    private String referenceNumber;

    @NotBlank(message = "Source is required")
    private String source;

    private String performedBy;

    private MultipartFile[] documents;
}
