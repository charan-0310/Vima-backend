package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDeductionDto {
    private UUID id;
    private String deductionDetails;
    private BigDecimal deductionAmount;
    private LocalDateTime createdAt;
}
