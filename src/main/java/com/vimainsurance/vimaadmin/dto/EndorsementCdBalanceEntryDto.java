package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EndorsementCdBalanceEntryDto {

    @NotNull(message = "Policy ID is required")
    private Long policyId;

    @NotNull(message = "CD amount is required")
    private BigDecimal amount;

    private String transactionType;

    private String description;

    private String notes;

    private String referenceNumber;

    private MultipartFile[] documents;
}
