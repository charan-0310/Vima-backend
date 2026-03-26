package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class CdAccountResponseDto {
    private UUID cdAccountId;
    private UUID organizationId;
    private String insurerName;
    private String label;
    private BigDecimal cdBalance;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
