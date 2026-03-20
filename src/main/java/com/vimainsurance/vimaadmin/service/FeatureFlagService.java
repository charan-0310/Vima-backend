package com.vimainsurance.vimaadmin.service;

import java.util.List;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsManagementResponse;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsOrganizationResponse;
import com.vimainsurance.vimaadmin.dto.FeatureFlagUpdateDto;


public interface FeatureFlagService {

    /**
     * Returns true if the given feature flag key is enabled for the current user (roles + orgs from TenantContext).
     * SUPER_ADMIN is treated as having all flags enabled.
     */
    boolean isFeatureEnabledForCurrentUser(String flagKey);

    List<FeatureFlagResponseDto> findAllMatchedFeatureFlags();

    List<FeatureFlagsManagementResponse> getFeatureFlagsGroupedByType();

    void updateFeatureFlagRoles(String roleName, FeatureFlagUpdateDto updateDto);

    List<FeatureFlagsOrganizationResponse> getFeatureFlagsGroupedByOrganization();

    void updateFeatureFlagCompanies(String organizationId, FeatureFlagUpdateDto updateDto);

    /**
     * For each feature flag assigned to ROLE_HR_ADMIN in {@code feature_flag_roles}, inserts a matching
     * {@code feature_flag_companies} row for the organization when none exists yet (idempotent).
     */
    void seedOrganizationFeaturesFromHrAdminRole(String organizationId);
}
