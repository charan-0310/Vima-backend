package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class EndorsementCdBalanceEntryDto {

    @NotNull(message = "CD account ID is required")
    private UUID cdAccountId;

    private Long policyId;

    @NotNull(message = "CD amount is required")
    @JsonAlias("cdAmount")
    private BigDecimal amount;

    private String transactionType;

    private String description;

    private String notes;

    private String referenceNumber;

    private MultipartFile[] documents;
}
