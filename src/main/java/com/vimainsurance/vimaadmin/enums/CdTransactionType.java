package com.vimainsurance.vimaadmin.enums;

/**
 * CD balance ledger transaction kinds (maps to {@code cpc.cd_transaction_type_enum}).
 */
public enum CdTransactionType {
    INITIAL_DEPOSIT,
    ENDORSEMENT_DEBIT,
    ENDORSEMENT_CREDIT,
    ADJUSTMENT,
    TOP_UP,
    SETTLEMENT
}
