package com.vimainsurance.vimaadmin.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregates for the CD ledger matching the same filters as the paged transaction list,
 * computed over all matching rows (not just the current page).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdBalanceLedgerSummaryDto {

    /** Sum of positive transaction amounts (deposits / credits). */
    private BigDecimal totalDeposited;

    /** Sum of absolute values of negative amounts (utilization / debits). */
    private BigDecimal totalUtilized;

    /**
     * Net endorsement premium effect: sum of absolute ENDORSEMENT_DEBIT amounts minus sum of
     * absolute ENDORSEMENT_CREDIT amounts (aligned with ledger UI).
     */
    private BigDecimal endorsementPremium;

    /** Count of rows with type ENDORSEMENT_CREDIT or ENDORSEMENT_DEBIT. */
    private long endorsementTransactionCount;
}
