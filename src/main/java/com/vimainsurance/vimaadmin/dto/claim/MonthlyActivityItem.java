package com.vimainsurance.vimaadmin.dto.claim;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One month's activity for HR claims dashboard chart. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyActivityItem {
    private int month;
    private int year;
    private long claimCount;
    private BigDecimal totalAmount;
}
