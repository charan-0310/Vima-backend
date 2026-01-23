package com.vimainsurance.vimaadmin.service.serviceimpl;

import com.vimainsurance.vimaadmin.dto.EmployeeInsuranceResponseDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.InsuranceProvider;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IEmployeeInsuranceService;
import com.vimainsurance.vimaadmin.util.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

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

        // Fetch policies for the employee (primary individual)
        List<Policy> policies = policyRepository.findByPrimaryIndividualId(organizationId);

        // Use the first active policy or fallback to the first policy
        Policy primaryPolicy = policies.stream()
                .filter(p -> p.getStatus() != null && PolicyStatus.ACTIVE.equals(p.getStatus()))
                .findFirst()
                .orElse(policies.isEmpty() ? null : policies.get(0));

        if (primaryPolicy == null) {
            logger.warn("No policy found for employee {}", employeeId);
        }

        // Build covered members list: employee + dependents
        List<EmployeeInsuranceResponseDto.CoveredMemberDto> coveredMembers = new ArrayList<>();
        Long policyId = primaryPolicy != null ? primaryPolicy.getPolicyId() : null;

        // Add primary employee to covered members
        coveredMembers.add(mapToCoveredMember(employee, policyId));

        // Add all dependents to covered members
        dependents.forEach(dependent -> coveredMembers.add(mapToCoveredMember(dependent, policyId)));


        // Build and return the response DTO
        return EmployeeInsuranceResponseDto.builder()
                .employeeId(employee.getIndividualId())
                .employeeNumber(employee.getEmployeeNumber())
                .employeeName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .cardType("")
                .healthId(employee.getHealthId())
                .insuranceType(primaryPolicy != null  && primaryPolicy.getProductType() !=null ? primaryPolicy.getProductType().getValue() : null)
                .coverageType(primaryPolicy != null && primaryPolicy.getCoverageType() !=null ? primaryPolicy.getCoverageType().getValue() : null)
                .insuranceProviderLogo(primaryPolicy != null ?
                        resolveInsuranceProviderName(primaryPolicy.getInsuranceProviderId()) : null)
                .policyId(policyId)
                .policyNumber(primaryPolicy != null ? primaryPolicy.getPolicyNumber() : null)
                .validUntil(primaryPolicy != null ? primaryPolicy.getEndDate() : null)
                .policyStatus(primaryPolicy != null ? primaryPolicy.getStatus() : null)
                .sumInsured(primaryPolicy != null ? primaryPolicy.getSumInsured() : null)
                .premiumAmount(primaryPolicy != null ? primaryPolicy.getPremiumAmount() : null)
                .policyStartDate(primaryPolicy != null ? primaryPolicy.getStartDate() : null)

                .coveredMembers(coveredMembers)
                .build();

    }

    /**
     * Map Deals entity to CoveredMemberDto
     */
    private EmployeeInsuranceResponseDto.CoveredMemberDto mapToCoveredMember(Deals deal, Long policyId) {
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
                .healthId(deal.getHealthId())
                .individualId(deal.getIndividualId())
                .fullName(deal.getFullName())
                .relationship(deal.getRelationship())
                .dateOfBirth(deal.getDateOfBirth())
                .gender(deal.getGender())
                .email(deal.getEmail())
                .phone(deal.getPhone())
                .status(deal.getStatus() != null ? deal.getStatus().getValue() : null)
                .sumInsured(sumInsured)
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
