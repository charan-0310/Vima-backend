package com.vimainsurance.vimaadmin.service;

import java.util.List;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsManagementResponse;


public interface FeatureFlagService {

    List<FeatureFlagResponseDto> findAllMatchedFeatureFlags();

    List<FeatureFlagsManagementResponse> getFeatureFlagsGroupedByType();
}
