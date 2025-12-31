package com.vimainsurance.vimaadmin.util;

import java.util.List;
import java.util.Map;

/**
 * Simple ThreadLocal holder for current tenant id. Remember to clear after request.
 */
public final class TenantContext {
    private static final ThreadLocal<Map<String, List<String>>> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(Map<String, List<String>> tenant) {
        CURRENT.set(tenant);
    }

    public static Map<String, List<String>> getCurrentTenant() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}

