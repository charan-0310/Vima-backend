package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndorsementSplitSummaryDto {
    private UUID endorsementId;
    private Long policyId;
    private String policyType;
    private Integer employeeCount;
    private Integer dependentCount;
    private UUID splitGroupId;
    private UUID parentEndorsementId;
}
