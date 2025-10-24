package com.vimainsurance.vimaadmin.service.serviceimpl;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IPolicyService;
import com.vimainsurance.vimaadmin.util.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
/**
 * Service implementation for Policy operations
 */
@Service
public class PolicyServiceImpl implements IPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyServiceImpl.class);

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private IDealsRepository dealsRepository;

    @Override
    public ResponseEntity<ResponseDto<String>> createPolicy(PolicyRequestDto requestDto) {
        logger.info("[correlationId:{}] createPolicy called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Check if policy number already exists
            if (policyRepository.existsByPolicyNumber(requestDto.getPolicyNumber())) {
                return responseObj.render(responseObj.formErrorResponse("Policy number already exists"));
            }

            // Create policy entity
            Policy policy = new Policy();
            policy.setPolicyNumber(requestDto.getPolicyNumber());
            policy.setPrimaryIndividualId(requestDto.getPrimaryIndividualId());
            policy.setInsuranceProviderId(requestDto.getInsuranceProviderId());
            policy.setInsuranceProductId(requestDto.getInsuranceProductId());
            policy.setOrganizationId(requestDto.getOrganizationId());
            policy.setProductType(ProductType.fromValue(requestDto.getProductType()));
            policy.setCoverageType(CoverageType.fromValue(requestDto.getCoverageType()));
            policy.setStatus(requestDto.getStatus() != null ? 
                PolicyStatus.fromValue(requestDto.getStatus()) : PolicyStatus.ACTIVE);
            policy.setSumInsured(requestDto.getSumInsured());
            policy.setPremiumAmount(requestDto.getPremiumAmount());
            policy.setStartDate(requestDto.getStartDate());
            policy.setEndDate(requestDto.getEndDate());
            policy.setRenewalDate(requestDto.getRenewalDate());
            policy.setLeadId(requestDto.getLeadId());

            Policy savedPolicy = policyRepository.save(policy);
            logger.info("[correlationId:{}] Policy created successfully with ID: {}", 
                       MDC.get("correlationId"), savedPolicy.getPolicyId());
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in createPolicy: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> updatePolicy(Long policyId, PolicyRequestDto requestDto) {
        logger.info("[correlationId:{}] updatePolicy called for ID: {}", MDC.get("correlationId"), policyId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            Optional<Policy> policyOpt = policyRepository.findById(policyId);
            if (policyOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            Policy policy = policyOpt.get();
            
            // Check if policy number is being changed and if it already exists
            if (!policy.getPolicyNumber().equals(requestDto.getPolicyNumber()) && 
                policyRepository.existsByPolicyNumber(requestDto.getPolicyNumber())) {
                return responseObj.render(responseObj.formErrorResponse("Policy number already exists"));
            }

            // Update policy fields
            policy.setPolicyNumber(requestDto.getPolicyNumber());
            policy.setPrimaryIndividualId(requestDto.getPrimaryIndividualId());
            policy.setInsuranceProviderId(requestDto.getInsuranceProviderId());
            policy.setInsuranceProductId(requestDto.getInsuranceProductId());
            policy.setOrganizationId(requestDto.getOrganizationId());
            policy.setProductType(ProductType.fromValue(requestDto.getProductType()));
            policy.setCoverageType(CoverageType.fromValue(requestDto.getCoverageType()));
            if (requestDto.getStatus() != null) {
                policy.setStatus(PolicyStatus.fromValue(requestDto.getStatus()));
            }
            policy.setPremiumAmount(requestDto.getPremiumAmount());
            policy.setStartDate(requestDto.getStartDate());
            policy.setEndDate(requestDto.getEndDate());
            policy.setRenewalDate(requestDto.getRenewalDate());
            policy.setLeadId(requestDto.getLeadId());

            policyRepository.save(policy);
            logger.info("[correlationId:{}] Policy updated successfully", MDC.get("correlationId"));
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in updatePolicy: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<PolicyResponseDto>> getPolicyById(Long policyId) {
        logger.info("[correlationId:{}] getPolicyById called for ID: {}", MDC.get("correlationId"), policyId);
        BaseResponse<PolicyResponseDto> responseObj = new BaseResponse<>();
        
        try {
            Optional<Policy> policyOpt = policyRepository.findById(policyId);
            if (policyOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            PolicyResponseDto responseDto = mapToResponseDto(policyOpt.get());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPolicyById: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<PolicyResponseDto>> getPolicyByNumber(String policyNumber) {
        logger.info("[correlationId:{}] getPolicyByNumber called for: {}", MDC.get("correlationId"), policyNumber);
        BaseResponse<PolicyResponseDto> responseObj = new BaseResponse<>();
        
        try {
            Optional<Policy> policyOpt = policyRepository.findByPolicyNumber(policyNumber);
            if (policyOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            PolicyResponseDto responseDto = mapToResponseDto(policyOpt.get());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPolicyByNumber: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Page<PolicyResponseDto>>> getAllPolicies(Pageable pageable) {
        logger.info("[correlationId:{}] getAllPolicies called", MDC.get("correlationId"));
        BaseResponse<Page<PolicyResponseDto>> responseObj = new BaseResponse<>();
        
        try {
            Page<Policy> policies = policyRepository.findAll(pageable);
            Page<PolicyResponseDto> responseDtos = policies.map(this::mapToResponseDto);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getAllPolicies: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByIndividualId(UUID individualId) {
        logger.info("[correlationId:{}] getPoliciesByIndividualId called for: {}", MDC.get("correlationId"), individualId);
        BaseResponse<List<PolicyResponseDto>> responseObj = new BaseResponse<>();
        
        try {
            List<Policy> policies = policyRepository.findByPrimaryIndividualId(individualId);
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPoliciesByIndividualId: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByProviderId(UUID providerId) {
        logger.info("[correlationId:{}] getPoliciesByProviderId called for: {}", MDC.get("correlationId"), providerId);
        BaseResponse<List<PolicyResponseDto>> responseObj = new BaseResponse<>();
        
        try {
            List<Policy> policies = policyRepository.findByInsuranceProviderId(providerId);
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPoliciesByProviderId: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByOrganizationId(UUID organizationId) {
        logger.info("[correlationId:{}] getPoliciesByOrganizationId called for: {}", MDC.get("correlationId"), organizationId);
        BaseResponse<List<PolicyResponseDto>> responseObj = new BaseResponse<>();
        
        try {
            List<Policy> policies = policyRepository.findByOrganizationId(organizationId);
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPoliciesByOrganizationId: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByStatus(String status) {
        logger.info("[correlationId:{}] getPoliciesByStatus called for: {}", MDC.get("correlationId"), status);
        BaseResponse<List<PolicyResponseDto>> responseObj = new BaseResponse<>();
        
        try {
            PolicyStatus policyStatus = PolicyStatus.fromValue(status);
            List<Policy> policies = policyRepository.findByStatus(policyStatus);
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPoliciesByStatus: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByLeadId(UUID leadId) {
        logger.info("[correlationId:{}] getPoliciesByLeadId called for: {}", MDC.get("correlationId"), leadId);
        BaseResponse<List<PolicyResponseDto>> responseObj = new BaseResponse<>();
        
        try {
            List<Policy> policies = policyRepository.findByLeadId(leadId);
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPoliciesByLeadId: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getActivePolicies() {
        logger.info("[correlationId:{}] getActivePolicies called", MDC.get("correlationId"));
        BaseResponse<List<PolicyResponseDto>> responseObj = new BaseResponse<>();
        
        try {
            List<Policy> policies = policyRepository.findActivePolicies();
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getActivePolicies: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesExpiringBy(LocalDate expiryDate) {
        logger.info("[correlationId:{}] getPoliciesExpiringBy called for: {}", MDC.get("correlationId"), expiryDate);
        BaseResponse<List<PolicyResponseDto>> responseObj = new BaseResponse<>();
        
        try {
            List<Policy> policies = policyRepository.findPoliciesExpiringBy(expiryDate);
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPoliciesExpiringBy: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> updatePolicyStatus(Long policyId, String status) {
        logger.info("[correlationId:{}] updatePolicyStatus called for ID: {} to status: {}", 
                   MDC.get("correlationId"), policyId, status);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            Optional<Policy> policyOpt = policyRepository.findById(policyId);
            if (policyOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            Policy policy = policyOpt.get();
            policy.setStatus(PolicyStatus.fromValue(status));
            policyRepository.save(policy);
            
            logger.info("[correlationId:{}] Policy status updated successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Policy status updated successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in updatePolicyStatus: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> deletePolicy(Long policyId) {
        logger.info("[correlationId:{}] deletePolicy called for ID: {}", MDC.get("correlationId"), policyId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            Optional<Policy> policyOpt = policyRepository.findById(policyId);
            if (policyOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            Policy policy = policyOpt.get();
            policy.setStatus(PolicyStatus.CANCELLED);
            policyRepository.save(policy);
            
            logger.info("[correlationId:{}] Policy deleted successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in deletePolicy: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Boolean>> checkPolicyNumberExists(String policyNumber) {
        logger.info("[correlationId:{}] checkPolicyNumberExists called for: {}", MDC.get("correlationId"), policyNumber);
        BaseResponse<Boolean> responseObj = new BaseResponse<>();
        
        try {
            boolean exists = policyRepository.existsByPolicyNumber(policyNumber);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, exists));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in checkPolicyNumberExists: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Object>> getPolicyStatistics() {
        logger.info("[correlationId:{}] getPolicyStatistics called", MDC.get("correlationId"));
        BaseResponse<Object> responseObj = new BaseResponse<>();
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalPolicies", policyRepository.count());
            statistics.put("activePolicies", policyRepository.countByStatus(PolicyStatus.ACTIVE));
            statistics.put("lapsedPolicies", policyRepository.countByStatus(PolicyStatus.LAPSED));
            statistics.put("cancelledPolicies", policyRepository.countByStatus(PolicyStatus.CANCELLED));
            statistics.put("expiredPolicies", policyRepository.countByStatus(PolicyStatus.EXPIRED));
            statistics.put("pendingPolicies", policyRepository.countByStatus(PolicyStatus.PENDING));
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, statistics));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getPolicyStatistics: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    /**
     * Map Policy entity to PolicyResponseDto
     */
    private PolicyResponseDto mapToResponseDto(Policy policy) {
        PolicyResponseDto responseDto = new PolicyResponseDto();
        responseDto.setPolicyId(policy.getPolicyId());
        responseDto.setPolicyNumber(policy.getPolicyNumber());
        responseDto.setPrimaryIndividualId(policy.getPrimaryIndividualId());
        responseDto.setInsuranceProviderId(policy.getInsuranceProviderId());
        responseDto.setInsuranceProductId(policy.getInsuranceProductId());
        responseDto.setOrganizationId(policy.getOrganizationId());
        responseDto.setProductType(policy.getProductType().getValue());
        responseDto.setCoverageType(policy.getCoverageType().getValue());
        responseDto.setStatus(policy.getStatus().getValue());
        responseDto.setSumInsured(policy.getSumInsured());
        responseDto.setPremiumAmount(policy.getPremiumAmount());
        responseDto.setStartDate(policy.getStartDate());
        responseDto.setEndDate(policy.getEndDate());
        responseDto.setRenewalDate(policy.getRenewalDate());
        responseDto.setLeadId(policy.getLeadId());
        responseDto.setCreatedAt(policy.getCreatedAt());
        responseDto.setUpdatedAt(policy.getUpdatedAt());
        List<Deals> dependents = dealsRepository.findByIndividualIdIn(policy.getCoveredIndividuals());
        responseDto.setDependents(dependents.stream()
            .map(this::mapToSimplifiedDependent)
            .collect(Collectors.toList()));
        return responseDto;
    }
    
    /**
     * Maps Deals entity to simplified dependent response with only name, date of birth, and relationship
     */
    private DealsResponseDto mapToSimplifiedDependent(com.vimainsurance.vimaadmin.entity.Deals deals) {
        DealsResponseDto responseDto = new DealsResponseDto();
        responseDto.setIndividualId(deals.getIndividualId());
        responseDto.setFirstName(deals.getFirstName());
        responseDto.setLastName(deals.getLastName());
        responseDto.setDateOfBirth(deals.getDateOfBirth());
        responseDto.setRelationship(deals.getRelationship());
        
        return responseDto;
    }
}
