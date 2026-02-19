package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRequest {
    private BigDecimal grossSanctionedAmount;
    private BigDecimal netSanctionedAmount;
    private BigDecimal totalDisallowedAmount;
    private BigDecimal deductionAmount;
    private BigDecimal copayAmount;
    private BigDecimal amountPaid;
    private String paymentMode;
    private String chequeNumber;
    private LocalDate chequeDate;
    private LocalDate paymentDate;
    private String paymentReference;
    private String insurerRemarks;
}
