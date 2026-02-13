package com.vimainsurance.vimaadmin.util;

import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Utility for transaction operations. Use when a transaction may not be present (e.g. unit tests).
 */
public final class TransactionUtil {

    private TransactionUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Marks the current transaction for rollback only. Safe when no transaction is active (e.g. in unit tests).
     */
    public static void markRollbackOnly() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (Exception ignored) {
            // No active transaction (e.g. in unit tests) - ignore
        }
    }
}
