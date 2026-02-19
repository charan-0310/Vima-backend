package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCoverageResult {
    private String source;
    private BigDecimal sumInsured;
    private BigDecimal balanceSumInsured;
    private Map<String, Object> memberDetails;
}
