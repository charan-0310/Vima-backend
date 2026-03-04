package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.*;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsManagementResponse;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsOrganizationDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsOrganizationResponse;
import com.vimainsurance.vimaadmin.dto.FeatureFlagUpdateDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagUpdateItemDto;
import com.vimainsurance.vimaadmin.entity.FeatureFlag;
import com.vimainsurance.vimaadmin.entity.FeatureFlagCompany;
import com.vimainsurance.vimaadmin.entity.FeatureFlagRole;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.repository.IFeatureFlagRepository;
import com.vimainsurance.vimaadmin.repository.IFeatureFlagCompanyRepository;
import com.vimainsurance.vimaadmin.repository.FeatureFlagRoleRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import com.vimainsurance.vimaadmin.util.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import java.util.Optional;
import com.vimainsurance.vimaadmin.exception.OrganizationAccessDeniedException;

@Slf4j
@Service
public class FeatureFlagServiceImpl implements FeatureFlagService {

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Autowired
    private IFeatureFlagRepository featureFlagRepository;

    @Autowired
    private FeatureFlagRoleRepository featureFlagRoleRepository;

    @Autowired
    private IFeatureFlagCompanyRepository featureFlagCompanyRepository;

    @Autowired
    private IOrganizationRepository iOrganizationRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;


    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlagResponseDto> findAllMatchedFeatureFlags() {


        String username = jwtUserExtractor.getCurrentUsername();
        if (username == null) {
            log.warn("No username found in JWT token");
            return List.of();
        }

        Optional<AdminUser> adminUser = adminUserRepository.findByUsername(username);
        if (adminUser.isEmpty()) {
            log.warn("No admin user found for username: {}", username);
            throw new OrganizationAccessDeniedException("No admin user found for username: " + username);
        }


        // Fetch flags with roles and companies to avoid N+1
        List<FeatureFlag> featureFlags = featureFlagRepository.findAll();

        Map<String, List<String>> currentTenant = TenantContext.getCurrentTenant();
        // Support both keys "ROLES" and "roles" (tenant source may vary)
        final List<String> rolesFromContext = new ArrayList<>();
        if (currentTenant != null) {
            rolesFromContext.addAll(currentTenant.getOrDefault("ROLES", List.of()));
            rolesFromContext.addAll(currentTenant.getOrDefault("Roles", List.of()));
        }
        log.info("############  ROLES from TenantContext: {}", rolesFromContext);
        final Set<String> normalizedRoles = rolesFromContext.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        final List<String> organizationIds = (currentTenant != null) ? currentTenant.getOrDefault("organizationIds", List.of()) : List.of();
        final Set<String> orgIdSet = organizationIds.stream().filter(id -> id != null && !id.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new));

        log.info("############  Organization IDs from TenantContext: {}", organizationIds);
        List<FeatureFlagResponseDto> responseDtos = new ArrayList<>();

        for (FeatureFlag flag : featureFlags) {
            if (flag == null) continue;
            // skip inactive flags
            // if (flag.getIsActive() != null && !flag.getIsActive()) continue;

            // Collect matching roles
            List<FeatureFlagRole> matchedRoles = new ArrayList<>();
            List<FeatureFlagRole> flagRoles = flag.getRoles();
            if (flagRoles != null) {
                for (FeatureFlagRole role : flagRoles) {
                    if (role == null || role.getRoleName() == null) continue;
                    if (normalizedRoles.contains(role.getRoleName())) {
                        matchedRoles.add(role);
                    }
                }
            }

            // Collect matching companies by organization id
            List<FeatureFlagCompany> matchedCompanies = new ArrayList<>();
            List<FeatureFlagCompany> companies = flag.getCompanies();
            if (companies != null) {
                for (FeatureFlagCompany comp : companies) {
                    if (comp == null) continue;
                    if (comp.getOrganization() == null || comp.getOrganization().getOrganizationId() == null) continue;
                    String orgId = comp.getOrganization().getOrganizationId().toString();
                    if (orgIdSet.contains(orgId)) {
                        matchedCompanies.add(comp);
                    }
                }
            }

            // Decide inclusion: include if any matched role or company OR user is SUPER_ADMIN
            boolean isSuperAdmin = normalizedRoles.stream().anyMatch(r -> r.equalsIgnoreCase("ROLE_SUPER_ADMIN") || r.equalsIgnoreCase("SUPER_ADMIN"));
            FeatureFlagResponseDto dto = createDto(flag, matchedRoles, matchedCompanies, isSuperAdmin);
            responseDtos.add(dto);
        }

        return responseDtos;
    }

    @Override
    public List<FeatureFlagsManagementResponse> getFeatureFlagsGroupedByType() {

        List<FeatureFlag> flags = featureFlagRepository.findAllWithRoles();
        Map<String, List<FeatureFlagResponseDto>> grouped = new HashMap<>();
        for (FeatureFlag flag : flags) {
            List<FeatureFlagRole> roles = flag.getRoles();
            if (roles != null) {

                for (FeatureFlagRole role : roles) {
                    String roleName = role.getRoleName();

                    // Include parent feature flag
                    FeatureFlagResponseDto parentDto = createDto(flag, role);
                    // Include subfeatures explicitly
                    List<FeatureFlagResponseDto> subFeatures = new ArrayList<>();

                    // Create a defensive copy to avoid ConcurrentModificationException during lazy loading
                    List<FeatureFlag> subFeatureFlags = featureFlagRepository.findSubFeatureFlagsByParentId(flag.getFlagId());
                    List<FeatureFlag> subFlags = (subFeatureFlags != null)
                            ? new ArrayList<>(subFeatureFlags).stream().filter(Objects::nonNull).toList()
                            : Collections.emptyList();

                    for (FeatureFlag sub : subFlags) {
                        Optional<FeatureFlagRole> featureFlagRoleOptional= featureFlagRoleRepository.findByFlagIdAndRoleName(sub.getFlagId(), role.getRoleName());
                        if (featureFlagRoleOptional.isEmpty()) {
                            continue;
                        }
                        FeatureFlagResponseDto subDto = createDto(sub, featureFlagRoleOptional.get());
                        subFeatures.add(subDto);
                    }
                    parentDto.setSubFeatures(subFeatures);
                    grouped.computeIfAbsent(roleName, k -> new ArrayList<>()).add(parentDto);
                }
            } else {
                log.warn("Feature flag with ID {} has no associated roles, adding to UNMAPPED group...", flag.getFlagId());

           }

        }

        // Fill missing flags per role with disabled DTOs
        // Build universe of role names from existing map and flags
        Set<String> allRoleNames = new LinkedHashSet<>(grouped.keySet());
        for (FeatureFlag f : flags) {
            if (f.getRoles() != null) {
                for (FeatureFlagRole r : f.getRoles()) {
                    if (r.getRoleName() != null) {
                        allRoleNames.add(r.getRoleName());
                    }
                }
            }
        }

        // For each role, ensure every flag has an entry; if missing, add disabled parent and subfeatures
        for (String roleName : allRoleNames) {
            List<FeatureFlagResponseDto> roleList = grouped.computeIfAbsent(roleName, k -> new ArrayList<>());
            Set<String> presentFlagIds = roleList.stream()
                    .map(FeatureFlagResponseDto::getFlagId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            for (FeatureFlag f : flags) {
                String fid = f.getFlagId() != null ? f.getFlagId().toString() : null;
                if (fid == null || presentFlagIds.contains(fid)) {
                    continue;
                }
                FeatureFlagResponseDto disabledParent = createDisabledDto(f);

                List<FeatureFlag> subFeatureFlags = featureFlagRepository.findSubFeatureFlagsByParentId(f.getFlagId());
                List<FeatureFlagResponseDto> disabledSubs = new ArrayList<>();
                if (subFeatureFlags != null) {
                    for (FeatureFlag sub : subFeatureFlags) {
                        disabledSubs.add(createDisabledDto(sub));
                    }
                }
                disabledParent.setSubFeatures(disabledSubs);
                grouped.computeIfAbsent(roleName, k -> new ArrayList<>()).add(disabledParent);
            }
        }


        List<FeatureFlagsManagementResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<FeatureFlagResponseDto>> e : grouped.entrySet()) {
            FeatureFlagsManagementResponse m = new FeatureFlagsManagementResponse();
            m.setType("role");
            m.setIdentifier(e.getKey());
            m.setFeatures(e.getValue());
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlagsOrganizationResponse> getFeatureFlagsGroupedByOrganization() {
        log.info("Fetching feature flags grouped by organization");

        // Get role name from TenantContext
        Map<String, List<String>> currentTenant = TenantContext.getCurrentTenant();
        final List<String> rolesFromContext = new ArrayList<>();
        if (currentTenant != null) {
            rolesFromContext.addAll(currentTenant.getOrDefault("ROLES", List.of()));
            rolesFromContext.addAll(currentTenant.getOrDefault("Roles", List.of()));
        }
        log.info("ROLES from TenantContext for organization query: {}", rolesFromContext);

        if (rolesFromContext.isEmpty()) {
            log.warn("No roles found in TenantContext, returning empty list");
            return List.of();
        }

        // Query FeatureFlagCompany where featureFlag matches the role's featureFlag
        List<FeatureFlagCompany> matchedCompanies = new ArrayList<>();
        for (String roleName : rolesFromContext) {
            if (roleName != null && !roleName.isBlank()) {
                List<FeatureFlagCompany> companies = featureFlagCompanyRepository.findByRoleName(roleName.toUpperCase());
                matchedCompanies.addAll(companies);
            }
        }

        log.info("Found {} matched FeatureFlagCompany records", matchedCompanies.size());

        // Group by organization
        Map<String, List<FeatureFlagCompany>> groupedByOrg = matchedCompanies.stream()
                .filter(c -> c.getOrganization() != null && c.getOrganization().getOrganizationId() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getOrganization().getOrganizationId().toString()
                ));

        List<FeatureFlagsOrganizationResponse> result = new ArrayList<>();

        for (Map.Entry<String, List<FeatureFlagCompany>> entry : groupedByOrg.entrySet()) {
            String organizationId = entry.getKey();
            List<FeatureFlagCompany> companyFeatures = entry.getValue();

            if (companyFeatures.isEmpty()) continue;

            // Get organization name from first entry
            String organizationName = companyFeatures.get(0).getOrganization().getOrganizationName();

            // Build feature DTOs
            List<FeatureFlagsOrganizationDto> features = new ArrayList<>();
            for (FeatureFlagCompany ffc : companyFeatures) {
                FeatureFlag flag = ffc.getFeatureFlag();
                if (flag == null) continue;

                FeatureFlagsOrganizationDto dto = new FeatureFlagsOrganizationDto();
                dto.setFlagId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
                dto.setFlagKey(flag.getFlagKey());
                dto.setDescription(flag.getDescription());
                dto.setIsActive(true);
                dto.setIsEnabled(ffc.getIsActive());
                dto.setActions(ffc.getActions() != null ? Arrays.asList(ffc.getActions()) : List.of());
                List<FeatureFlag> subFeatureFlags = featureFlagRepository.findSubFeatureFlagsByParentId(flag.getFlagId());
                List<FeatureFlagsOrganizationDto> subFeatures = new ArrayList<>();
                UUID currentOrgId = ffc.getOrganization().getOrganizationId();

                if (subFeatureFlags != null) {
                    // Build a map of subfeature company states for the current org to avoid repeated lookups
                    Map<UUID, FeatureFlagCompany> subCompanyByFlagId = new HashMap<>();
                    for (FeatureFlag sub : subFeatureFlags) {
                        Optional<FeatureFlagCompany> subCompanyOpt =
                                featureFlagCompanyRepository.findByFlagIdAndOrganizationId(sub.getFlagId(), currentOrgId);
                        subCompanyOpt.ifPresent(sc -> subCompanyByFlagId.put(sub.getFlagId(), sc));
                    }

                    for (FeatureFlag sub : subFeatureFlags) {
                        FeatureFlagsOrganizationDto subDto = new FeatureFlagsOrganizationDto();
                        subDto.setFlagId(sub.getFlagId() != null ? sub.getFlagId().toString() : null);
                        subDto.setFlagKey(sub.getFlagKey());
                        subDto.setDescription(sub.getDescription());

                        FeatureFlagCompany subFfc = subCompanyByFlagId.get(sub.getFlagId());
                        if (subFfc != null) {
                            subDto.setIsActive(true);
                            subDto.setIsEnabled(subFfc.getIsActive());
                            subDto.setActions(subFfc.getActions() != null ? Arrays.asList(subFfc.getActions()) : List.of());
                        } else {
                            subDto.setIsActive(true);
                            subDto.setIsEnabled(false);
                            subDto.setActions(List.of());
                        }
                        subFeatures.add(subDto);
                    }
                }
                dto.setSubFeatures(subFeatures);
                features.add(dto);
            }

            FeatureFlagsOrganizationResponse response = new FeatureFlagsOrganizationResponse();
            response.setType("organization");
            response.setIdentifier(organizationName);
            response.setOrganizationId(organizationId);
            response.setFeatures(features);
            result.add(response);
        }

        log.info("Returning {} organization feature flag groups", result.size());
        return result;
    }

    private FeatureFlagResponseDto createDto(FeatureFlag flag,
                                             List<FeatureFlagRole> matchedRoles,
                                             List<FeatureFlagCompany> matchedCompanies,
                                             boolean isSuperAdmin) {
        FeatureFlagResponseDto dto = new FeatureFlagResponseDto();
        dto.setFlagId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
        dto.setFlagKey(flag.getFlagKey());
        dto.setDescription(flag.getDescription());
        // Active if there is at least one matched role
        boolean isActiveOrSuperAdmin = matchedRoles.stream().anyMatch(FeatureFlagRole::getIsActive) || isSuperAdmin;
        dto.setIsActive(isActiveOrSuperAdmin);
        dto.setIsEnabled(isActiveOrSuperAdmin);

        List<String> actions = new ArrayList<>();
        for (FeatureFlagRole r : matchedRoles) {
            if (r.getActions() != null) {
                actions.addAll(List.of(r.getActions()));
            }
        }

        for (FeatureFlagCompany c : matchedCompanies) {
            if (c.getActions() != null) {

               actions.addAll(Arrays.asList(c.getActions()));
            }
        }

        if (isSuperAdmin && actions.isEmpty()) {
            for (FeatureFlagRole r : flag.getRoles()) {
                if (r.getActions() != null) actions.addAll(List.of(r.getActions()));
            }
            for (FeatureFlagCompany c : flag.getCompanies()) {
                if (c.getActions() != null) {
                    actions.addAll(Arrays.asList(c.getActions()));
                }
            }
        }
        dto.setActions(actions);

        List<FeatureFlagResponseDto.CompanyDto> companyDtos = new ArrayList<>();
        for (FeatureFlagCompany c : matchedCompanies) {
            FeatureFlagResponseDto.CompanyDto cd = getCompanyDto(flag, c);
            companyDtos.add(cd);
        }
        dto.setCompanies(companyDtos);

        return dto;
    }

    private FeatureFlagResponseDto createDto(FeatureFlag flag, FeatureFlagRole matchedRole) {
        FeatureFlagResponseDto dto = new FeatureFlagResponseDto();
        dto.setFlagId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
        dto.setFlagKey(flag.getFlagKey());
        dto.setDescription(flag.getDescription());
        // Active if there is at least one matched role
        dto.setIsActive(matchedRole.getIsActive());
        dto.setIsEnabled(matchedRole.getIsActive());
        dto.setActions(matchedRole.getActions() != null ? Arrays.asList(matchedRole.getActions()) : List.of());
        return dto;
    }


    private FeatureFlagResponseDto createDisabledDto(FeatureFlag flag) {
        FeatureFlagResponseDto dto = new FeatureFlagResponseDto();
        dto.setFlagId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
        dto.setFlagKey(flag.getFlagKey());
        dto.setDescription(flag.getDescription());
        dto.setIsActive(false);
        dto.setIsEnabled(false);
        dto.setActions(List.of());
        dto.setSubFeatures(List.of());
        return dto;
    }

    private static FeatureFlagResponseDto.CompanyDto getCompanyDto(FeatureFlag flag, FeatureFlagCompany c) {
        FeatureFlagResponseDto.CompanyDto cd = new FeatureFlagResponseDto.CompanyDto();
        cd.setId(c.getId() != null ? c.getId().toString() : null);
        cd.setFlagId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
        try {
            if (c.getOrganization() != null) {
                var org = c.getOrganization();
                cd.setOrganizationId(org.getOrganizationId() != null ? org.getOrganizationId().toString() : null);
                cd.setOrganizationName(org.getOrganizationName() != null ? org.getOrganizationName() : null);
            } else {
                cd.setOrganizationId(null);
                cd.setOrganizationName(null);
            }
        } catch (jakarta.persistence.EntityNotFoundException ex) {
            // referenced organization not present — treat as absent
            cd.setOrganizationId(null);
            cd.setOrganizationName(null);
        }
        cd.setActions(c.getActions() != null ? List.of(c.getActions()) : List.of());
        return cd;
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "feature_flag_roles", entityType = "FEATURE_FLAG_ROLE", action = "UPDATE")
    public void updateFeatureFlagRoles(String roleName, FeatureFlagUpdateDto updateDto) {
        log.info("Updating feature flag roles for roleName: {} with {} updates",
                roleName, updateDto.getUpdates().size());

        if (updateDto.getUpdates() == null || updateDto.getUpdates().isEmpty()) {
            log.warn("No updates provided in the request");
            return;
        }

        String normalizedRoleName = roleName.toUpperCase();
        log.info("Normalized role name: {}", normalizedRoleName);

        List<UUID> flagIds = updateDto.getUpdates().stream()
                .map(FeatureFlagUpdateItemDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

         if (flagIds.isEmpty()) {
            log.warn("No valid flag IDs provided in the request");
            return;
        }

        // Find existing feature flag roles for this role and the provided flag IDs
        List<FeatureFlagRole> existingRoles = featureFlagRoleRepository
                .findByFeatureFlagIdsAndRoleName(flagIds, normalizedRoleName);

        log.info("Found {} existing FeatureFlagRole entries for role: {}", existingRoles.size(), normalizedRoleName);

        // Create a map for quick lookup: flagId -> FeatureFlagRole
        Map<UUID, FeatureFlagRole> existingRolesMap = new HashMap<>();
        for (FeatureFlagRole role : existingRoles) {
            if (role.getFeatureFlag() != null && role.getFeatureFlag().getFlagId() != null) {
                existingRolesMap.put(role.getFeatureFlag().getFlagId(), role);
            }
        }

        log.info("Existing roles map keys: {}", existingRolesMap.keySet());

        List<FeatureFlagRole> rolesToSave = new ArrayList<>();

        for (FeatureFlagUpdateItemDto updateItem : updateDto.getUpdates()) {
            UUID flagId = updateItem.getId();

            Boolean enabled = updateItem.getEnabled() != null ? updateItem.getEnabled() : false;
            List<String> actions = updateItem.getActions() != null ? updateItem.getActions() : Collections.emptyList();

            log.info("Processing update for flagId: {}, enabled: {}, actions: {}", flagId, enabled, actions);

            FeatureFlagRole roleToUpdate = existingRolesMap.get(flagId);

            if (roleToUpdate != null) {
                // Update existing role
                log.info("Found existing FeatureFlagRole with id: {} for flagId: {}, updating...",
                        roleToUpdate.getId(), flagId);
                roleToUpdate.setIsActive(enabled);
                roleToUpdate.setActions(actions.isEmpty() ? null : actions.toArray(new String[0]));
                rolesToSave.add(roleToUpdate);
                log.info("Prepared update for existing role - flag ID: {}, enabled: {}, actions: {}",
                        flagId, enabled, actions);
            } else {
                // Create new role entry if it doesn't exist
                log.info("No existing FeatureFlagRole for flagId: {}, checking if FeatureFlag exists...", flagId);

                // Try to find the FeatureFlag
                Optional<FeatureFlag> featureFlagOpt = featureFlagRepository.findById(flagId);

                if (featureFlagOpt.isPresent()) {
                    FeatureFlag featureFlag = featureFlagOpt.get();
                    log.info("FeatureFlag found - flagId: {}, flagKey: {}, creating new FeatureFlagRole...",
                            featureFlag.getFlagId(), featureFlag.getFlagKey());

                    FeatureFlagRole newRole = new FeatureFlagRole();
                    newRole.setId(UUID.randomUUID());
                    newRole.setFeatureFlag(featureFlag);
                    newRole.setRoleName(normalizedRoleName);
                    newRole.setIsActive(enabled);
                    newRole.setActions(actions.isEmpty() ? null : actions.toArray(String[]::new));

                    rolesToSave.add(newRole);
                    log.info("Created new FeatureFlagRole - id: {}, flagId: {}, roleName: {}, enabled: {}, actions: {}",
                            newRole.getId(), flagId, normalizedRoleName, enabled, actions);
                } else {
                    log.warn("FeatureFlag with ID {} not found in feature_flags table, skipping. " +
                            "Please verify this flag_id exists in admin.feature_flags table.", flagId);
                }
            }
        }

        if (!rolesToSave.isEmpty()) {
            log.info("Saving {} feature flag roles...", rolesToSave.size());
            try {
                List<FeatureFlagRole> savedRoles = featureFlagRoleRepository.saveAll(rolesToSave);
                featureFlagRoleRepository.flush(); // Force immediate write to database
                log.info("Successfully saved {} feature flag roles to database", savedRoles.size());

            } catch (Exception e) {
                log.error("Failed to save feature flag roles: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to save feature flag roles: " + e.getMessage(), e);
            }
        } else {
            log.warn("No feature flag roles to save - check if the flag IDs exist in the database. " +
                    "Provided flagIds: {}", flagIds);
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "admin", tableName = "feature_flag_companies", entityType = "FEATURE_FLAG_COMPANY", action = "UPDATE")
    public void updateFeatureFlagCompanies(String organizationId, FeatureFlagUpdateDto updateDto) {
        log.info("Updating feature flag companies for organizationId: {} with {} updates",
                organizationId, updateDto.getUpdates().size());

        UUID orgId;
        try {
            orgId = UUID.fromString(organizationId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid organizationId format: {}", organizationId);
            throw new IllegalArgumentException("Invalid organizationId format: " + organizationId);
        }
        AuditContextSupplier.setOrganizationId(orgId);

        List<UUID> flagIds = updateDto.getUpdates().stream()
                .map(FeatureFlagUpdateItemDto::getId)
                .collect(Collectors.toList());

        // Find existing feature flag companies for this organization and the provided flag IDs
        List<FeatureFlagCompany> existingCompanies = featureFlagCompanyRepository
                .findByOrganizationIdAndFlagIds(orgId, flagIds);

        // Create a map for quick lookup: flagId -> FeatureFlagCompany
        Map<UUID, FeatureFlagCompany> existingCompaniesMap = existingCompanies.stream()
                .collect(Collectors.toMap(
                        company -> company.getFeatureFlag().getFlagId(),
                        company -> company
                ));

        List<FeatureFlagCompany> companiesToUpdate = new ArrayList<>();

        for (FeatureFlagUpdateItemDto updateItem : updateDto.getUpdates()) {
            UUID flagId = updateItem.getId();
            Boolean enabled = updateItem.getEnabled();
            List<String> actions = updateItem.getActions();

            FeatureFlagCompany companyToUpdate = existingCompaniesMap.get(flagId);

            if (companyToUpdate != null) {
                // Update existing company record
                companyToUpdate.setIsActive(enabled);
                companyToUpdate.setActions(actions == null || actions.isEmpty() ? null : actions.toArray(String[]::new));
                companiesToUpdate.add(companyToUpdate);
                log.debug("Updated existing company for flag ID: {}, enabled: {}, actions: {}",
                        flagId, enabled, actions);
            } else {

                // Create new FeatureFlagCompany if none exists
                Optional<FeatureFlag> featureFlagOpt = featureFlagRepository.findById(flagId);
                if (featureFlagOpt.isPresent()) {
                    FeatureFlagCompany newCompany = new FeatureFlagCompany();
                    newCompany.setId(UUID.randomUUID());
                    newCompany.setFeatureFlag(featureFlagOpt.get());

                    Optional<Organization> organizationOptional = iOrganizationRepository.findByOrganizationId(orgId);
                    if(organizationOptional.isEmpty()) {
                         log.warn("Organization with ID {} not found, skipping creation for feature flag {}", organizationId, flagId);
                         continue;
                    }
                    newCompany.setOrganization(organizationOptional.get());
                    newCompany.setIsActive(enabled != null ? enabled : false);
                    newCompany.setActions(actions == null || actions.isEmpty() ? null : actions.toArray(String[]::new));
                    companiesToUpdate.add(newCompany);
                    log.debug("Created new FeatureFlagCompany for flag ID: {}, org ID: {}, enabled: {}, actions: {}",
                            flagId, organizationId, enabled, actions);
                } else {
                    log.warn("FeatureFlag with ID {} not found, skipping creation for organization {}", flagId, organizationId);
                }
            }

        }

        if (!companiesToUpdate.isEmpty()) {
            featureFlagCompanyRepository.saveAll(companiesToUpdate);
            log.info("Successfully updated {} feature flag companies", companiesToUpdate.size());
        } else {
            log.info("No feature flag companies to update for organization: {}", organizationId);
        }
    }
}