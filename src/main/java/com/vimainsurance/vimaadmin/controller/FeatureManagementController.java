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

    @GetMapping("/features/organizations")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<List<FeatureFlagsOrganizationResponse>> getFeatureFlagsByOrganization() {
        logger.info("Request received: get feature flags by organizations");
        try {
            List<FeatureFlagsOrganizationResponse> flags = featureFlagService.getFeatureFlagsGroupedByOrganization();
            if (flags == null || flags.isEmpty()) {
                logger.info("No feature flags found for organizations");
                return ResponseEntity.noContent().build();
            }
            logger.debug("Returning {} feature flag groups for organizations", flags.size());
            return ResponseEntity.ok(flags);
        } catch (Exception e) {
            logger.error("Error while fetching feature flags for organizations", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/features/organizations")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<String> updateFeatureFlagCompanies(
            @RequestParam("organizationId") String organizationId,
            @Valid @RequestBody FeatureFlagUpdateDto updateDto) {
        logger.info("Request received: update feature flag companies for organizationId: {}", organizationId);
        try {
            if (organizationId == null || organizationId.isBlank()) {
                logger.warn("organizationId is required but was not provided");
                return ResponseEntity.badRequest().body("organizationId is required");
            }
            featureFlagService.updateFeatureFlagCompanies(organizationId, updateDto);
            logger.info("Successfully updated feature flag companies for organizationId: {}", organizationId);
            return ResponseEntity.ok("Feature flag companies updated successfully");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for organizationId: {}", organizationId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error while updating feature flag companies for organizationId: {}", organizationId, e);
            return ResponseEntity.status(500).body("Failed to update feature flag companies: " + e.getMessage());
        }
    }



}
