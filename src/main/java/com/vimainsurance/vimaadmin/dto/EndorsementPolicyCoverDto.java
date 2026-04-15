package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndorsementPolicyCoverDto {

    private Long policyId;
    private String policyNumber;
    private String productType;
    private String insurerName;
    private BigDecimal sumInsured;
    private String coverageTier;
    private Boolean isVoluntary;
    private String status;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String source;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
}
