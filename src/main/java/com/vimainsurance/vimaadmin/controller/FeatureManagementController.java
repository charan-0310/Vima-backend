package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.AdminUserResponseDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsManagementResponse;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/features")
public class FeatureManagementController {

  private static final Logger logger = LoggerFactory.getLogger(FeatureManagementController.class);

  @Autowired
  private FeatureFlagService featureFlagService;

    @GetMapping("/roles")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<List<FeatureFlagsManagementResponse>> getFeatureFlags() {
        logger.info("Request received: get feature flags by roles");
        try {
            List<FeatureFlagsManagementResponse> flags = featureFlagService.getFeatureFlagsGroupedByType();
            if (flags == null || flags.isEmpty()) {
                logger.info("No feature flags found for roles");
                return ResponseEntity.noContent().build();
            }
            logger.debug("Returning {} feature flags for roles", flags.size());
            return ResponseEntity.ok(flags);
        } catch (Exception e) {
            logger.error("Error while fetching feature flags for roles", e);
            return ResponseEntity.status(500).build();
        }
    }


}
