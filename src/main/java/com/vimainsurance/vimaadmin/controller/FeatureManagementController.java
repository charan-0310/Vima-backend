package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.*;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class FeatureManagementController {

  private static final Logger logger = LoggerFactory.getLogger(FeatureManagementController.class);

  @Autowired
  private FeatureFlagService featureFlagService;

    @GetMapping("/features/roles")
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

    @PostMapping("/features")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<String> updateFeatureFlagRoles(@Valid @RequestBody FeatureFlagUpdateDto updateDto) {
        logger.info("Request received: update feature flag roles for identifier: {}", updateDto.getIdentifier());
        try {
            featureFlagService.updateFeatureFlagRoles(updateDto);
            logger.info("Successfully updated feature flag roles for identifier: {}", updateDto.getIdentifier());
            return ResponseEntity.ok("Feature flag roles updated successfully");
        } catch (Exception e) {
            logger.error("Error while updating feature flag roles for identifier: {}", updateDto.getIdentifier(), e);
            return ResponseEntity.status(500).body("Failed to update feature flag roles: " + e.getMessage());
        }
    }


}
