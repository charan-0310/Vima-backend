package com.vimainsurance.vimaadmin.service;

import java.util.List;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsManagementResponse;
import com.vimainsurance.vimaadmin.dto.FeatureFlagUpdateDto;


public interface FeatureFlagService {

    List<FeatureFlagResponseDto> findAllMatchedFeatureFlags();

    List<FeatureFlagsManagementResponse> getFeatureFlagsGroupedByType();

    void updateFeatureFlagRoles(FeatureFlagUpdateDto updateDto);
}
