package com.vimainsurance.vimaadmin.util;

public enum ZohoSyncStrategy {
    ZOHO_FIRST,    // Zoho CRM data takes precedence
    DB_FIRST,      // Local database data takes precedence
    LATEST_WINS,   // Most recently updated record wins
    MANUAL         // Manual resolution required
} 