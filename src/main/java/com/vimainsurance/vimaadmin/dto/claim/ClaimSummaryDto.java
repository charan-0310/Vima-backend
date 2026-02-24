package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;

import lombok.Data;

@Data
public class ClaimSummaryDto {

    private UUID id;
    private String claimNumber;
    private String memberName;
    private String hospitalName;
    private BigDecimal claimAmount;
    private ClaimStatus status;
    private LocalDate dateOfSubmission;
}
