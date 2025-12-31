package com.vimainsurance.vimaadmin.service;

import java.util.List;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;


public interface FeatureFlagService {

    List<FeatureFlagResponseDto> findAllMatchedFeatureFlags();
}
