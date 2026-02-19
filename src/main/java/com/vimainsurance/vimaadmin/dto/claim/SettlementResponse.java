package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {
    private UUID id;
    private ClaimStatus claimStatus;
    private BigDecimal amountPaid;
}
