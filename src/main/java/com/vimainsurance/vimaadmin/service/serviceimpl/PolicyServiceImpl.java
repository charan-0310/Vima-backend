package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyDocumentRefDto;
import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.ProductCatalogRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.CdAccount;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.Nominee;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.entity.ProductCatalog;
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
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.ICdAccountRepository;
import com.vimainsurance.vimaadmin.repository.ICdBalanceTransactionRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.IMotorPolicyDetailsRepository;
import com.vimainsurance.vimaadmin.repository.INomineeRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.repository.IProductCatalogRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IPolicyService;
import com.vimainsurance.vimaadmin.service.IProductCatalogService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.audit.PlatformAuditPublisher;
import com.vimainsurance.vimaadmin.util.PolicyValidationUtil;
import com.vimainsurance.vimaadmin.util.TopupPremiumOptionsUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Service implementation for Policy operations
 */
@Service
public class PolicyServiceImpl implements IPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyServiceImpl.class);

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private ICdAccountRepository cdAccountRepository;

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
    private PlatformAuditPublisher platformAuditPublisher;
    
    @Autowired
    private IDocumentService documentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IProductCatalogService productCatalogService;

    @Autowired
    private IProductCatalogRepository productCatalogRepository;

    @Autowired
    private IEndorsementRepository endorsementRepository;

    @Autowired
    private ICdBalanceTransactionRepository cdBalanceTransactionRepository;

    @Autowired
    private com.vimainsurance.vimaadmin.service.IS3Service s3Service;

    @Autowired
    private com.vimainsurance.vimaadmin.config.S3Config s3Config;

    /**
     * Maximum size for a wording / claim-checklist PDF upload — 50 MB.
     * Wording PDFs typically run 60–70 pages with embedded images, which can
     * exceed 25 MB; keep this aligned with {@code spring.servlet.multipart.max-file-size}
     * (currently 50 MB on prod). Anything larger should be rejected at the
     * Spring layer first; this constant is a defensive secondary guard.
     */
    private static final long POLICY_DOCUMENT_MAX_BYTES = 50L * 1024L * 1024L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "policies", entityType = "POLICY", action = "CREATE")
    public ResponseEntity<ResponseDto<String>> createPolicy(PolicyRequestDto requestDto) {
        logger.info("[correlationId:{}] createPolicy called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Validate policy request based on policy type
            PolicyValidationUtil.validatePolicyRequest(requestDto);

            // Check if policy number already exists
            if (policyRepository.existsByPolicyNumber(requestDto.getPolicyNumber())) {
                return responseObj.render(responseObj.formErrorResponse(DUPLICATE_POLICY_NUMBER_MESSAGE));
            }
            Optional<AdminUser> adminUser = jwtUserExtractor.resolveCurrentAdminUser();
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            AdminUser agent = adminUser.get();
            Deals primaryIndividual = dealsRepository.findById(requestDto.getPrimaryIndividualId()).orElseThrow(() -> new RuntimeException("Primary individual not found"));

            // Get policy type
            ProductType policyType = ProductType.fromValue(requestDto.getProductType());

            // PARENT_GMC / TOP_UP / SUPER_TOP_UP: require active base GMC for the organization
            if (requestDto.getOrganizationId() != null
                && (policyType == ProductType.PARENT_GMC || policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP)) {
                if (!organizationHasActiveBaseGmc(requestDto.getOrganizationId())) {
                    return responseObj.render(responseObj.formErrorResponse(BASE_GMC_REQUIRED_MESSAGE));
                }
            }

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
            policy.setMaxChildrenAllowed(resolveAndValidateMaxChildrenAllowed(
                    policyType, policy.getCoverageType(), requestDto.getMaxChildrenAllowed()));

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
            // policy_wording / claim_checklist are now uploaded via the dedicated PDF
            // endpoints below — see PolicyController#uploadPolicyWordingDocument etc.
            // TOP_UP / SUPER_TOP_UP do not require policy-level total premium inputs from form.
            // Keep DB NOT NULL monetary columns populated with safe defaults.
            if ((policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) && policy.getPremiumAmount() == null) {
                policy.setPremiumAmount(BigDecimal.ZERO);
            }
            if ((policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) && policy.getNetAmount() == null) {
                policy.setNetAmount(BigDecimal.ZERO);
            }
            if ((policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) && policy.getGst() == null) {
                policy.setGst(BigDecimal.ZERO);
            }
            if (requestDto.getPaymentFrequency() != null && !requestDto.getPaymentFrequency().isEmpty()) {
                policy.setPaymentFrequency(PaymentFrequency.fromValue(requestDto.getPaymentFrequency()));
            } else {
                policy.setPaymentFrequency(PaymentFrequency.YEARLY);
            }
            policy.setCoveredIndividuals(coveredIndividualIds);

            // Set TPA details (GMC and legacy GHI — same group medical form in portal)
            if (policyType == ProductType.GMC || policyType == ProductType.GHI) {
                policy.setTpaOrganizationName(requestDto.getTpaOrganizationName());
                policy.setTpaContactInfo(requestDto.getTpaContactInfo());
            }

            if (policyType == ProductType.PARENT_GMC) {
                policy.setParentCoverageEnabled(requestDto.getParentCoverageEnabled());
                policy.setInLawCoverageEnabled(requestDto.getInLawCoverageEnabled());
                policy.setMaxParents(requestDto.getMaxParents());
                policy.setMaxInLaws(requestDto.getMaxInLaws());
                policy.setParentAgeLimit(requestDto.getParentAgeLimit());
            }
            if (policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) {
                validateTopupTieredOptionsRequired(requestDto.getSumInsuredOptions(), requestDto.getTopupPremiumOptions());
                policy.setDescription(requestDto.getDescription());
                policy.setInsurerName(requestDto.getInsurerName());
                policy.setDeductibleAmount(requestDto.getDeductibleAmount());
                policy.setSumInsuredOptions(normalizeSumInsuredOptionsForSave(requestDto.getSumInsuredOptions()));
                policy.setTopupPremiumOptions(
                        normalizeTopupPremiumOptionsForSave(requestDto.getSumInsuredOptions(), requestDto.getTopupPremiumOptions()));
                policy.setCoversDependents(requestDto.getCoversDependents());
                policy.setCoversParents(requestDto.getCoversParents());
                policy.setIsDeleted(requestDto.getIsDeleted());
                policy.setEffectiveFrom(requestDto.getEffectiveFrom());
                policy.setEffectiveTo(requestDto.getEffectiveTo());
            }

            policy.setPolicyWordingSummary(requestDto.getPolicyWordingSummary());
            policy.setClaimChecklistAdditionalDocs(requestDto.getClaimChecklistAdditionalDocs());

            Policy savedPolicy = policyRepository.save(policy);
            logger.info("[correlationId:{}] Policy created successfully with ID: {}",
                       MDC.get("correlationId"), savedPolicy.getPolicyId());

            // TOP_UP / SUPER_TOP_UP: create product_catalog row so the product appears in the catalog
            if (policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) {
                createProductCatalogForTopup(savedPolicy, requestDto.getPricingModel());
            }
            // PARENT_GMC: create product_catalog row so it appears in enrollment plans
            if (policyType == ProductType.PARENT_GMC) {
                createProductCatalogForParentGmc(savedPolicy);
            }
            // NOTE: Default cost-sharing rules are no longer auto-seeded here.
            // The seed previously created 8 rows per policy keyed by LocalDate.now(),
            // which could accumulate duplicates and confused HR ("which row do I edit?").
            // CostSharingRuleServiceImpl.getEffectiveRule + applyCostSharing default to
            // 100% employer / 0% employee when no rule exists, so behavior is preserved.
            // HR explicitly adds rules from the Cost-Sharing Config page when they want a split.

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        } catch (BadRequestException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Validation error in createPolicy: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Data integrity violation in createPolicy: {}", MDC.get("correlationId"), e.getMessage(), e);
            if (isDuplicatePolicyNumberViolation(e)) {
                return responseObj.render(responseObj.formErrorResponse(DUPLICATE_POLICY_NUMBER_MESSAGE));
            }
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Exception in createPolicy: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "policies", entityType = "POLICY", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> updatePolicy(Long policyId, PolicyRequestDto requestDto) {
        logger.info("[correlationId:{}] updatePolicy called for ID: {}", MDC.get("correlationId"), policyId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Validate policy request based on policy type
            Optional<Policy> policyOpt = policyRepository.findById(policyId);
            if (policyOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            Policy policy = policyOpt.get();
            PolicyValidationUtil.validatePolicyRequest(requestDto, true);
            try {
                AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(policy));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Could not serialize policy for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
            }
            // Check if policy number is being changed and if it already exists
            if (!policy.getPolicyNumber().equals(requestDto.getPolicyNumber()) && 
                policyRepository.existsByPolicyNumber(requestDto.getPolicyNumber())) {
                return responseObj.render(responseObj.formErrorResponse(DUPLICATE_POLICY_NUMBER_MESSAGE));
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

            policy.setProductType(policyType);
            policy.setAppliesToEmployees(requestDto.getAppliesToEmployees() != null ?
                requestDto.getAppliesToEmployees() : true);

            if (policyType == ProductType.PARENT_GMC) {
                policy.setCoverageType(CoverageType.PARENT);
            } else if (requestDto.getCoverageType() != null && !requestDto.getCoverageType().isEmpty()) {
                policy.setCoverageType(CoverageType.fromValue(requestDto.getCoverageType()));
            } else {
                policy.setCoverageType(null);
            }
            policy.setMaxChildrenAllowed(resolveAndValidateMaxChildrenAllowed(
                    policyType, policy.getCoverageType(), requestDto.getMaxChildrenAllowed()));

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
            // policy_wording / claim_checklist documents are managed through the
            // dedicated PDF endpoints; the JSON PUT no longer touches them.
            if ((policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) && policy.getPremiumAmount() == null) {
                policy.setPremiumAmount(BigDecimal.ZERO);
            }
            if ((policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) && policy.getNetAmount() == null) {
                policy.setNetAmount(BigDecimal.ZERO);
            }
            if ((policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) && policy.getGst() == null) {
                policy.setGst(BigDecimal.ZERO);
            }
            if (requestDto.getPaymentFrequency() != null && !requestDto.getPaymentFrequency().isEmpty()) {
                policy.setPaymentFrequency(PaymentFrequency.fromValue(requestDto.getPaymentFrequency()));
            }

            // Update TPA details (GMC and legacy GHI); blank fields = leave existing values unchanged
            if (policyType == ProductType.GMC || policyType == ProductType.GHI) {
                boolean nameBlank = requestDto.getTpaOrganizationName() == null
                        || requestDto.getTpaOrganizationName().trim().isEmpty();
                boolean contactBlank = requestDto.getTpaContactInfo() == null
                        || requestDto.getTpaContactInfo().trim().isEmpty();
                if (!nameBlank) {
                    policy.setTpaOrganizationName(requestDto.getTpaOrganizationName());
                }
                if (!contactBlank) {
                    policy.setTpaContactInfo(requestDto.getTpaContactInfo());
                }
            } else {
                policy.setTpaOrganizationName(null);
                policy.setTpaContactInfo(null);
            }

            // PARENT_GMC
            if (policyType == ProductType.PARENT_GMC) {
                policy.setParentCoverageEnabled(requestDto.getParentCoverageEnabled());
                policy.setInLawCoverageEnabled(requestDto.getInLawCoverageEnabled());
                policy.setMaxParents(requestDto.getMaxParents());
                policy.setMaxInLaws(requestDto.getMaxInLaws());
                policy.setParentAgeLimit(requestDto.getParentAgeLimit());
            }

            // TOP_UP / SUPER_TOP_UP
            if (policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) {
                validateTopupTieredOptionsRequired(requestDto.getSumInsuredOptions(), requestDto.getTopupPremiumOptions());
                policy.setDescription(requestDto.getDescription());
                policy.setInsurerName(requestDto.getInsurerName());
                policy.setDeductibleAmount(requestDto.getDeductibleAmount());
                policy.setSumInsuredOptions(normalizeSumInsuredOptionsForSave(requestDto.getSumInsuredOptions()));
                policy.setTopupPremiumOptions(
                        normalizeTopupPremiumOptionsForSave(requestDto.getSumInsuredOptions(), requestDto.getTopupPremiumOptions()));
                policy.setCoversDependents(requestDto.getCoversDependents());
                policy.setCoversParents(requestDto.getCoversParents());
                policy.setIsDeleted(requestDto.getIsDeleted());
                policy.setEffectiveFrom(requestDto.getEffectiveFrom());
                policy.setEffectiveTo(requestDto.getEffectiveTo());
            } else {
                policy.setTopupPremiumOptions(null);
            }

            policy.setPolicyWordingSummary(requestDto.getPolicyWordingSummary());
            policy.setClaimChecklistAdditionalDocs(requestDto.getClaimChecklistAdditionalDocs());

            policyRepository.save(policy);
            if (policyType == ProductType.TOP_UP || policyType == ProductType.SUPER_TOP_UP) {
                createProductCatalogForTopup(policy, requestDto.getPricingModel());
            }
            com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setNewSnapshotEntity(policy);
            logger.info("[correlationId:{}] Policy updated successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        } catch (BadRequestException e) {
            logger.error("[correlationId:{}] Validation error in updatePolicy: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            logger.error("[correlationId:{}] Data integrity violation in updatePolicy: {}", MDC.get("correlationId"), e.getMessage(), e);
            if (isDuplicatePolicyNumberViolation(e)) {
                return responseObj.render(responseObj.formErrorResponse(DUPLICATE_POLICY_NUMBER_MESSAGE));
            }
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
            List<Policy> policies = policyRepository.findByOrganizationId(organizationId);
            Long employeesCount = dealsRepository.countByOrganizationIdAndRelationshipSelf(organizationId);
            Long dependentsCount = dealsRepository.countByOrganizationIdAndRelationshipNonSelf(organizationId);
            Map<Long, PolicyPremiumSummary> policyPremiumSummaryMap = getPolicyPremiumSummary(organizationId, policies);
            List<PolicyResponseDto> responseDtos = policies.stream()
                .map(policy -> {
                    PolicyResponseDto responseDto = mapToResponseDto(
                        policy,
                        policyPremiumSummaryMap.get(policy.getPolicyId())
                    );
                    responseDto.setEmployeesCount(employeesCount);
                    responseDto.setDependentsCount(dependentsCount);
                    return responseDto;
                })
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
    @AuditedOperation(schemaName = "cpc", tableName = "policies", entityType = "POLICY", action = "UPDATE")
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
            com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setNewSnapshotEntity(policy);
            logger.info("[correlationId:{}] Policy status updated successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Policy status updated successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in updatePolicyStatus: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "policies", entityType = "POLICY", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> deletePolicy(Long policyId) {
        logger.info("[correlationId:{}] deletePolicy called for ID: {}", MDC.get("correlationId"), policyId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            Optional<Policy> policyOpt = policyRepository.findById(policyId);
            if (policyOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            
            Policy policy = policyOpt.get();
            if (policy.getOrganizationId() != null) {
                long organizationPolicyCount = policyRepository.countByOrganizationId(policy.getOrganizationId());
                if (organizationPolicyCount <= 1) {
                    return responseObj.render(responseObj.formErrorResponse(
                            "Cannot delete policy. At least one policy must remain for the company."));
                }
            }
            // Capture the wording / checklist document refs before delete; the
            // FK has ON DELETE SET NULL on the policies table, but the Document
            // rows + S3 objects belong to the policy and would otherwise leak.
            Document wordingDoc = policy.getPolicyWordingDocument();
            Document checklistDoc = policy.getClaimChecklistDocument();
            policyRepository.delete(policy);
            deleteDocumentSafely(wordingDoc);
            deleteDocumentSafely(checklistDoc);
            
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

    // ---------------------------------------------------------------
    // Policy wording / claim checklist PDF management
    // ---------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<PolicyDocumentRefDto>> attachPolicyWordingDocument(UUID organizationId, Long policyId, MultipartFile file) {
        return attachPolicyDocumentEndpoint(organizationId, policyId, file, DocumentType.POLICY_WORDING);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<PolicyDocumentRefDto>> attachClaimChecklistDocument(UUID organizationId, Long policyId, MultipartFile file) {
        return attachPolicyDocumentEndpoint(organizationId, policyId, file, DocumentType.CLAIM_CHECKLIST);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<String>> removePolicyWordingDocument(UUID organizationId, Long policyId) {
        return removePolicyDocumentEndpoint(organizationId, policyId, DocumentType.POLICY_WORDING);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<String>> removeClaimChecklistDocument(UUID organizationId, Long policyId) {
        return removePolicyDocumentEndpoint(organizationId, policyId, DocumentType.CLAIM_CHECKLIST);
    }

    private ResponseEntity<ResponseDto<PolicyDocumentRefDto>> attachPolicyDocumentEndpoint(
            UUID organizationId, Long policyId, MultipartFile file, DocumentType slot) {
        BaseResponse<PolicyDocumentRefDto> responseObj = new BaseResponse<>();
        try {
            if (file == null || file.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("File is required"));
            }
            Policy policy = policyRepository.findById(policyId)
                    .orElse(null);
            if (policy == null) {
                return responseObj.render(responseObj.formErrorResponse("Policy not found"));
            }
            verifyPolicyBelongsToOrganization(policy, organizationId);
            Document doc = attachPolicyDocument(policy, file, slot);
            String action = slot == DocumentType.POLICY_WORDING ? "ATTACH_WORDING" : "ATTACH_CLAIM_CHECKLIST";
            platformAuditPublisher.publishAuthenticated(
                    "cpc",
                    "policies",
                    "POLICY_DOCUMENT",
                    action,
                    policyId.toString(),
                    organizationId,
                    java.util.Map.of(
                            "documentId", doc.getDocumentId().toString(),
                            "filename", doc.getOriginalFilename() != null ? doc.getOriginalFilename() : ""));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, toPolicyDocumentRefDto(doc)));
        } catch (BadRequestException e) {
            logger.warn("[correlationId:{}] Validation failed attaching {} to policy {}: {}",
                    MDC.get("correlationId"), slot, policyId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error attaching {} to policy {}",
                    MDC.get("correlationId"), slot, policyId, e);
            return responseObj.render(responseObj.formErrorResponse("Failed to upload document: " + e.getMessage()));
        }
    }

    private ResponseEntity<ResponseDto<String>> removePolicyDocumentEndpoint(UUID organizationId, Long policyId, DocumentType slot) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Policy policy = policyRepository.findById(policyId).orElse(null);
            if (policy == null) {
                return responseObj.render(responseObj.formErrorResponse("Policy not found"));
            }
            verifyPolicyBelongsToOrganization(policy, organizationId);
            Document existing = (slot == DocumentType.POLICY_WORDING)
                    ? policy.getPolicyWordingDocument()
                    : policy.getClaimChecklistDocument();
            if (existing == null) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "No document attached"));
            }
            String removeAction = slot == DocumentType.POLICY_WORDING ? "REMOVE_WORDING" : "REMOVE_CLAIM_CHECKLIST";
            platformAuditPublisher.publishAuthenticated(
                    "cpc",
                    "policies",
                    "POLICY_DOCUMENT",
                    removeAction,
                    policyId.toString(),
                    organizationId,
                    java.util.Map.of(
                            "removedDocumentId", existing.getDocumentId().toString(),
                            "filename", existing.getOriginalFilename() != null ? existing.getOriginalFilename() : ""));
            if (slot == DocumentType.POLICY_WORDING) {
                policy.setPolicyWordingDocument(null);
            } else {
                policy.setClaimChecklistDocument(null);
            }
            policyRepository.save(policy);
            deleteDocumentSafely(existing);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document removed"));
        } catch (BadRequestException e) {
            logger.warn("[correlationId:{}] Access denied removing {} from policy {}: {}",
                    MDC.get("correlationId"), slot, policyId, e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error removing {} from policy {}",
                    MDC.get("correlationId"), slot, policyId, e);
            return responseObj.render(responseObj.formErrorResponse("Failed to remove document: " + e.getMessage()));
        }
    }

    /**
     * Reject the request if the resolved tenant organization does not own the policy.
     * Refuses both null org id and mismatched org id; the controller layer
     * ({@code @CurrentOrganization}) is the primary guard, this is the
     * secondary check at the service boundary.
     */
    private void verifyPolicyBelongsToOrganization(Policy policy, UUID organizationId) {
        if (organizationId == null) {
            throw new BadRequestException("Organization ID is required");
        }
        if (policy.getOrganizationId() == null || !organizationId.equals(policy.getOrganizationId())) {
            throw new BadRequestException("Access denied: policy does not belong to your organization");
        }
    }

    /**
     * Validates the file is a PDF, uploads it to S3, creates a {@link Document}
     * row, swaps the FK on the policy, and deletes any previously attached
     * document for that slot. Returns the newly persisted {@link Document}.
     *
     * <p>Registers a transaction-synchronization callback that deletes the
     * freshly uploaded S3 object if the surrounding transaction rolls back,
     * so we don't leak orphaned bytes on DB failures after the upload.
     */
    private Document attachPolicyDocument(Policy policy, MultipartFile file, DocumentType slot) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > POLICY_DOCUMENT_MAX_BYTES) {
            throw new BadRequestException(
                    "File too large. Maximum allowed size is " + (POLICY_DOCUMENT_MAX_BYTES / (1024 * 1024)) + " MB");
        }
        String mime = file.getContentType();
        if (mime == null || !"application/pdf".equalsIgnoreCase(mime.trim())) {
            throw new BadRequestException("Only PDF files are allowed");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        if (!original.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are allowed");
        }

        String sanitized = sanitizeFilename(original);
        String slotFolder = (slot == DocumentType.POLICY_WORDING) ? "wording" : "claim-checklist";
        String s3Key = String.format("policies/%d/%s/%s_%s",
                policy.getPolicyId(), slotFolder, UUID.randomUUID(), sanitized);

        s3Service.uploadFile(file, s3Key);
        registerS3RollbackCleanup(s3Key);

        Document document = new Document();
        document.setEntityType(DocumentEntityType.POLICY);
        document.setEntityId(String.valueOf(policy.getPolicyId()));
        document.setDocumentType(slot);
        document.setDocumentCategory(DocumentCategory.POLICY_DOCUMENTS);
        document.setS3Bucket(s3Config.getBucketName());
        document.setS3Key(s3Key);
        document.setOriginalFilename(original);
        document.setFileSize(file.getSize());
        document.setMimeType(mime);
        document.setUploadedAt(LocalDateTime.now());
        try {
            UUID uploadedBy = jwtUserExtractor.getCurrentUserId();
            UserRole uploadedByRole = jwtUserExtractor.getCurrentUserRole();
            document.setUploadedBy(uploadedBy);
            document.setUploadedByRole(uploadedByRole);
        } catch (Exception ignored) {
            // Anonymous / system uploads are acceptable for these slots.
        }
        Document saved = documentRepository.save(document);

        Document previous;
        if (slot == DocumentType.POLICY_WORDING) {
            previous = policy.getPolicyWordingDocument();
            policy.setPolicyWordingDocument(saved);
        } else {
            previous = policy.getClaimChecklistDocument();
            policy.setClaimChecklistDocument(saved);
        }
        policyRepository.save(policy);

        if (previous != null) {
            deleteDocumentSafely(previous);
        }
        return saved;
    }

    /**
     * Schedule an S3 object delete if (and only if) the current transaction rolls back.
     * Uses Spring's transaction synchronization so commits leave the object in
     * place. No-op when called outside a transaction (defensive).
     */
    private void registerS3RollbackCleanup(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    s3Service.deleteFile(s3Key);
                } catch (Exception ex) {
                    logger.warn("[correlationId:{}] Failed to delete orphaned S3 object {} after rollback: {}",
                            MDC.get("correlationId"), s3Key, ex.getMessage());
                }
            }
        });
    }

    private void deleteDocumentSafely(Document doc) {
        if (doc == null) {
            return;
        }
        try {
            if (doc.getS3Key() != null && !doc.getS3Key().isBlank()) {
                s3Service.deleteFile(doc.getS3Key());
            }
        } catch (Exception e) {
            logger.warn("[correlationId:{}] Failed to delete S3 object {} for replaced document {}: {}",
                    MDC.get("correlationId"), doc.getS3Key(), doc.getDocumentId(), e.getMessage());
        }
        try {
            documentRepository.delete(doc);
        } catch (Exception e) {
            logger.warn("[correlationId:{}] Failed to delete document row {} after replacement: {}",
                    MDC.get("correlationId"), doc.getDocumentId(), e.getMessage());
        }
    }

    private static String sanitizeFilename(String name) {
        if (name == null) return "file.pdf";
        String base = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.length() > 120) {
            base = base.substring(base.length() - 120);
        }
        return base;
    }

    /**
     * Map a Document entity to a PolicyDocumentRefDto for non-employee responses
     * (no presigned URL — admin UIs render filename and have separate download URL).
     */
    private PolicyDocumentRefDto toPolicyDocumentRefDto(Document doc) {
        if (doc == null) {
            return null;
        }
        return PolicyDocumentRefDto.builder()
                .documentId(doc.getDocumentId())
                .filename(doc.getOriginalFilename())
                .fileSizeBytes(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }

    /**
     * Map Policy entity to PolicyResponseDto
     */
    private PolicyResponseDto mapToResponseDto(Policy policy) {
        return mapToResponseDto(policy, null);
    }

    private PolicyResponseDto mapToResponseDto(Policy policy, PolicyPremiumSummary premiumSummary) {
        PolicyResponseDto responseDto = new PolicyResponseDto();
        responseDto.setPolicyId(policy.getPolicyId());
        responseDto.setPolicyNumber(policy.getPolicyNumber());
        responseDto.setPrimaryIndividualId(policy.getPrimaryIndividualId());
        var insuranceProviderEntity = insuranceProviderRepository.findById(policy.getInsuranceProviderId())
                .orElseThrow(() -> new RuntimeException("Insurance provider not found"));
        responseDto.setInsuranceProvider(insuranceProviderEntity.getProviderName());
        responseDto.setInsuranceProviderCode(insuranceProviderEntity.getProviderCode());
        responseDto.setNetworkHospitalsUrl(insuranceProviderEntity.getNetworkHospitalsUrl());
        responseDto.setBlacklistedHospitalsUrl(insuranceProviderEntity.getBlacklistedHospitalsUrl());
        responseDto.setInsuranceProductId(policy.getInsuranceProductId());
        responseDto.setOrganizationId(policy.getOrganizationId());
        responseDto.setDocumentId(policy.getDocument() != null ? policy.getDocument().getDocumentId() : null);

        // Map policy type (using productType field) and category
        responseDto.setProductType(policy.getProductType() != null ? policy.getProductType().getValue() : null);
        responseDto.setAppliesToEmployees(policy.getAppliesToEmployees());

        responseDto.setProductType(policy.getProductType().getValue());

        // Map coverage type (E, ES, ESC, ESCP for GMC or INDIVIDUAL, FAMILY_FLOATER, GROUP for traditional)
        responseDto.setCoverageType(policy.getCoverageType() != null ? policy.getCoverageType().getValue() : null);
        responseDto.setMaxChildrenAllowed(policy.getMaxChildrenAllowed());

        responseDto.setStatus(policy.getStatus().getValue());
        responseDto.setSumInsured(policy.getSumInsured());
        responseDto.setSumInsuredMultiplier(policy.getSumInsuredMultiplier());
        responseDto.setPremiumAmount(policy.getPremiumAmount());
        BigDecimal inceptionPremium = policy.getPremiumAmount() != null ? policy.getPremiumAmount() : BigDecimal.ZERO;
        BigDecimal endorsementPremium = premiumSummary != null && premiumSummary.endorsementPremium() != null
            ? premiumSummary.endorsementPremium()
            : BigDecimal.ZERO;
        BigDecimal runningPremium = inceptionPremium.add(endorsementPremium);
        responseDto.setInceptionPremium(inceptionPremium);
        responseDto.setEndorsementPremium(endorsementPremium);
        responseDto.setRunningPremium(runningPremium);
        responseDto.setEndorsementCount(premiumSummary != null ? premiumSummary.endorsementCount() : 0);
        responseDto.setPendingEndorsementCount(premiumSummary != null ? premiumSummary.pendingEndorsementCount() : 0);
        if (inceptionPremium.compareTo(BigDecimal.ZERO) > 0) {
            responseDto.setPremiumDeltaPercent(
                endorsementPremium
                    .multiply(BigDecimal.valueOf(100))
                    .divide(inceptionPremium, 2, java.math.RoundingMode.HALF_UP)
            );
        } else {
            responseDto.setPremiumDeltaPercent(BigDecimal.ZERO);
        }
        LocalDateTime asOfDateTime = policy.getUpdatedAt();
        if (premiumSummary != null && premiumSummary.lastEndorsementUpdatedAt() != null) {
            if (asOfDateTime == null || premiumSummary.lastEndorsementUpdatedAt().isAfter(asOfDateTime)) {
                asOfDateTime = premiumSummary.lastEndorsementUpdatedAt();
            }
        }
        if (asOfDateTime != null) {
            responseDto.setPremiumAsOfDate(asOfDateTime.toLocalDate());
        }
        responseDto.setStartDate(policy.getStartDate());
        responseDto.setEndDate(policy.getEndDate());
        responseDto.setRenewalDate(policy.getRenewalDate());
        responseDto.setLeadId(policy.getLeadId());
        responseDto.setPaymentFrequency(policy.getPaymentFrequency() != null ? policy.getPaymentFrequency().getValue() : null);
        responseDto.setCreatedAt(policy.getCreatedAt());
        responseDto.setUpdatedAt(policy.getUpdatedAt());
        responseDto.setNetAmount(policy.getNetAmount());
        responseDto.setGst(policy.getGst());
        responseDto.setCdAccountId(policy.getCdAccountId());
        BigDecimal hydratedCdBalance = policy.getCdBalance();
        if (policy.getCdAccountId() != null) {
            CdAccount cdAccount = cdAccountRepository.findById(policy.getCdAccountId()).orElse(null);
            if (cdAccount != null && cdAccount.getCdBalance() != null) {
                hydratedCdBalance = cdAccount.getCdBalance();
            }
        }
        responseDto.setCdBalance(hydratedCdBalance);

        // Map TPA details (for GMC only)
        responseDto.setTpaOrganizationName(policy.getTpaOrganizationName());
        responseDto.setTpaContactInfo(policy.getTpaContactInfo());

        // PARENT_GMC
        responseDto.setParentCoverageEnabled(policy.getParentCoverageEnabled());
        responseDto.setInLawCoverageEnabled(policy.getInLawCoverageEnabled());
        responseDto.setMaxParents(policy.getMaxParents());
        responseDto.setMaxInLaws(policy.getMaxInLaws());
        responseDto.setParentAgeLimit(policy.getParentAgeLimit());

        // TOP_UP / SUPER_TOP_UP
        responseDto.setDescription(policy.getDescription());
        responseDto.setInsurerName(policy.getInsurerName());
        responseDto.setDeductibleAmount(policy.getDeductibleAmount());
        responseDto.setSumInsuredOptions(policy.getSumInsuredOptions());
        responseDto.setTopupPremiumOptions(policy.getTopupPremiumOptions());
        responseDto.setPolicyWordingDocument(toPolicyDocumentRefDto(policy.getPolicyWordingDocument()));
        responseDto.setClaimChecklistDocument(toPolicyDocumentRefDto(policy.getClaimChecklistDocument()));
        responseDto.setPolicyWordingSummary(policy.getPolicyWordingSummary());
        responseDto.setClaimChecklistAdditionalDocs(policy.getClaimChecklistAdditionalDocs());
        responseDto.setCoversDependents(policy.getCoversDependents());
        responseDto.setCoversParents(policy.getCoversParents());
        responseDto.setIsDeleted(policy.getIsDeleted());
        responseDto.setEffectiveFrom(policy.getEffectiveFrom());
        responseDto.setEffectiveTo(policy.getEffectiveTo());
        // NOTE: `coveredIndividuals` is intentionally NOT exposed on this response.
        // Its semantics are inconsistent across creation paths:
        //   - createPolicy()              → list of real individual UUIDs
        //   - uploadPolicyForOrganization → Arrays.asList(organizationId)  (legacy)
        // Until that data model is normalized, exposing the field would let
        // clients build matching logic on top of a misleading signal. Clients
        // that need the per-employee → policy mapping should use the dedicated
        // employee-policy mapping endpoints instead.

        List<Deals> dependents = (policy.getCoveredIndividuals() != null && !policy.getCoveredIndividuals().isEmpty())
            ? dealsRepository.findByIndividualIdIn(policy.getCoveredIndividuals())
            : new ArrayList<>();
        responseDto.setDependents(dependents.stream()
            .map(this::mapToSimplifiedDependent)
            .collect(Collectors.toList()));
        List<Nominee> nominees = nomineeRepository.findByPolicyPolicyId(policy.getPolicyId());
        
        return responseDto;
    }

    private Map<Long, PolicyPremiumSummary> getPolicyPremiumSummary(UUID organizationId, List<Policy> policies) {
        if (policies == null || policies.isEmpty()) {
            return Map.of();
        }
        List<Long> policyIds = policies.stream()
            .map(Policy::getPolicyId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (policyIds.isEmpty()) {
            return Map.of();
        }
        List<Object[]> endorsementRows = endorsementRepository.getPolicyPremiumSummaryByOrganizationAndPolicyIds(
            organizationId,
            policyIds
        );
        List<Object[]> ledgerRows = cdBalanceTransactionRepository.getEndorsementTransactionSummaryByOrganizationAndPolicyIds(
            organizationId,
            policyIds
        );
        Map<Long, PolicyPremiumSummary> result = new HashMap<>();
        for (Object[] row : endorsementRows) {
            if (row == null || row.length < 5 || row[0] == null) {
                continue;
            }
            Long policyId = ((Number) row[0]).longValue();
            int endorsementCount = row[2] != null ? ((Number) row[2]).intValue() : 0;
            int pendingEndorsementCount = row[3] != null ? ((Number) row[3]).intValue() : 0;
            LocalDateTime lastUpdatedAt = toLocalDateTime(row[4]);
            result.put(
                policyId,
                new PolicyPremiumSummary(
                    BigDecimal.ZERO,
                    endorsementCount,
                    pendingEndorsementCount,
                    lastUpdatedAt
                )
            );
        }

        for (Object[] row : ledgerRows) {
            if (row == null || row.length < 4 || row[0] == null) {
                continue;
            }
            Long policyId = ((Number) row[0]).longValue();
            BigDecimal totalEndorsementCredit = toBigDecimal(row[1]);
            BigDecimal totalEndorsementDebit = toBigDecimal(row[2]);
            BigDecimal endorsementPremium = totalEndorsementDebit.subtract(totalEndorsementCredit);
            LocalDateTime ledgerUpdatedAt = toLocalDateTime(row[3]);

            PolicyPremiumSummary existing = result.get(policyId);
            if (existing == null) {
                result.put(
                    policyId,
                    new PolicyPremiumSummary(endorsementPremium, 0, 0, ledgerUpdatedAt)
                );
                continue;
            }
            LocalDateTime mergedUpdatedAt = existing.lastEndorsementUpdatedAt();
            if (ledgerUpdatedAt != null && (mergedUpdatedAt == null || ledgerUpdatedAt.isAfter(mergedUpdatedAt))) {
                mergedUpdatedAt = ledgerUpdatedAt;
            }
            result.put(
                policyId,
                new PolicyPremiumSummary(
                    endorsementPremium,
                    existing.endorsementCount(),
                    existing.pendingEndorsementCount(),
                    mergedUpdatedAt
                )
            );
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }

    private record PolicyPremiumSummary(
        BigDecimal endorsementPremium,
        int endorsementCount,
        int pendingEndorsementCount,
        LocalDateTime lastEndorsementUpdatedAt
    ) {}
    
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
        responseDto.setActualRelationship(deals.getActualRelationship());
        
        return responseDto;
    }

    /** Error message when dependent policy types (PARENT_GMC, TOP_UP, SUPER_TOP_UP) are added without a base GMC. */
    private static final String BASE_GMC_REQUIRED_MESSAGE =
        "Base Group Medical Coverage (GMC) policy must be created before adding Parent Coverage or Top-Up plans.";

    /**
     * Returns true if the organization has at least one GMC (or GHI) policy (any status), so dependent types (PARENT_GMC, TOP_UP, SUPER_TOP_UP) can be added.
     */
    private boolean organizationHasActiveBaseGmc(UUID organizationId) {
        List<Policy> orgPolicies = policyRepository.findByOrganizationId(organizationId);
        return orgPolicies.stream().anyMatch(p ->
            p.getProductType() == ProductType.GMC || p.getProductType() == ProductType.GHI);
    }

    /**
     * Build and create a product_catalog row for TOP_UP or SUPER_TOP_UP policy so the product appears in the catalog.
     * Mapping: Policy form / Policy table → Product Catalog API (POST /api/v1/product-catalog)
     * - Policy Type (form) / product_type (policy) → productType
     * - companyId (form) / organization_id (policy) → organizationId
     * - topupName or description (form) / description (policy) → name
     * - topupPricingModel (form) / request pricingModel → pricingModel
     * - sum_insured_options (policy, JSON) → coverageOptions
     * - covers_dependents + covers_parents (policy) → coveredRelationships (JSON array)
     * - effective_from, effective_to (policy) → effectiveFrom, effectiveTo
     * - saved policy_id → policyId
     *
     * @param savedPolicy the saved TOP_UP or SUPER_TOP_UP policy
     * @param pricingModelFromRequest optional pricing model from form (FLAT, AGE_BANDED, FAMILY_FLOATER); defaults to FLAT
     */
    private void createProductCatalogForTopup(Policy savedPolicy, String pricingModelFromRequest) {
        if (savedPolicy.getOrganizationId() == null) {
            return;
        }
        ProductType productType = savedPolicy.getProductType();
        if (productType != ProductType.TOP_UP && productType != ProductType.SUPER_TOP_UP) {
            return;
        }
        LocalDate effectiveFrom = savedPolicy.getEffectiveFrom() != null
            ? savedPolicy.getEffectiveFrom()
            : savedPolicy.getStartDate();
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("effectiveFrom or startDate is required for TOP_UP/SUPER_TOP_UP product catalog");
        }
        String name = (savedPolicy.getDescription() != null && !savedPolicy.getDescription().isBlank())
            ? savedPolicy.getDescription()
            : (productType.getValue() + " Plan");
        if (name.length() > 255) {
            name = name.substring(0, 255);
        }
        List<String> relationships = new ArrayList<>();
        if (Boolean.TRUE.equals(savedPolicy.getCoversDependents())) {
            relationships.add("SELF");
            relationships.add("SPOUSE");
            relationships.add("CHILD");
        }
        if (Boolean.TRUE.equals(savedPolicy.getCoversParents())) {
            relationships.add("FATHER");
            relationships.add("MOTHER");
        }
        if (relationships.isEmpty()) {
            relationships.add("SELF");
        }
        String coveredRelationshipsJson;
        try {
            coveredRelationshipsJson = objectMapper.writeValueAsString(relationships);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            coveredRelationshipsJson = "[\"SELF\"]";
        }
        String pricingModel = (pricingModelFromRequest != null && !pricingModelFromRequest.isBlank())
            ? pricingModelFromRequest
            : "FLAT";
        List<ProductCatalog> existing = productCatalogRepository.findByOrganizationIdOrderByDisplayOrderAsc(savedPolicy.getOrganizationId());
        int displayOrder = existing.isEmpty() ? 0 : (existing.get(existing.size() - 1).getDisplayOrder() == null ? 0 : existing.get(existing.size() - 1).getDisplayOrder()) + 1;

        ProductCatalogRequestDto catalogDto = ProductCatalogRequestDto.builder()
            .organizationId(savedPolicy.getOrganizationId())
            .productType(productType.getValue())
            .name(name)
            .isMandatory(false)
            .pricingModel(pricingModel)
            .coverageOptions(savedPolicy.getSumInsuredOptions())
            .premiumPreviewOptions(buildPremiumPreviewOptionsForCatalog(savedPolicy))
            .coveredRelationships(coveredRelationshipsJson)
            .displayOrder(displayOrder)
            .policyId(savedPolicy.getPolicyId())
            .gradeFilter(null)
            .isActive(true)
            .effectiveFrom(effectiveFrom)
            .effectiveTo(savedPolicy.getEffectiveTo())
            .build();

        Optional<ProductCatalog> existingPc = productCatalogRepository.findByOrganizationIdAndPolicyId(
                savedPolicy.getOrganizationId(), savedPolicy.getPolicyId());
        ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.ProductCatalogResponseDto>> response =
                existingPc.isPresent()
                        ? productCatalogService.update(existingPc.get().getId(), catalogDto)
                        : productCatalogService.create(catalogDto);
        if (response.getBody() != null && response.getBody().getErrorCode() != null) {
            throw new RuntimeException("Failed to create product catalog: " + response.getBody().getMessage());
        }
        logger.info("[correlationId:{}] Product catalog created for policyId: {}", MDC.get("correlationId"), savedPolicy.getPolicyId());
    }

    /**
     * Create a product_catalog row for a PARENT_GMC policy so it appears in enrollment plans.
     */
    private void createProductCatalogForParentGmc(Policy savedPolicy) {
        if (savedPolicy.getOrganizationId() == null || savedPolicy.getPolicyId() == null) return;
        LocalDate effectiveFrom = savedPolicy.getEffectiveFrom() != null ? savedPolicy.getEffectiveFrom() : savedPolicy.getStartDate();
        if (effectiveFrom == null) {
            logger.warn("[correlationId:{}] PARENT_GMC policy {} has no effectiveFrom/startDate; skipping product_catalog", MDC.get("correlationId"), savedPolicy.getPolicyId());
            return;
        }
        String name = (savedPolicy.getDescription() != null && !savedPolicy.getDescription().isBlank())
                ? savedPolicy.getDescription()
                : "Parent / In-Law Coverage";
        if (name.length() > 255) name = name.substring(0, 255);
        List<ProductCatalog> existing = productCatalogRepository.findByOrganizationIdOrderByDisplayOrderAsc(savedPolicy.getOrganizationId());
        int displayOrder = existing.isEmpty() ? 0 : (existing.get(existing.size() - 1).getDisplayOrder() == null ? 0 : existing.get(existing.size() - 1).getDisplayOrder()) + 1;
        ProductCatalogRequestDto catalogDto = ProductCatalogRequestDto.builder()
                .organizationId(savedPolicy.getOrganizationId())
                .productType(ProductType.PARENT_GMC.getValue())
                .name(name)
                .isMandatory(false)
                .coverageOptions("[1]")
                .displayOrder(displayOrder)
                .policyId(savedPolicy.getPolicyId())
                .isActive(true)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(savedPolicy.getEffectiveTo() != null ? savedPolicy.getEffectiveTo() : savedPolicy.getEndDate())
                .build();
        ResponseEntity<ResponseDto<com.vimainsurance.vimaadmin.dto.ProductCatalogResponseDto>> response = productCatalogService.create(catalogDto);
        if (response.getBody() != null && response.getBody().getErrorCode() != null) {
            throw new RuntimeException("Failed to create product catalog for PARENT_GMC: " + response.getBody().getMessage());
        }
        logger.info("[correlationId:{}] Product catalog created for PARENT_GMC policyId: {}", MDC.get("correlationId"), savedPolicy.getPolicyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "policies", entityType = "POLICY", action = "CREATE")
    public ResponseEntity<ResponseDto<String>> uploadPolicyForOrganization(UUID organizationId, PolicyUploadRequestDto requestDto) {
        logger.info("[correlationId:{}] uploadPolicyForOrganization called for organizationId: {}", MDC.get("correlationId"), organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (policyRepository.existsByPolicyNumber(requestDto.getPolicyNumber())) {
                return responseObj.render(responseObj.formErrorResponse(DUPLICATE_POLICY_NUMBER_MESSAGE));
            }
            // Agent is required only when uploading documents (for uploadedBy). Without files, Keycloak-only users (e.g. e2e-vima-admin) can create policies.
            AdminUser agent = null;
            if (requestDto.getFiles() != null && requestDto.getFiles().length > 0) {
                Optional<AdminUser> adminUser = jwtUserExtractor.resolveCurrentAdminUser();
                if (adminUser.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Agent not found"));
                }
                agent = adminUser.get();
            }

            // Create Policy entity
            Policy policy = new Policy();
            policy.setPolicyNumber(requestDto.getPolicyNumber());
            // Organization policies: no primary individual required (company may have no customers/employees yet)
            policy.setPrimaryIndividualId(null);
            policy.setInsuranceProviderId(insuranceProviderRepository.findByProviderCode(requestDto.getProviderCode())
                .orElseThrow(() -> new RuntimeException("Insurance provider not found")).getProviderId());
            policy.setOrganizationId(organizationId);
            ProductType productType = ProductType.fromValue(requestDto.getProductType());
            policy.setProductType(productType);
            // PARENT_GMC / TOP_UP / SUPER_TOP_UP: require active base GMC (or GHI) for the organization
            if (productType == ProductType.PARENT_GMC || productType == ProductType.TOP_UP || productType == ProductType.SUPER_TOP_UP) {
                if (productType == ProductType.PARENT_GMC) {
                    policy.setCoverageType(CoverageType.PARENT);
                }
                if (!organizationHasActiveBaseGmc(organizationId)) {
                    return responseObj.render(responseObj.formErrorResponse(BASE_GMC_REQUIRED_MESSAGE));
                }
            }
            if (productType == ProductType.PARENT_GMC) {
                policy.setCoverageType(CoverageType.PARENT);
            } else if (productType == ProductType.GMC || productType == ProductType.GHI) {
                if (requestDto.getCoverageType() == null || requestDto.getCoverageType().trim().isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse(
                        "Coverage Type is required for GMC/GHI policies (E, ES, ESC, or ESCP)"));
                }
                policy.setCoverageType(CoverageType.fromValue(requestDto.getCoverageType().trim()));
            } else if (requestDto.getCoverageType() != null && !requestDto.getCoverageType().trim().isEmpty()) {
                policy.setCoverageType(CoverageType.fromValue(requestDto.getCoverageType().trim()));
            } else {
                policy.setCoverageType(null);
            }
            policy.setMaxChildrenAllowed(resolveAndValidateMaxChildrenAllowed(
                    productType, policy.getCoverageType(), requestDto.getMaxChildrenAllowed()));
            policy.setStatus(PolicyStatus.fromValue(requestDto.getStatus()));
            policy.setCoveredIndividuals(Arrays.asList(organizationId));

            // GMC: sum_insured = coverage amount; no multiplier.
            // GPA/GTL: MULTIPLIER → sum_insured_multiplier set, sum_insured null; FIXED → sum_insured set, sum_insured_multiplier null.
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
            // policy_wording / claim_checklist PDFs are attached after the policy
            // is saved via attachPolicyWordingDocument / attachClaimChecklistDocument
            // below (so the policyId exists for the S3 key + Document.entityId).
            if ((productType == ProductType.TOP_UP || productType == ProductType.SUPER_TOP_UP) && policy.getPremiumAmount() == null) {
                policy.setPremiumAmount(BigDecimal.ZERO);
            }
            if ((productType == ProductType.TOP_UP || productType == ProductType.SUPER_TOP_UP) && policy.getNetAmount() == null) {
                policy.setNetAmount(BigDecimal.ZERO);
            }
            if ((productType == ProductType.TOP_UP || productType == ProductType.SUPER_TOP_UP) && policy.getGst() == null) {
                policy.setGst(BigDecimal.ZERO);
            }
            if (requestDto.getPaymentFrequency() != null && !requestDto.getPaymentFrequency().isEmpty()) {
                policy.setPaymentFrequency(PaymentFrequency.fromValue(requestDto.getPaymentFrequency()));
            } else {
                policy.setPaymentFrequency(PaymentFrequency.YEARLY);
            }

            // Set TPA details (GMC)
            policy.setTpaOrganizationName(requestDto.getTpaOrganizationName());
            policy.setTpaContactInfo(requestDto.getTpaContactInfo());

            // PARENT_GMC: Parent/In-Law fields
            if (productType == ProductType.PARENT_GMC) {
                policy.setParentCoverageEnabled(Boolean.TRUE.equals(requestDto.getParentCoverageEnabled()));
                policy.setInLawCoverageEnabled(Boolean.TRUE.equals(requestDto.getInLawCoverageEnabled()));
                policy.setMaxParents(requestDto.getMaxParents() != null ? requestDto.getMaxParents() : 0);
                policy.setMaxInLaws(requestDto.getMaxInLaws() != null ? requestDto.getMaxInLaws() : 0);
                policy.setParentAgeLimit(requestDto.getParentAgeLimit());
            }

            // TOP_UP / SUPER_TOP_UP fields
            if (productType == ProductType.TOP_UP || productType == ProductType.SUPER_TOP_UP) {
                validateTopupTieredOptionsRequired(requestDto.getSumInsuredOptions(), requestDto.getTopupPremiumOptions());
                policy.setDescription(requestDto.getDescription());
                policy.setInsurerName(requestDto.getInsurerName());
                policy.setDeductibleAmount(requestDto.getDeductibleAmount());
                policy.setCoversDependents(Boolean.TRUE.equals(requestDto.getCoversDependents()));
                policy.setCoversParents(Boolean.TRUE.equals(requestDto.getCoversParents()));
                policy.setIsDeleted(Boolean.TRUE.equals(requestDto.getIsDeleted()));
                policy.setEffectiveFrom(requestDto.getEffectiveFrom());
                policy.setEffectiveTo(requestDto.getEffectiveTo());
                policy.setSumInsuredOptions(normalizeSumInsuredOptionsForSave(requestDto.getSumInsuredOptions()));
                policy.setTopupPremiumOptions(
                        normalizeTopupPremiumOptionsForSave(policy.getSumInsuredOptions(), requestDto.getTopupPremiumOptions()));
            }

            policy.setPolicyWordingSummary(requestDto.getPolicyWordingSummary());
            policy.setClaimChecklistAdditionalDocs(requestDto.getClaimChecklistAdditionalDocs());

            policy.setCreatedAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());
            
            // Save policy to database
            Policy savedPolicy = policyRepository.save(policy);
            logger.info("[correlationId:{}] Policy saved with ID: {}", MDC.get("correlationId"), savedPolicy.getPolicyId());

            // TOP_UP / SUPER_TOP_UP: create product_catalog row so the product appears in the catalog
            if (productType == ProductType.TOP_UP || productType == ProductType.SUPER_TOP_UP) {
                createProductCatalogForTopup(savedPolicy, requestDto.getPricingModel());
            }
            // PARENT_GMC: create product_catalog row so it appears in enrollment plans
            if (productType == ProductType.PARENT_GMC) {
                createProductCatalogForParentGmc(savedPolicy);
            }
            // NOTE: Default cost-sharing rules are no longer auto-seeded here.
            // See createPolicy() for the rationale — the lookup defaults to 100% employer
            // when no rule exists, so HR-managed rules are now the sole source of truth.

            // Optional policy wording PDF + claim checklist PDF uploaded as multipart parts
            // alongside the policy. Each is independent — failure to upload one shouldn't
            // block policy creation, but we surface validation errors cleanly.
            if (requestDto.getPolicyWordingFile() != null && !requestDto.getPolicyWordingFile().isEmpty()) {
                attachPolicyDocument(savedPolicy, requestDto.getPolicyWordingFile(), DocumentType.POLICY_WORDING);
            }
            if (requestDto.getClaimChecklistFile() != null && !requestDto.getClaimChecklistFile().isEmpty()) {
                attachPolicyDocument(savedPolicy, requestDto.getClaimChecklistFile(), DocumentType.CLAIM_CHECKLIST);
            }

            // Upload documents if provided (agent already looked up above when files present)
            if (requestDto.getFiles() != null && requestDto.getFiles().length > 0 && agent != null) {
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
        } catch (DataIntegrityViolationException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Data integrity violation in uploadPolicyForOrganization: {}", MDC.get("correlationId"), e.getMessage(), e);
            if (isDuplicatePolicyNumberViolation(e)) {
                return responseObj.render(responseObj.formErrorResponse(DUPLICATE_POLICY_NUMBER_MESSAGE));
            }
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("[correlationId:{}] Exception in uploadPolicyForOrganization: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    /**
     * Validates index-aligned premium list vs sum insured list; returns JSON array for {@code policy.topup_premium_options}, or null if premiums omitted.
     */
    private String normalizeTopupPremiumOptionsForSave(String sumInsuredOptionsRaw, String topupPremiumRaw) {
        if (topupPremiumRaw == null || topupPremiumRaw.isBlank()) {
            return null;
        }
        if (sumInsuredOptionsRaw == null || sumInsuredOptionsRaw.isBlank()) {
            throw new BadRequestException(
                    "Sum insured options are required when premium amounts are provided for Top-Up / Super Top-Up.");
        }
        List<BigDecimal> siList = TopupPremiumOptionsUtil.parseDecimalList(sumInsuredOptionsRaw);
        if (siList.isEmpty()) {
            throw new BadRequestException(
                    "Sum insured options are required when premium amounts are provided for Top-Up / Super Top-Up.");
        }
        List<BigDecimal> premList = TopupPremiumOptionsUtil.parseDecimalList(topupPremiumRaw);
        if (premList.isEmpty()) {
            return null;
        }
        if (premList.size() != siList.size()) {
            throw new BadRequestException(String.format(
                    "Premium amounts count (%d) must match sum insured options count (%d) for Top-Up / Super Top-Up.",
                    premList.size(),
                    siList.size()));
        }
        try {
            return objectMapper.writeValueAsString(premList);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BadRequestException("Invalid premium amounts format.");
        }
    }

    private String normalizeSumInsuredOptionsForSave(String sumInsuredOptionsRaw) {
        if (sumInsuredOptionsRaw == null || sumInsuredOptionsRaw.isBlank()) {
            throw new BadRequestException("Sum insured options are required for Top-Up / Super Top-Up.");
        }
        List<BigDecimal> sumInsuredList = TopupPremiumOptionsUtil.parseDecimalList(sumInsuredOptionsRaw);
        if (sumInsuredList.isEmpty()) {
            throw new BadRequestException("Invalid sum insured options format.");
        }
        try {
            return objectMapper.writeValueAsString(sumInsuredList);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BadRequestException("Invalid sum insured options format.");
        }
    }

    private void validateTopupTieredOptionsRequired(String sumInsuredOptionsRaw, String topupPremiumRaw) {
        if (sumInsuredOptionsRaw == null || sumInsuredOptionsRaw.isBlank()) {
            throw new BadRequestException("Sum insured options are required for Top-Up / Super Top-Up.");
        }
        if (topupPremiumRaw == null || topupPremiumRaw.isBlank()) {
            throw new BadRequestException("Premium amounts are required for Top-Up / Super Top-Up.");
        }
    }

    private Integer resolveAndValidateMaxChildrenAllowed(
            ProductType policyType,
            CoverageType coverageType,
            Integer requestMaxChildrenAllowed) {
        if (policyType != ProductType.GMC && policyType != ProductType.GHI) {
            return null;
        }
        if (coverageType != CoverageType.ESC && coverageType != CoverageType.ESCP) {
            return null;
        }
        int resolved = requestMaxChildrenAllowed != null ? requestMaxChildrenAllowed : 4;
        if (resolved < 1 || resolved > 4) {
            throw new BadRequestException("maxChildrenAllowed must be between 1 and 4 for GMC/GHI with ESC/ESCP coverage");
        }
        return resolved;
    }

    private String buildPremiumPreviewOptionsForCatalog(Policy savedPolicy) {
        Map<BigDecimal, BigDecimal> pairMap = TopupPremiumOptionsUtil.buildPreviewMap(
                savedPolicy.getSumInsuredOptions(), savedPolicy.getTopupPremiumOptions());
        if (pairMap.isEmpty()) {
            return null;
        }
        Map<String, BigDecimal> asStringKeyMap = new java.util.LinkedHashMap<>();
        pairMap.forEach((k, v) -> asStringKeyMap.put(k.stripTrailingZeros().toPlainString(), v));
        try {
            return objectMapper.writeValueAsString(asStringKeyMap);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return null;
        }
    }

    private boolean isDuplicatePolicyNumberViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("policies_policy_number_key")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final String DUPLICATE_POLICY_NUMBER_MESSAGE = "Policy number already added";
}
