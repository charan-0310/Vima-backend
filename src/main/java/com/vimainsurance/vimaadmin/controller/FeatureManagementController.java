package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.*;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class FeatureManagementController {

  private static final Logger logger = LoggerFactory.getLogger(FeatureManagementController.class);

  @Autowired
  private FeatureFlagService featureFlagService;

    @GetMapping("/features/roles")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<List<FeatureFlagsManagementResponse>> getFeatureFlags(
            @RequestParam(value = "roleName", required = false) String roleName) {
        logger.info("Request received: get feature flags by roles (roleName filter: {})", roleName);
        try {
            List<FeatureFlagsManagementResponse> flags = featureFlagService.getFeatureFlagsGroupedByType(roleName);
            if (flags == null || flags.isEmpty()) {
                logger.info("No feature flags found for roles");
                return ResponseEntity.ok(Collections.emptyList());
            }
            logger.debug("Returning {} feature flags for roles", flags.size());
            return ResponseEntity.ok(flags);
        } catch (Exception e) {
            logger.error("Error while fetching feature flags for roles", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/features/roles")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<String> updateFeatureFlagRoles( @RequestParam(value ="roleName", required = true) String roleName,
                                                          @Valid @NotNull  @RequestBody FeatureFlagUpdateDto updateDto) {
        logger.info("Request received: update feature flag roles for identifier: {}", updateDto.getIdentifier());
        try {
            featureFlagService.updateFeatureFlagRoles(roleName, updateDto);
            logger.info("Successfully updated feature flag roles for identifier: {}", updateDto.getIdentifier());
            return ResponseEntity.ok("Feature flag roles updated successfully");
        } catch (Exception e) {
            logger.error("Error while updating feature flag roles for identifier: {}", updateDto.getIdentifier(), e);
            return ResponseEntity.status(500).body("Failed to update feature flag roles: " + e.getMessage());
        }
    }

    /**
     * Lightweight parent-feature counts per organization (for admin UI badges). Avoids loading full org trees.
     */
    @GetMapping("/features/organizations/feature-counts")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<List<OrganizationFeatureCountDto>> getOrganizationFeatureCounts() {
        logger.info("Request received: organization parent feature counts");
        try {
            List<OrganizationFeatureCountDto> counts = featureFlagService.getOrganizationParentFeatureCounts();
            return ResponseEntity.ok(counts == null ? Collections.emptyList() : counts);
        } catch (Exception e) {
            logger.error("Error while fetching organization feature counts", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/features/organizations")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<?> getFeatureFlagsByOrganization(
            @RequestParam(value = "organizationId", required = false) String organizationId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        logger.info(
                "Request received: get feature flags by organizations (organizationId filter: {}, page: {}, pageSize: {})",
                organizationId, page, pageSize);
        try {
            List<FeatureFlagsOrganizationResponse> flags =
                    featureFlagService.getFeatureFlagsGroupedByOrganization(organizationId);
            if (flags == null) {
                flags = Collections.emptyList();
            }

            boolean paginate = organizationId == null
                    && page != null
                    && pageSize != null
                    && page > 0
                    && pageSize > 0;
            if (paginate) {
                int size = Math.min(pageSize, 100);
                int p = page;
                long total = flags.size();
                int fromIndex = (p - 1) * size;
                List<FeatureFlagsOrganizationResponse> slice =
                        fromIndex >= flags.size()
                                ? Collections.emptyList()
                                : flags.subList(fromIndex, Math.min(fromIndex + size, flags.size()));
                int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / (double) size);
                FeatureFlagsOrganizationsPageResponse body =
                        new FeatureFlagsOrganizationsPageResponse(slice, total, p, size, totalPages);
                return ResponseEntity.ok(body);
            }

            if (flags.isEmpty()) {
                logger.info("No feature flags found for organizations");
                return ResponseEntity.ok(Collections.emptyList());
            }
            logger.debug("Returning {} feature flag groups for organizations", flags.size());
            return ResponseEntity.ok(flags);
        } catch (Exception e) {
            logger.error("Error while fetching feature flags by organizations", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/features/organizations")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<String> updateFeatureFlagCompanies(
            @RequestParam(value ="organizationId", required = true) String organizationId,
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

    /**
     * Syncs existing organization feature rows from ROLE_HR_ADMIN (does not add missing features).
     */
    @PostMapping("/features/organizations/defaults")
    @PreAuthorize("hasRole('VIMA_ADMIN')")
    public ResponseEntity<String> seedOrganizationFeatureDefaults(@RequestParam("organizationId") String organizationId) {
        logger.info("Request received: sync existing HR_ADMIN defaults for organizationId: {}", organizationId);
        try {
            if (organizationId == null || organizationId.isBlank()) {
                return ResponseEntity.badRequest().body("organizationId is required");
            }
            featureFlagService.seedOrganizationFeaturesFromHrAdminRole(organizationId);
            return ResponseEntity.ok("Existing organization feature rows synced from ROLE_HR_ADMIN");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid organizationId: {}", organizationId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error syncing organization defaults for organizationId: {}", organizationId, e);
            return ResponseEntity.status(500).body("Failed to sync organization defaults: " + e.getMessage());
        }
    }


}
