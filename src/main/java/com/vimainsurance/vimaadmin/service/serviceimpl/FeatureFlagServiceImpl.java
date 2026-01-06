package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.*;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagsManagementResponse;
import com.vimainsurance.vimaadmin.dto.FeatureFlagUpdateDto;
import com.vimainsurance.vimaadmin.dto.FeatureFlagUpdateItemDto;
import com.vimainsurance.vimaadmin.entity.FeatureFlag;
import com.vimainsurance.vimaadmin.entity.FeatureFlagCompany;
import com.vimainsurance.vimaadmin.entity.FeatureFlagRole;
import com.vimainsurance.vimaadmin.repository.IFeatureFlagRepository;
import com.vimainsurance.vimaadmin.repository.FeatureFlagRoleRepository;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import com.vimainsurance.vimaadmin.util.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class FeatureFlagServiceImpl implements FeatureFlagService {

    @Autowired
    private IFeatureFlagRepository featureFlagRepository;

    @Autowired
    private FeatureFlagRoleRepository featureFlagRoleRepository;


    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlagResponseDto> findAllMatchedFeatureFlags() {


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
                    FeatureFlagResponseDto dto = createDto(flag, List.of(role), List.of(), false);
                    grouped.computeIfAbsent(roleName, k -> new ArrayList<>()).add(dto);
                }
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

    private FeatureFlagResponseDto createDto(FeatureFlag flag,
                                             List<FeatureFlagRole> matchedRoles,
                                             List<FeatureFlagCompany> matchedCompanies,
                                             boolean isSuperAdmin) {
        FeatureFlagResponseDto dto = new FeatureFlagResponseDto();
        dto.setFlagId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
        dto.setFlagKey(flag.getFlagKey());
        dto.setDescription(flag.getDescription());
        // Active if there is at least one matched role
        dto.setIsActive( !matchedRoles.isEmpty() || isSuperAdmin);
        dto.setIsEnabled(!matchedRoles.isEmpty() || isSuperAdmin);

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
    public void updateFeatureFlagRoles(FeatureFlagUpdateDto updateDto) {
        log.info("Updating feature flag roles for identifier: {} with {} updates",
                updateDto.getIdentifier(), updateDto.getUpdates().size());

        String roleName = updateDto.getIdentifier();
        List<UUID> flagIds = updateDto.getUpdates().stream()
                .map(FeatureFlagUpdateItemDto::getId)
                .collect(Collectors.toList());

        // Find existing feature flag roles for this role and the provided flag IDs
        List<FeatureFlagRole> existingRoles = featureFlagRoleRepository
                .findByFeatureFlagIdsAndRoleName(flagIds, roleName);

        // Create a map for quick lookup: flagId -> FeatureFlagRole
        Map<UUID, FeatureFlagRole> existingRolesMap = existingRoles.stream()
                .collect(Collectors.toMap(
                        role -> role.getFeatureFlag().getFlagId(),
                        role -> role
                ));

        List<FeatureFlagRole> rolesToUpdate = new ArrayList<>();

        for (FeatureFlagUpdateItemDto updateItem : updateDto.getUpdates()) {
            UUID flagId = updateItem.getId();
            Boolean enabled = updateItem.getEnabled();
            List<String> actions = updateItem.getActions();

            FeatureFlagRole roleToUpdate = existingRolesMap.get(flagId);

            if (roleToUpdate != null) {
                // Update existing role
                roleToUpdate.setIsActive(enabled);
                roleToUpdate.setActions(actions.isEmpty() ? null : actions.toArray(String[]::new));
                rolesToUpdate.add(roleToUpdate);
                log.debug("Updated existing role for flag ID: {}, enabled: {}, actions: {}",
                        flagId, enabled, actions);
            } else {
                // Create new role entry if it doesn't exist
                Optional<FeatureFlag> featureFlagOpt = featureFlagRepository.findById(flagId);
                if (featureFlagOpt.isPresent()) {
                    FeatureFlagRole newRole = new FeatureFlagRole();
                    newRole.setId(UUID.randomUUID());
                    newRole.setFeatureFlag(featureFlagOpt.get());
                    newRole.setRoleName(roleName);
                    newRole.setIsActive(enabled);

                    if (actions != null) {
                        newRole.setActions(actions.isEmpty() ? null : actions.toArray(String[]::new));
                    }
                    rolesToUpdate.add(newRole);
                    log.debug("Created new role for flag ID: {}, enabled: {}, actions: {}",
                            flagId, enabled, actions);
                } else {
                    log.warn("Feature flag with ID {} not found, skipping update", flagId);
                }
            }
        }

        if (!rolesToUpdate.isEmpty()) {
            featureFlagRoleRepository.saveAll(rolesToUpdate);
            log.info("Successfully updated {} feature flag roles", rolesToUpdate.size());
        }
    }
}