package com.vimainsurance.vimaadmin.service;

import java.util.List;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsManagementResponse;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsOrganizationResponse;
import com.vimainsurance.vimaadmin.dto.FeatureFlagUpdateDto;
import com.vimainsurance.vimaadmin.dto.OrganizationFeatureCountDto;

public interface FeatureFlagService {

    /**
     * Returns true if the given feature flag key is enabled for the current user (roles + orgs from TenantContext).
     * SUPER_ADMIN is treated as having all flags enabled.
     */
    boolean isFeatureEnabledForCurrentUser(String flagKey);

    List<FeatureFlagResponseDto> findAllMatchedFeatureFlags();

    /**
     * When non-null and non-empty, the SPA should treat these as the only organization UUIDs the user may operate on.
     * Used for org-scoped VIMA_ADMIN ({@code admin_users.organization_id} set).
     *
     * @return null when unrestricted (derive orgs from JWT / tenant as today)
     */
    List<String> resolveAllowedOrganizationIdsForAuthMe();

    /**
     * @param roleNameFilter optional; when set (e.g. ROLE_VIMA_ADMIN), only that role bucket is built and returned.
     */
    List<FeatureFlagsManagementResponse> getFeatureFlagsGroupedByType(String roleNameFilter);

    void updateFeatureFlagRoles(String roleName, FeatureFlagUpdateDto updateDto);

    /**
     * @param organizationIdFilter optional organization UUID; when set, only that organization is loaded (much faster).
     */
    List<FeatureFlagsOrganizationResponse> getFeatureFlagsGroupedByOrganization(String organizationIdFilter);

    /**
     * Parent-level feature assignment counts per organization (lightweight; for admin UI badges).
     */
    List<OrganizationFeatureCountDto> getOrganizationParentFeatureCounts();

    void updateFeatureFlagCompanies(String organizationId, FeatureFlagUpdateDto updateDto);

    /**
     * For each feature flag assigned to ROLE_HR_ADMIN in {@code feature_flag_roles}, inserts a matching
     * {@code feature_flag_companies} row for the organization when none exists yet (idempotent).
     */
    void seedOrganizationFeaturesFromHrAdminRole(String organizationId);
}
