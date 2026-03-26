package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class CdBalanceResponseDto {

    private Long policyId;
    private UUID organizationId;
    private BigDecimal cdBalance;
    private LocalDateTime updatedAt;
}
