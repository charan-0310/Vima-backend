package com.vimainsurance.vimaadmin.enums;

/**
 * How a CD balance transaction was recorded (maps to {@code cpc.cd_transaction_source_enum}).
 */
public enum CdTransactionSource {
    MANUAL,
    ENDORSEMENT_APPROVAL,
    API_SYNC
}
