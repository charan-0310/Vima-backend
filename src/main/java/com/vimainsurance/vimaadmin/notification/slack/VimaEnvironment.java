package com.vimainsurance.vimaadmin.notification.slack;

/**
 * Deployment environment for this service, resolved once at startup from
 * {@code SPRING_PROFILES_ACTIVE}.
 *
 * <p>Environment topology (see {@code CLAUDE.md}):
 * <ul>
 *   <li>{@link #LOCAL} — developer machine; runs against the dev database.</li>
 *   <li>{@link #DEV} — shared dev backend; runs against the dev database.</li>
 *   <li>{@link #STAGING} — runs against the stage database.</li>
 *   <li>{@link #PROD} — production; runs against the prod database.</li>
 * </ul>
 *
 * <p>UAT is not part of the deployment matrix in this project and the
 * {@code application-uat.properties} file has been removed. If a stray
 * {@code uat} profile is ever set, it maps to {@link #DEV} so it inherits all
 * non-prod safety rules.
 *
 * <p>The {@code test} profile (separate {@code vima_test} database, used by
 * automated/integration tests so they do not crowd the shared dev data) is
 * also folded into {@link #DEV} — for Slack routing purposes every non-prod
 * environment behaves identically (always resolves to {@code TEST_NOTIFICATIONS}).
 *
 * <p>Unknown / missing profile → {@link #LOCAL}. Doing so means an
 * accidental typo can never escalate a process to production routing.
 */
public enum VimaEnvironment {
    LOCAL,
    DEV,
    STAGING,
    PROD;

    public boolean isProd() {
        return this == PROD;
    }

    public boolean isNonProd() {
        return this != PROD;
    }

    /**
     * Map Spring's active profile list to a single environment. The first
     * profile we recognise wins; everything else falls through to
     * {@link #LOCAL}.
     */
    public static VimaEnvironment fromActiveProfiles(String[] activeProfiles) {
        if (activeProfiles == null) {
            return LOCAL;
        }
        for (String raw : activeProfiles) {
            if (raw == null) {
                continue;
            }
            String profile = raw.trim().toLowerCase();
            switch (profile) {
                case "prod":
                case "production":
                    return PROD;
                case "stage":
                case "staging":
                    return STAGING;
                case "dev":
                case "development":
                case "uat":
                case "test":
                    return DEV;
                case "local":
                    return LOCAL;
                default:
                    // keep scanning — multiple profiles may be active
            }
        }
        return LOCAL;
    }
}
