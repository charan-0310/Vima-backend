package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.dto.FeatureFlagResponseDto;
import com.vimainsurance.vimaadmin.entity.FeatureFlag;
import com.vimainsurance.vimaadmin.entity.FeatureFlagCompany;
import com.vimainsurance.vimaadmin.entity.FeatureFlagRole;
import com.vimainsurance.vimaadmin.repository.IFeatureFlagRepository;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import com.vimainsurance.vimaadmin.specification.FeatureFlagSpecification;
import com.vimainsurance.vimaadmin.util.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureFlagServiceImpl implements FeatureFlagService {

    @Autowired
    private IFeatureFlagRepository featureFlagRepository;


    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlagResponseDto> findAllMatchedFeatureFlags() {

        // Fetch flags with roles and companies to avoid N+1
        List<FeatureFlag> featureFlags = featureFlagRepository.findAll(FeatureFlagSpecification.fetchRelations());

        Map<String, List<String>> currentTenant = TenantContext.getCurrentTenant();
        // Support both keys "ROLES" and "roles" (tenant source may vary)
        final List<String> rolesFromContext = new ArrayList<>();
        if (currentTenant != null) {
            rolesFromContext.addAll(currentTenant.getOrDefault("ROLES", List.of()));
            rolesFromContext.addAll(currentTenant.getOrDefault("roles", List.of()));
        }
        final Set<String> normalizedRoles = rolesFromContext.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        final List<String> organizationIds = (currentTenant != null) ? currentTenant.getOrDefault("organizationIds", List.of()) : List.of();
        final Set<String> orgIdSet = organizationIds.stream().filter(id -> id != null && !id.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new));

        List<FeatureFlagResponseDto> responseDtos = new ArrayList<>();

        for (FeatureFlag flag : featureFlags) {
            if (flag == null) continue;
            // skip inactive flags
           // if (flag.getIsActive() != null && !flag.getIsActive()) continue;

            // Collect matching roles
            List<FeatureFlagRole> matchedRoles = new ArrayList<>();
            if (flag.getRoles() != null) {
                for (FeatureFlagRole role : flag.getRoles()) {
                    if (role == null || role.getRoleName() == null) continue;
                    if (normalizedRoles.contains(role.getRoleName())) {
                        matchedRoles.add(role);
                    }
                }
            }

            // Collect matching companies by organization id
            List<FeatureFlagCompany> matchedCompanies = new ArrayList<>();
            if (flag.getCompanies() != null) {
                for (FeatureFlagCompany comp : flag.getCompanies()) {
                    if (comp == null || comp.getOrganization() == null || comp.getOrganization().getOrganizationId() == null) continue;
                    String orgId = comp.getOrganization().getOrganizationId().toString();
                    if (orgIdSet.contains(orgId)) {
                        matchedCompanies.add(comp);
                    }
                }
            }

            // Decide inclusion: include if any matched role or company OR user is SUPER_ADMIN
            boolean isSuperAdmin = normalizedRoles.stream().anyMatch(r -> r.equalsIgnoreCase("ROLE_SUPER_ADMIN"));
            if (!isSuperAdmin && matchedRoles.isEmpty() && matchedCompanies.isEmpty()) {
                continue; // not accessible to this tenant
            }

            // Build DTO
            FeatureFlagResponseDto dto = new FeatureFlagResponseDto();
            // Use flagId as both id and flag_id for now
            dto.setId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
            dto.setFlagId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
            dto.setFlagKey(flag.getFlagKey());
            dto.setDescription(flag.getDescription());
            dto.setIsActive(Boolean.TRUE);

            // Collect actions from matched roles and companies (union)
            Set<String> actionsUnion = new LinkedHashSet<>();
            for (FeatureFlagRole r : matchedRoles) {
                if (r.getActions() != null) {
                    actionsUnion.addAll(Arrays.asList(r.getActions()));
                }
            }
            for (FeatureFlagCompany c : matchedCompanies) {
                if (c.getActions() != null) {
                    actionsUnion.addAll(Arrays.asList(c.getActions()));
                }
            }
            // If super admin and no explicit actions found, optionally expose all role actions
            if (isSuperAdmin && actionsUnion.isEmpty()) {
                // gather all actions from flag
                if (flag.getRoles() != null) {
                    for (FeatureFlagRole r : flag.getRoles()) {
                        if (r.getActions() != null) actionsUnion.addAll(Arrays.asList(r.getActions()));
                    }
                }
                if (flag.getCompanies() != null) {
                    for (FeatureFlagCompany c : flag.getCompanies()) {
                        if (c.getActions() != null) actionsUnion.addAll(Arrays.asList(c.getActions()));
                    }
                }
            }
            dto.setActions(new ArrayList<>(actionsUnion));

            // Map companies (only matched ones)
            List<FeatureFlagResponseDto.CompanyDto> companyDtos = new ArrayList<>();
            for (FeatureFlagCompany c : matchedCompanies) {
                FeatureFlagResponseDto.CompanyDto cd = new FeatureFlagResponseDto.CompanyDto();
                cd.setId(c.getId() != null ? c.getId().toString() : null);
                cd.setFlagId(flag.getFlagId() != null ? flag.getFlagId().toString() : null);
                cd.setOrganizationId(c.getOrganization() != null && c.getOrganization().getOrganizationId() != null ? c.getOrganization().getOrganizationId().toString() : null);
                cd.setActions(c.getActions() != null ? Arrays.asList(c.getActions()) : List.of());
                companyDtos.add(cd);
            }
            dto.setCompanies(companyDtos);

            responseDtos.add(dto);
        }

        return responseDtos;
    }

    private List<FeatureFlagResponseDto> mapToResponseDtos() {
        // Deprecated: mapping is now done in findAllMatchedFeatureFlags
        return List.of();
    }
}

