package com.vimainsurance.vimaadmin.repository;

import java.util.UUID;

/**
 * Projection for a single aggregation query returning progress counts per enrollment window.
 * Used to avoid loading full entity lists when building the window list with completion rate.
 */
public interface WindowProgressProjection {

    UUID getWindowId();

    long getEmployeeCount();

    long getInvitationCount();

    long getSubmittedCount();
}
