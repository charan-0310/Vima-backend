package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.Nominee;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.PaymentFrequency;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.exception.OrganizationAccessDeniedException;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.IMotorPolicyDetailsRepository;
import com.vimainsurance.vimaadmin.repository.INomineeRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IPolicyService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.PolicyValidationUtil;
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

    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private INomineeRepository nomineeRepository;

    @Autowired
    private IMotorPolicyDetailsRepository motorPolicyDetailsRepository;

    @Autowired
    private IDocumentRepository documentRepository;

    @Autowired
    private JwtUserExtractor jwtUserExtractor;
    
    @Autowired
    private IDocumentService documentService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<String>> createPolicy(PolicyRequestDto requestDto) {
        logger.info("[correlationId:{}] createPolicy called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Validate policy request based on policy type
            PolicyValidationUtil.validatePolicyRequest(requestDto);

            // Check if policy number already exists
            if (policyRepository.existsByPolicyNumber(requestDto.getPolicyNumber())) {
                return responseObj.render(responseObj.formErrorResponse("Policy number already exists"));
            }
            final String currentUsername = jwtUserExtractor.extractCurrentUsername();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(currentUsername);
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            AdminUser agent = adminUser.get();
            Deals primaryIndividual = dealsRepository.findById(requestDto.getPrimaryIndividualId()).orElseThrow(() -> new RuntimeException("Primary individual not found"));

            // Get policy type
            ProductType policyType = ProductType.fromValue(requestDto.getProductType());

            // Create and save dependents first (only for GMC policies)
            List<UUID> coveredIndividualIds = new ArrayList<>();
            if (policyType == ProductType.GMC && requestDto.getDependents() != null && !requestDto.getDependents().isEmpty()) {
                for (DealsRequestDto dependentDto : requestDto.getDependents()) {
                    // Create dependent as a new Deals entity
                    Deals dependent = new Deals();
                    dependent.setFirstName(dependentDto.getFirstName());
                    dependent.setLastName(dependentDto.getLastName());
                    dependent.setEmail(primaryIndividual.getEmail());
                    dependent.setPhone(primaryIndividual.getPhone());
                    dependent.setDateOfBirth(dependentDto.getDateOfBirth());
                    dependent.setGender(dependentDto.getGender());
                    dependent.setPanNumber(dependentDto.getPanNumber());
                    dependent.setAadhaarNumber(dependentDto.getAadhaarNumber());
                    dependent.setAddress(primaryIndividual.getAddress());
                    dependent.setCity(primaryIndividual.getCity());
                    dependent.setState(primaryIndividual.getState());
                    dependent.setPincode(primaryIndividual.getPincode());
                    dependent.setAccountType(AccountType.RETAIL_DEPENDENT);
                    dependent.setStatus(AccountStatus.ACTIVE);
                    dependent.setEmployeeNumber(dependentDto.getEmployeeNumber());
                    dependent.setRelationship(dependentDto.getRelationship());
                    dependent.setIsPrimaryMember(false);
                    dependent.setPrimaryIndividual(primaryIndividual);
                    dependent.setCreatedAt(LocalDateTime.now());
                    dependent.setUpdatedAt(LocalDateTime.now());
                    
                    // Save dependent
                    Deals savedDependent = dealsRepository.save(dependent);
                    coveredIndividualIds.add(savedDependent.getIndividualId());
                    logger.info("[correlationId:{}] Dependent saved with ID: {}", MDC.get("correlationId"), savedDependent.getIndividualId());
                }
            }

            // Create policy entity
            Policy policy = new Policy();
            policy.setPolicyNumber(requestDto.getPolicyNumber());
            policy.setPrimaryIndividualId(requestDto.getPrimaryIndividualId());
            logger.info("[correlationId:{}]  Insurance Company Code: {}", MDC.get("correlationId"), requestDto.getInsuranceCompanyCode());
            policy.setInsuranceProviderId(insuranceProviderRepository.findByProviderCode(requestDto.getInsuranceCompanyCode()).orElseThrow(() -> new RuntimeException("Insurance provider not found")).getProviderId());
            policy.setInsuranceProductId(requestDto.getInsuranceProductId());
            policy.setOrganizationId(requestDto.getOrganizationId());
            policy.setDocument(resolveDocument(requestDto.getDocumentId()));

            // Set product type (used for policy type: GMC, GPA, GTL, or traditional types)
            policy.setProductType(policyType);

            // Set policy category
            policy.setProductType(requestDto.getProductType() != null ?
                ProductType.fromValue(requestDto.getProductType()) : ProductType.EMPLOYEE);
            policy.setAppliesToEmployees(requestDto.getAppliesToEmployees() != null ?
                requestDto.getAppliesToEmployees() : true);

            // Set coverage type (for GMC: E, ES, ESC, ESCP; for traditional: INDIVIDUAL, FAMILY_FLOATER, GROUP)
            if (requestDto.getCoverageType() != null && !requestDto.getCoverageType().isEmpty()) {
                policy.setCoverageType(CoverageType.fromValue(requestDto.getCoverageType()));
            }

            // Set sum insured multiplier (for GPA/GTL only)
            if ((policyType == ProductType.GPA || policyType == ProductType.GTL) && requestDto.getSumInsuredMultiplier() != null) {
                policy.setSumInsuredMultiplier(requestDto.getSumInsuredMultiplier());
            }

            policy.setStatus(requestDto.getStatus() != null ?
                PolicyStatus.fromValue(requestDto.getStatus()) : PolicyStatus.ACTIVE);
            policy.setSumInsured(requestDto.getSumInsured());
            policy.setPremiumAmount(requestDto.getPremiumAmount());
            policy.setStartDate(requestDto.getStartDate());
            policy.setEndDate(requestDto.getEndDate());
            policy.setRenewalDate(requestDto.getRenewalDate());
            policy.setLeadId(requestDto.getLeadId());
            if (requestDto.getPaymentFrequency() != null && !requestDto.getPaymentFrequency().isEmpty()) {
                policy.setPaymentFrequency(PaymentFrequency.fromValue(requestDto.getPaymentFrequency()));
            } else {
                policy.setPaymentFrequency(PaymentFrequency.YEARLY);
            }
            policy.setCoveredIndividuals(coveredIndividualIds);

            // Set TPA details (for GMC only)
            if (policyType == ProductType.GMC) {
                policy.setTpaOrganizationName(requestDto.getTpaOrganizationName());
                policy.setTpaContactInfo(requestDto.getTpaContactInfo());
            }

            Policy savedPolicy = policyRepository.save(policy);
            logger.info("[correlationId:{}] Policy created successfully with ID: {}", 
                       MDC.get("correlationId"), savedPolicy.getPolicyId());
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        } catch (BadRequestException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Validation error in createPolicy: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Exception in createPolicy: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> updatePolicy(Long policyId, PolicyRequestDto requestDto) {
        logger.info("[correlationId:{}] updatePolicy called for ID: {}", MDC.get("correlationId"), policyId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Validate policy request based on policy type
            PolicyValidationUtil.validatePolicyRequest(requestDto);

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

            // Get policy type
            ProductType policyType = ProductType.fromValue(requestDto.getProductType());

            // Update policy fields
            policy.setPolicyNumber(requestDto.getPolicyNumber());
            policy.setPrimaryIndividualId(requestDto.getPrimaryIndividualId());
            policy.setInsuranceProviderId(insuranceProviderRepository.findByProviderCode(requestDto.getInsuranceCompanyCode()).orElseThrow(() -> new RuntimeException("Insurance provider not found")).getProviderId());
            policy.setInsuranceProductId(requestDto.getInsuranceProductId());
            policy.setOrganizationId(requestDto.getOrganizationId());
            policy.setDocument(resolveDocument(requestDto.getDocumentId()));

            // Update product type (used for policy type: GMC, GPA, GTL, or traditional types)
            policy.setProductType(policyType);

            // Update policy category
            policy.setProductType(requestDto.getProductType() != null ?
                ProductType.fromValue(requestDto.getProductType()) : ProductType.EMPLOYEE);
            policy.setAppliesToEmployees(requestDto.getAppliesToEmployees() != null ?
                requestDto.getAppliesToEmployees() : true);

            // Update coverage type (for GMC: E, ES, ESC, ESCP; for traditional: INDIVIDUAL, FAMILY_FLOATER, GROUP)
            if (requestDto.getCoverageType() != null && !requestDto.getCoverageType().isEmpty()) {
                policy.setCoverageType(CoverageType.fromValue(requestDto.getCoverageType()));
            } else {
                policy.setCoverageType(null);
            }

            // Update sum insured multiplier (for GPA/GTL only)
            // Update sum insured multiplier (for GPA/GTL only)
            if ((policyType == ProductType.GPA || policyType == ProductType.GTL) && requestDto.getSumInsuredMultiplier() != null) {
                policy.setSumInsuredMultiplier(requestDto.getSumInsuredMultiplier());
            } else {
                policy.setSumInsuredMultiplier(null);
            }

            if (requestDto.getStatus() != null) {
                policy.setStatus(PolicyStatus.fromValue(requestDto.getStatus()));
            }
            policy.setSumInsured(requestDto.getSumInsured());
            policy.setPremiumAmount(requestDto.getPremiumAmount());
            policy.setStartDate(requestDto.getStartDate());
            policy.setEndDate(requestDto.getEndDate());
            policy.setRenewalDate(requestDto.getRenewalDate());
            policy.setLeadId(requestDto.getLeadId());
            if (requestDto.getPaymentFrequency() != null && !requestDto.getPaymentFrequency().isEmpty()) {
                policy.setPaymentFrequency(PaymentFrequency.fromValue(requestDto.getPaymentFrequency()));
            }

            // Update TPA details (for GMC only)
            if (policyType == ProductType.GMC) {
                policy.setTpaOrganizationName(requestDto.getTpaOrganizationName());
                policy.setTpaContactInfo(requestDto.getTpaContactInfo());
            } else {
                policy.setTpaOrganizationName(null);
                policy.setTpaContactInfo(null);
            }

            policyRepository.save(policy);
            logger.info("[correlationId:{}] Policy updated successfully", MDC.get("correlationId"));
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        } catch (BadRequestException e) {
            logger.error("[correlationId:{}] Validation error in updatePolicy: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
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
            jwtUserExtractor.validateOrganizationAccess(organizationId);
            List<Policy> policies = policyRepository.findByOrganizationId(organizationId);
            Long employeesCount = dealsRepository.countByOrganizationIdAndRelationshipSelf(organizationId);
            Long dependentsCount = dealsRepository.countByOrganizationIdAndRelationshipNonSelf(organizationId);
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(policy -> {
                    PolicyResponseDto responseDto = mapToResponseDto(policy);
                    responseDto.setEmployeesCount(employeesCount);
                    responseDto.setDependentsCount(dependentsCount);
                    return responseDto;
                })
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtos));
        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied for organizationId: {}", MDC.get("correlationId"), organizationId);
            return responseObj.render(responseObj.formErrorResponse(403, "Organization access denied"));
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
            // Document document = policyOpt.get().getDocument();
            policyRepository.delete(policy);
            
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
        responseDto.setInsuranceProvider(insuranceProviderRepository.findById(policy.getInsuranceProviderId()).orElseThrow(() -> new RuntimeException("Insurance provider not found")).getProviderName());
        responseDto.setInsuranceProductId(policy.getInsuranceProductId());
        responseDto.setOrganizationId(policy.getOrganizationId());
        responseDto.setDocumentId(policy.getDocument() != null ? policy.getDocument().getDocumentId() : null);

        // Map policy type (using productType field) and category
        responseDto.setProductType(policy.getProductType() != null ? policy.getProductType().getValue() : null);
        responseDto.setAppliesToEmployees(policy.getAppliesToEmployees());

        responseDto.setProductType(policy.getProductType().getValue());

        // Map coverage type (E, ES, ESC, ESCP for GMC or INDIVIDUAL, FAMILY_FLOATER, GROUP for traditional)
        responseDto.setCoverageType(policy.getCoverageType() != null ? policy.getCoverageType().getValue() : null);

        responseDto.setStatus(policy.getStatus().getValue());
        responseDto.setSumInsured(policy.getSumInsured());
        responseDto.setSumInsuredMultiplier(policy.getSumInsuredMultiplier());
        responseDto.setPremiumAmount(policy.getPremiumAmount());
        responseDto.setStartDate(policy.getStartDate());
        responseDto.setEndDate(policy.getEndDate());
        responseDto.setRenewalDate(policy.getRenewalDate());
        responseDto.setLeadId(policy.getLeadId());
        responseDto.setPaymentFrequency(policy.getPaymentFrequency() != null ? policy.getPaymentFrequency().getValue() : null);
        responseDto.setCreatedAt(policy.getCreatedAt());
        responseDto.setUpdatedAt(policy.getUpdatedAt());
        responseDto.setNetAmount(policy.getNetAmount());
        responseDto.setGst(policy.getGst());

        // Map TPA details (for GMC only)
        responseDto.setTpaOrganizationName(policy.getTpaOrganizationName());
        responseDto.setTpaContactInfo(policy.getTpaContactInfo());

        List<Deals> dependents = dealsRepository.findByIndividualIdIn(policy.getCoveredIndividuals());
        responseDto.setDependents(dependents.stream()
            .map(this::mapToSimplifiedDependent)
            .collect(Collectors.toList()));
        List<Nominee> nominees = nomineeRepository.findByPolicyPolicyId(policy.getPolicyId());
        
        return responseDto;
    }
    
    private Document resolveDocument(UUID documentId) {
        if (documentId == null) {
            return null;
        }
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
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

   
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<String>> uploadPolicyForOrganization(UUID organizationId, PolicyUploadRequestDto requestDto) {
        logger.info("[correlationId:{}] uploadPolicyForOrganization called for organizationId: {}", MDC.get("correlationId"), organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            // Get current user
            final String currentUsername = jwtUserExtractor.extractCurrentUsername();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(currentUsername);
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            AdminUser agent = adminUser.get();
            
            // For organization policies, try to find a primary individual from the organization
            // If not found, we'll use organizationId as primaryIndividualId directly
           
                 
            
            // Create Policy entity
            Policy policy = new Policy();
            policy.setPolicyNumber(requestDto.getPolicyNumber());
            // Use organizationId as primaryIndividualId as per requirement
            policy.setPrimaryIndividualId(organizationId);
            policy.setInsuranceProviderId(insuranceProviderRepository.findByProviderCode(requestDto.getProviderCode())
                .orElseThrow(() -> new RuntimeException("Insurance provider not found")).getProviderId());
            policy.setOrganizationId(organizationId);
            policy.setProductType(ProductType.valueOf(requestDto.getProductType()));
            policy.setCoverageType(CoverageType.valueOf(requestDto.getCoverageType()));
            policy.setStatus(PolicyStatus.valueOf(requestDto.getStatus()));
            policy.setCoveredIndividuals(Arrays.asList(organizationId));

            // GMC: sum_insured = coverage amount; no multiplier.
            // GPA/GTL: MULTIPLIER → sum_insured_multiplier set, sum_insured null; FIXED → sum_insured set, sum_insured_multiplier null.
            ProductType productType = policy.getProductType();
            if (productType == ProductType.GMC) {
                policy.setSumInsured(requestDto.getSumInsured());
                policy.setSumInsuredMultiplier(null);
            } else if (productType == ProductType.GPA || productType == ProductType.GTL) {
                if ("FIXED".equalsIgnoreCase(requestDto.getSumInsuredOption())) {
                    policy.setSumInsured(requestDto.getSumInsured());
                    policy.setSumInsuredMultiplier(null);
                } else {
                    policy.setSumInsuredMultiplier(requestDto.getSumInsuredMultiplier());
                    policy.setSumInsured(null);
                }
            } else {
                policy.setSumInsured(requestDto.getSumInsured());
                policy.setSumInsuredMultiplier(null);
            }

            policy.setPremiumAmount(requestDto.getPremiumAmount());
            policy.setStartDate(requestDto.getStartDate());
            policy.setEndDate(requestDto.getEndDate());
            policy.setRenewalDate(requestDto.getRenewalDate());
            policy.setNetAmount(requestDto.getNetAmount());
            policy.setGst(requestDto.getGst());
            policy.setLeadId(organizationId); // Use organizationId as leadId
            if (requestDto.getPaymentFrequency() != null && !requestDto.getPaymentFrequency().isEmpty()) {
                policy.setPaymentFrequency(PaymentFrequency.fromValue(requestDto.getPaymentFrequency()));
            } else {
                policy.setPaymentFrequency(PaymentFrequency.YEARLY);
            }

            // Set TPA details
            policy.setTpaOrganizationName(requestDto.getTpaOrganizationName());
            policy.setTpaContactInfo(requestDto.getTpaContactInfo());

            policy.setCreatedAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());
            
            // Save policy to database
            Policy savedPolicy = policyRepository.save(policy);
            logger.info("[correlationId:{}] Policy saved with ID: {}", MDC.get("correlationId"), savedPolicy.getPolicyId());
            
            // Upload documents if provided
            if (requestDto.getFiles() != null && requestDto.getFiles().length > 0) {
                DocumentRequestDto documentRequest = new DocumentRequestDto();
                documentRequest.setFiles(requestDto.getFiles());
                documentRequest.setDocumentType(requestDto.getDocumentType());
                documentRequest.setNotes(requestDto.getNotes());
                documentRequest.setUploadedBy(agent.getId());
                documentRequest.setUploadedByRole(UserRole.fromValue(agent.getRole()));
                
                ResponseEntity<ResponseDto<List<com.vimainsurance.vimaadmin.entity.Document>>> documentResponse = documentService.uploadKYCDocuments(
                    documentRequest.getFiles(), 
                    organizationId.toString(), 
                    DocumentEntityType.POLICY, 
                    DocumentType.fromValue(requestDto.getDocumentType()), 
                    documentRequest.getUploadedBy(), 
                    documentRequest.getUploadedByRole(), 
                    documentRequest.getNotes(),
                    DocumentCategory.POLICY_DOCUMENTS
                );
                if (documentResponse.getBody() != null && documentResponse.getBody().getPayload() != null && !documentResponse.getBody().getPayload().isEmpty()) {
                    savedPolicy.setDocument(documentResponse.getBody().getPayload().get(0));
                    policyRepository.save(savedPolicy);
                }
                if(documentResponse.getBody() != null && documentResponse.getBody().getErrorCode() != null){
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return responseObj.render(responseObj.formErrorResponse(documentResponse.getBody().getMessage()));
                }
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Policy created and documents uploaded successfully"));
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Exception in uploadPolicyForOrganization: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
 }
