package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.EmployeeInsuranceResponseDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyWordingChecklistDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.entity.InsuranceProvider;
import com.vimainsurance.vimaadmin.entity.Nominee;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.INomineeRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IEmployeeInsuranceService;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.TenantContext;

/**
 * Service implementation for employee insurance operations
 */
@Service
@Transactional(readOnly = true)
public class EmployeeInsuranceServiceImpl implements IEmployeeInsuranceService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeInsuranceServiceImpl.class);

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IPolicyRepository policyRepository;


    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private INomineeRepository nomineeRepository;

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Override
    public EmployeeInsuranceResponseDto getEmployeeInsuranceDetails(UUID employeeId) {
        // Resolve organization ID from tenant context
        UUID organizationId = resolveOrganizationIdFromTenant();
        logger.info("Fetching insurance details for employeeId: {}, organizationId: {}", employeeId, organizationId);

        // Fetch primary employee ensuring it belongs to the tenant organization
        Deals employee = dealsRepository.findByIndividualIdAndOrganizationId(employeeId, organizationId)
                .orElseThrow(() -> new BadRequestException("Employee not found in the current organization"));

        // Verify this is a primary member
        if (!Boolean.TRUE.equals(employee.getIsPrimaryMember())) {
            throw new BadRequestException("Provided ID is not a primary employee");
        }

        // Fetch dependents for this employee
        List<Deals> dependents = dealsRepository.findByPrimaryIndividualId(employee.getIndividualId());
        logger.info("Found {} dependents for employee {}", dependents.size(), employeeId);

        // All members (employee + dependents) for coverage resolution
        List<Deals> allMembers = new ArrayList<>();
        allMembers.add(employee);
        allMembers.addAll(dependents);

        // Fetch all active policies for the organization (GMC, GTL, GPA, etc.)
        List<Policy> orgPolicies = policyRepository.findByOrganizationIdAndStatus(organizationId, PolicyStatus.ACTIVE);
        logger.info("Found {} active policies for organization {}", orgPolicies.size(), organizationId);

        // Build one PolicyDetailDto per policy: GMC has coveredMembers, GTL/GPA have nominees
        List<EmployeeInsuranceResponseDto.PolicyDetailDto> policyDetails = new ArrayList<>();
        for (Policy policy : orgPolicies) {
            List<EmployeeInsuranceResponseDto.CoveredMemberDto> coveredForPolicy = coveredMembersForPolicy(allMembers, policy);
            List<EmployeeInsuranceResponseDto.NomineeDto> nomineesForPolicy = nomineesForPolicy(employee.getIndividualId(), policy);
            Integer multiplier = resolveSumInsuredMultiplier(policy);
            policyDetails.add(EmployeeInsuranceResponseDto.PolicyDetailDto.builder()
                    .insuranceType(policy.getProductType() != null ? policy.getProductType().getValue() : null)
                    .coverageType(policy.getCoverageType() != null ? policy.getCoverageType().getValue() : null)
                    .insuranceProviderLogo(resolveInsuranceProviderName(policy.getInsuranceProviderId()))
                    .policyId(policy.getPolicyId())
                    .policyNumber(policy.getPolicyNumber())
                    .validUntil(policy.getEndDate())
                    .policyStatus(policy.getStatus())
                    .sumInsured(policy.getSumInsured())
                    .sumInsuredMultiplier(multiplier)
                    .premiumAmount(policy.getPremiumAmount())
                    .policyStartDate(policy.getStartDate())
                    .tpaOrganizationName(policy.getTpaOrganizationName())
                    .tpaContactInfo(policy.getTpaContactInfo())
                    .coveredMembers(coveredForPolicy)
                    .nominees(nomineesForPolicy)
                    .build());
        }

        // Primary policy for backward compatibility: first policy in list, or first GMC if present
        Policy primaryPolicy = orgPolicies.stream()
                .filter(p -> p.getProductType() != null && "GMC".equals(p.getProductType().getValue()))
                .findFirst()
                .orElse(orgPolicies.isEmpty() ? null : orgPolicies.get(0));

        List<EmployeeInsuranceResponseDto.CoveredMemberDto> primaryCoveredMembers = new ArrayList<>();
        if (primaryPolicy != null) {
            primaryCoveredMembers = coveredMembersForPolicy(allMembers, primaryPolicy);
        }

        Long policyId = primaryPolicy != null ? primaryPolicy.getPolicyId() : null;
        InsuranceProvider primaryProvider = null;
        if (primaryPolicy != null) {
            UUID providerId = primaryPolicy.getInsuranceProviderId();
            if (providerId != null) {
                primaryProvider = insuranceProviderRepository.findById(providerId).orElse(null);
            }
        }

        Organization organization = employee.getOrganization();
        String primaryContactEmail = organization != null ? blankToNull(organization.getPrimaryContactEmail()) : null;
        String primaryContactPhone = organization != null ? blankToNull(organization.getPrimaryContactPhone()) : null;

        // Build and return the response DTO
        return EmployeeInsuranceResponseDto.builder()
                .employeeId(employee.getIndividualId())
                .employeeNumber(employee.getEmployeeNumber())
                .employeeName(employee.getFullName() != null && !employee.getFullName().isBlank() ?
                        employee.getFullName() : ((employee.getFirstName() != null ? employee.getFirstName().trim() : "") +
                        " " + (employee.getLastName() != null ? employee.getLastName().trim() : "")).trim())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .cardType("")
                .healthId(employee.getHealthId())
                .insuranceType(primaryPolicy != null && primaryPolicy.getProductType() != null ? primaryPolicy.getProductType().getValue() : null)
                .coverageType(primaryPolicy != null && primaryPolicy.getCoverageType() != null ? primaryPolicy.getCoverageType().getValue() : null)
                .insuranceProviderLogo(primaryPolicy != null ?
                        resolveInsuranceProviderName(primaryPolicy.getInsuranceProviderId()) : null)
                .policyId(policyId)
                .policyNumber(primaryPolicy != null ? primaryPolicy.getPolicyNumber() : null)
                .validUntil(primaryPolicy != null ? primaryPolicy.getEndDate() : null)
                .policyStatus(primaryPolicy != null ? primaryPolicy.getStatus() : null)
                .sumInsured(primaryPolicy != null ? primaryPolicy.getSumInsured() : null)
                .sumInsuredMultiplier(primaryPolicy != null ? resolveSumInsuredMultiplier(primaryPolicy) : null)
                .premiumAmount(primaryPolicy != null ? primaryPolicy.getPremiumAmount() : null)
                .policyStartDate(primaryPolicy != null ? primaryPolicy.getStartDate() : null)
                .tpaOrganizationName(primaryPolicy != null ? primaryPolicy.getTpaOrganizationName() : null)
                .tpaContactInfo(primaryPolicy != null ? primaryPolicy.getTpaContactInfo() : null)
                .networkHospitalsUrl(primaryProvider != null ? primaryProvider.getNetworkHospitalsUrl() : null)
                .blacklistedHospitalsUrl(primaryProvider != null ? primaryProvider.getBlacklistedHospitalsUrl() : null)
                .companyName(organization != null ? organization.getOrganizationName() : null)
                .primaryContactEmail(primaryContactEmail)
                .primaryContactPhone(primaryContactPhone)
                .coveredMembers(primaryCoveredMembers)
                .policies(policyDetails)
                .build();

    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public EmployeePolicyWordingChecklistDto getEmployeePolicyWordingChecklist(Long policyId) {
        if (policyId == null) {
            throw new BadRequestException("Policy ID is required");
        }
        UUID organizationId = resolveOrganizationIdFromTenant();
        UUID currentEmployeeId = jwtUserExtractor.getCurrentEmployeeId();
        if (currentEmployeeId == null) {
            throw new BadRequestException("Employee ID not found in authentication token");
        }

        dealsRepository.findByIndividualIdAndOrganizationId(currentEmployeeId, organizationId)
                .orElseThrow(() -> new BadRequestException("Employee not found in the current organization"));

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new BadRequestException("Policy not found"));
        if (policy.getOrganizationId() == null || !organizationId.equals(policy.getOrganizationId())) {
            throw new BadRequestException("Access denied: policy does not belong to your organization");
        }

        return EmployeePolicyWordingChecklistDto.builder()
                .policyId(policy.getPolicyId())
                .policyNumber(policy.getPolicyNumber())
                .productType(policy.getProductType() != null ? policy.getProductType().getValue() : null)
                .policyWordingSummary(blankToNull(policy.getPolicyWordingSummary()))
                .claimChecklistAdditionalDocs(blankToNull(policy.getClaimChecklistAdditionalDocs()))
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    /**
     * Resolve sum insured multiplier for response. Uses policy.getSumInsuredMultiplier() when set.
     * For legacy GPA/GTL policies that stored multiplier in sum_insured (1–5), returns that value so frontend can show "N× CTC".
     */
    private Integer resolveSumInsuredMultiplier(Policy policy) {
        if (policy.getSumInsuredMultiplier() != null) {
            return policy.getSumInsuredMultiplier();
        }
        if ((policy.getProductType() == ProductType.GPA || policy.getProductType() == ProductType.GTL)
                && policy.getSumInsured() != null) {
            try {
                int v = policy.getSumInsured().intValue();
                if (v >= 1 && v <= 5) {
                    return v;
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Covered members per policy: GMC/GHI floater (employee + spouse + children, no parents);
     * PARENT_GMC only parent/in-law rows; GPA/GTL return empty (nominees used for accident/life).
     */
    private List<EmployeeInsuranceResponseDto.CoveredMemberDto> coveredMembersForPolicy(List<Deals> allMembers, Policy policy) {
        if (policy.getProductType() == null) {
            return Collections.emptyList();
        }
        ProductType pt = policy.getProductType();
        if (pt == ProductType.GPA || pt == ProductType.GTL) {
            return Collections.emptyList();
        }
        Long policyId = policy.getPolicyId();
        if (pt == ProductType.PARENT_GMC) {
            return allMembers.stream()
                    .filter(d -> isParentRelationshipForPolicy(d.getRelationship()))
                    .map(d -> mapToCoveredMember(d, policyId, policy))
                    .collect(Collectors.toList());
        }
        if (pt == ProductType.GMC || pt == ProductType.GHI) {
            return allMembers.stream()
                    .filter(d -> !isParentRelationshipForPolicy(d.getRelationship()))
                    .map(d -> mapToCoveredMember(d, policyId, policy))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean isParentRelationshipForPolicy(String relationship) {
        if (relationship == null) {
            return false;
        }
        String r = relationship.trim();
        if ("FATHER".equalsIgnoreCase(r) || "MOTHER".equalsIgnoreCase(r)) {
            return true;
        }
        String compact = r.replaceAll("[\\s_-]+", "").toUpperCase(Locale.ROOT);
        return "FATHERINLAW".equals(compact) || "MOTHERINLAW".equals(compact);
    }

    /**
     * Nominees are only attached to GTL and GPA. GMC returns empty list.
     * Fetches nominees for this employee (customer) where policy type is GTL or GPA, then scoped to this policy.
     */
    private List<EmployeeInsuranceResponseDto.NomineeDto> nomineesForPolicy(UUID employeeId, Policy policy) {
        if (policy.getProductType() == null || (policy.getProductType() != ProductType.GTL && policy.getProductType() != ProductType.GPA)) {
            return Collections.emptyList();
        }
        List<Nominee> nominees = nomineeRepository.findByCustomerIndividualId(employeeId);
        return nominees.stream()
                .map(this::mapToNomineeDto)
                .collect(Collectors.toList());
    }

    private EmployeeInsuranceResponseDto.NomineeDto mapToNomineeDto(Nominee n) {
        String fullName = n.getFullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = ((n.getFirstName() != null ? n.getFirstName().trim() : "") + " " + (n.getLastName() != null ? n.getLastName().trim() : "")).trim();
        }
        return EmployeeInsuranceResponseDto.NomineeDto.builder()
                .nomineeId(n.getNomineeId())
                .firstName(n.getFirstName())
                .lastName(n.getLastName())
                .fullName(fullName)
                .dateOfBirth(n.getDateOfBirth())
                .gender(n.getGender())
                .relationship(n.getRelationship())
                .nomineePercentage(n.getNomineePercentage())
                .build();
    }

    /**
     * Map Deals entity to CoveredMemberDto
     */
    private EmployeeInsuranceResponseDto.CoveredMemberDto mapToCoveredMember(Deals deal, Long policyId, Policy primaryPolicy) {
        // Parse sum insured from string to BigDecimal
        BigDecimal sumInsured = null;
        if (deal.getSumInsured() != null && !deal.getSumInsured().isEmpty()) {
            try {
                sumInsured = new BigDecimal(deal.getSumInsured().replace(",", ""));
            } catch (NumberFormatException e) {
                logger.warn("Invalid sum insured format for individual {}: {}", deal.getIndividualId(), deal.getSumInsured());
            }
        }

        return EmployeeInsuranceResponseDto.CoveredMemberDto.builder()
                .policyId(policyId)
                .policyNumber(primaryPolicy != null ? primaryPolicy.getPolicyNumber() : null)
                .insuranceProviderLogo(primaryPolicy != null ?
                        resolveInsuranceProviderName(primaryPolicy.getInsuranceProviderId()) : null)
                .healthId(deal.getHealthId())
                .individualId(deal.getIndividualId())
                .fullName(deal.getFullName() != null && !deal.getFullName().isBlank() ?
                        deal.getFullName() : ((deal.getFirstName() != null ? deal.getFirstName().trim() : "") +
                        " " + (deal.getLastName() != null ? deal.getLastName().trim() : "")).trim())
                .relationship(deal.getRelationship())
                .actualRelationship(deal.getActualRelationship())
                .dateOfBirth(deal.getDateOfBirth())
                .gender(deal.getGender())
                .email(deal.getEmail())
                .phone(deal.getPhone())
                .status(deal.getStatus() != null ? deal.getStatus().getValue() : null)
                .sumInsured(sumInsured)
                // TPA Details
                .tpaOrganizationName(primaryPolicy != null ? primaryPolicy.getTpaOrganizationName() : null)
                .tpaContactInfo(primaryPolicy != null ? primaryPolicy.getTpaContactInfo() : null)
                .companyName(deal.getOrganization() != null ? deal.getOrganization().getOrganizationName() : null)
                .build();
    }

    /**
     * Resolve insurance provider name by ID
     */
    private String resolveInsuranceProviderName(UUID insuranceProviderId) {
        if (insuranceProviderId == null) {
            return null;
        }
        return insuranceProviderRepository.findById(insuranceProviderId)
                .map(InsuranceProvider::getProviderName)
                .orElse(null);
    }

    /**
     * Resolve organization ID from TenantContext
     */
    private UUID resolveOrganizationIdFromTenant() {
        Map<String, List<String>> tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new BadRequestException("Tenant context not set. Please provide organization header.");
        }

        // Try to get organization_id from headers
        String orgId = Optional.ofNullable(tenant.get("organizationIds"))
                .filter(l -> !l.isEmpty())
                .map(l -> l.get(0))
                .orElseGet(() -> Optional.ofNullable(tenant.get("X-Organization-Id"))
                        .filter(l -> !l.isEmpty())
                        .map(l -> l.get(0))
                        .orElse(null));

        if (orgId == null || orgId.isEmpty()) {
            throw new BadRequestException("Organization ID is missing in request headers");
        }

        try {
            return UUID.fromString(orgId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid organization ID format: " + orgId);
        }
    }
}
