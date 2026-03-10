package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.EmployerShareType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of applying a cost-sharing rule: employer and employee share of total premium.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostShareSplit {

    private BigDecimal employerShare;
    private BigDecimal employeeShare;
    private EmployerShareType shareType;
    private BigDecimal shareValue;
    private UUID ruleId;
}
