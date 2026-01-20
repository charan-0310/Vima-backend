package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.DealsDashboardResponseDto;
import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.NomineeRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.MotorPolicyDetailsRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.InsuranceProvider;
import com.vimainsurance.vimaadmin.entity.Nominee;
import com.vimainsurance.vimaadmin.entity.MotorPolicyDetails;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.Gender;
import com.vimainsurance.vimaadmin.enums.NomineeRelationship;
import com.vimainsurance.vimaadmin.enums.PaymentFrequency;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.INomineeRepository;
import com.vimainsurance.vimaadmin.repository.IMotorPolicyDetailsRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IDealsService;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.EnvironmentUtil;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.SlackNotificationUtil;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class DealsServiceImpl implements IDealsService{
    private static final Logger logger = LoggerFactory.getLogger(DealsServiceImpl.class);
    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IDocumentRepository documentRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;
    
    @Autowired
    private IDocumentService documentService;
    
    @Autowired
    private IS3Service s3Service;
    
    @Autowired
    private IPolicyRepository policyRepository;
    
    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;
    
    @Autowired
    private INomineeRepository nomineeRepository;
    
    @Autowired
    private IMotorPolicyDetailsRepository motorPolicyDetailsRepository;
    
    @Autowired
    private SlackNotificationUtil slackNotificationUtil;
    
    @Autowired
    private Environment environment;

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Override
    public ResponseEntity<ResponseDto<String>> createDeals(DealsRequestDto dealsRequestDto) {
        logger.info("[correlationId:{}] createDeals called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try{
            Deals deals = new Deals();
            deals.setFirstName(dealsRequestDto.getFirstName());
            deals.setLastName(dealsRequestDto.getLastName());
            deals.setFullName(dealsRequestDto.getFullName());
            deals.setEmail(dealsRequestDto.getEmail());
            deals.setPhone(dealsRequestDto.getPhone());
            deals.setDateOfBirth(dealsRequestDto.getDateOfBirth());
            deals.setGender(dealsRequestDto.getGender());
            deals.setPanNumber(dealsRequestDto.getPanNumber());
            deals.setAadhaarNumber(dealsRequestDto.getAadhaarNumber());
            deals.setAddress(dealsRequestDto.getAddress());
            deals.setCity(dealsRequestDto.getCity());
            deals.setState(dealsRequestDto.getState());
            deals.setPincode(dealsRequestDto.getPincode());
            deals.setAccountType(AccountType.fromValue(dealsRequestDto.getAccountType()));
            deals.setStatus(AccountStatus.fromValue(dealsRequestDto.getAccountStatus()));
            deals.setEmployeeNumber(dealsRequestDto.getEmployeeNumber());
            deals.setRelationship(dealsRequestDto.getRelationship());
            deals.setDesignation(dealsRequestDto.getDesignation());
            deals.setDateOfJoining(dealsRequestDto.getDateOfJoining());
            deals.setIsPrimaryMember(dealsRequestDto.getIsPrimaryMember());
            deals.setUsername(dealsRequestDto.getUsername());
            deals.setPasswordHash(dealsRequestDto.getPasswordHash());
            deals.setPreferredLanguage(dealsRequestDto.getPreferredLanguage());
            deals.setLeadId(dealsRequestDto.getLeadId());
            deals.setCustId(dealsRequestDto.getCustId());
            deals.setCreatedAt(dealsRequestDto.getCreatedAt());
            deals.setUpdatedAt(dealsRequestDto.getUpdatedAt());
            deals.setPrimaryIndividual(dealsRepository.findById(dealsRequestDto.getPrimaryIndividualId()).orElseThrow(() -> new RuntimeException("Primary individual not found")));
            dealsRepository.save(deals);
            logger.info("[correlationId:{}] createDeals success", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        }catch(Exception e){
            logger.error("Exception in createDeals", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> updateDeals(UUID individualId,
            DealsRequestDto dealsRequestDto) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try{
            Deals deals = dealsRepository.findById(individualId).orElseThrow(() -> new RuntimeException("Deals not found"));
            deals.setFirstName(dealsRequestDto.getFirstName());
            deals.setLastName(dealsRequestDto.getLastName());
            deals.setFullName(dealsRequestDto.getFullName());
            deals.setEmail(dealsRequestDto.getEmail());
            deals.setPhone(dealsRequestDto.getPhone());
            deals.setDateOfBirth(dealsRequestDto.getDateOfBirth());
            deals.setGender(dealsRequestDto.getGender());
            deals.setPanNumber(dealsRequestDto.getPanNumber());
            deals.setAadhaarNumber(dealsRequestDto.getAadhaarNumber());
            deals.setAddress(dealsRequestDto.getAddress());
            deals.setCity(dealsRequestDto.getCity());
            deals.setState(dealsRequestDto.getState());
            deals.setPincode(dealsRequestDto.getPincode());
            deals.setAccountType(AccountType.fromValue(dealsRequestDto.getAccountType()));
            deals.setStatus(AccountStatus.fromValue(dealsRequestDto.getAccountStatus()));
            deals.setEmployeeNumber(dealsRequestDto.getEmployeeNumber());
            deals.setRelationship(dealsRequestDto.getRelationship());
            deals.setDesignation(dealsRequestDto.getDesignation());
            deals.setDateOfJoining(dealsRequestDto.getDateOfJoining());
            deals.setIsPrimaryMember(dealsRequestDto.getIsPrimaryMember());
            deals.setUsername(dealsRequestDto.getUsername());
            deals.setPasswordHash(dealsRequestDto.getPasswordHash());
            deals.setPreferredLanguage(dealsRequestDto.getPreferredLanguage());
            deals.setLeadId(dealsRequestDto.getLeadId());
            deals.setCustId(dealsRequestDto.getCustId());
            deals.setMaritalStatus(dealsRequestDto.getMaritalStatus());
            deals.setSumInsured(dealsRequestDto.getSumInsured());
            deals.setCreatedAt(dealsRequestDto.getCreatedAt());
            deals.setUpdatedAt(dealsRequestDto.getUpdatedAt());
            dealsRepository.save(deals);
            logger.info("[correlationId:{}] createDeals success", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        }catch(Exception e){
            logger.error("Exception in updateDeals", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<DealsResponseDto>> getDealsById(UUID individualId) {
        BaseResponse<DealsResponseDto> responseObj = new BaseResponse<>();
        try{
            Deals deals = dealsRepository.findById(individualId).orElseThrow(() -> new RuntimeException("Deals not found"));
            if(deals.getOrganization() != null){
                return responseObj.render(responseObj.formErrorResponse("Unauthorized access"));
            }
            DealsResponseDto dealsResponseDto = new DealsResponseDto();
            dealsResponseDto.setIndividualId(deals.getIndividualId());
            dealsResponseDto.setFirstName(deals.getFirstName());
            dealsResponseDto.setLastName(deals.getLastName());
            dealsResponseDto.setFullName(deals.getFullName());
            dealsResponseDto.setEmail(deals.getEmail());
            dealsResponseDto.setPhone(deals.getPhone());
            dealsResponseDto.setDateOfBirth(deals.getDateOfBirth());
            dealsResponseDto.setGender(deals.getGender());
            dealsResponseDto.setPanNumber(deals.getPanNumber());
            dealsResponseDto.setAadhaarNumber(deals.getAadhaarNumber());
            dealsResponseDto.setAddress(deals.getAddress());
            dealsResponseDto.setCity(deals.getCity());
            dealsResponseDto.setState(deals.getState());
            dealsResponseDto.setPincode(deals.getPincode());
            dealsResponseDto.setAccountType(deals.getAccountType().getValue());
            dealsResponseDto.setAccountStatus(deals.getStatus().getValue());
            dealsResponseDto.setEmployeeNumber(deals.getEmployeeNumber());
            dealsResponseDto.setRelationship(deals.getRelationship());
            dealsResponseDto.setDesignation(deals.getDesignation());
            dealsResponseDto.setDateOfJoining(deals.getDateOfJoining());
            dealsResponseDto.setIsPrimaryMember(deals.getIsPrimaryMember());
            dealsResponseDto.setUsername(deals.getUsername());
            dealsResponseDto.setPasswordHash(deals.getPasswordHash());
            dealsResponseDto.setPreferredLanguage(deals.getPreferredLanguage());
            dealsResponseDto.setLeadId(deals.getLeadId());
            dealsResponseDto.setMaritalStatus(deals.getMaritalStatus());
            dealsResponseDto.setSumInsured(deals.getSumInsured());
            dealsResponseDto.setCustId(deals.getCustId());
            dealsResponseDto.setUpdatedAt(deals.getUpdatedAt());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dealsResponseDto));
        }catch(Exception e){
            logger.error("Exception in getDealsById", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<DealsResponseDto>>> getAllDeals() {
        BaseResponse<List<DealsResponseDto>> responseObj = new BaseResponse<>();
        try{
            // WARNING: This method loads all deals without pagination - can cause memory issues
            // Use a reasonable maximum limit to prevent memory issues
            int maxLimit = 10000; // Maximum records to fetch
            PageRequest maxPageRequest = PageRequest.of(0, maxLimit);
            Page<Deals> dealsPage = dealsRepository.findAll(maxPageRequest);
            
            if (dealsPage.getTotalElements() > maxLimit) {
                logger.warn("[correlationId:{}] Total deals ({}) exceeds maximum limit ({}). Only returning first {} records.", 
                    MDC.get("correlationId"), dealsPage.getTotalElements(), maxLimit, maxLimit);
            }
            
            List<Deals> deals = dealsPage.getContent().stream()
                .filter(deal -> deal.getIsPrimaryMember() != null && deal.getIsPrimaryMember())
                .collect(Collectors.toList());
            
            List<DealsResponseDto> dealsResponseDtoList = new ArrayList<>();
            
            // Optimized: Batch fetch all policies at once
            List<UUID> individualIds = deals.stream()
                .map(Deals::getIndividualId)
                .toList();
            
            Map<UUID, List<PolicyResponseDto>> policiesMap = new HashMap<>();
            
            if (!individualIds.isEmpty()) {
                // Single query to get all policies for all individuals
                List<Policy> allPolicies = policyRepository.findByPrimaryIndividualIdIn(individualIds);
                
                // Batch fetch insurance providers to avoid N+1 queries
                Set<UUID> providerIds = allPolicies.stream()
                    .map(Policy::getInsuranceProviderId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                
                final Map<UUID, String> finalProviderNamesMap;
                if (!providerIds.isEmpty()) {
                    List<InsuranceProvider> providers = insuranceProviderRepository.findAllById(providerIds);
                    finalProviderNamesMap = providers.stream()
                        .collect(Collectors.toMap(
                            InsuranceProvider::getProviderId,
                            InsuranceProvider::getProviderName,
                            (existing, replacement) -> existing
                        ));
                } else {
                    finalProviderNamesMap = new HashMap<>();
                }
                
                // Limit policies per deal to prevent excessive memory usage
                final int maxPoliciesPerDeal = 50;
                policiesMap = allPolicies.stream()
                    .collect(Collectors.groupingBy(
                        Policy::getPrimaryIndividualId,
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> list.stream()
                                .limit(maxPoliciesPerDeal)
                                .map(policy -> mapPolicyToResponseDto(policy, finalProviderNamesMap))
                                .collect(Collectors.toList())
                        )
                    ));
            }
            
            // Map deals to response DTOs with pre-fetched policies
            for(Deals deal : deals){
                DealsResponseDto dealsResponseDto = mapDealToResponseDto(deal);
                dealsResponseDto.setPolicies(policiesMap.getOrDefault(deal.getIndividualId(), new ArrayList<>()));
                dealsResponseDtoList.add(dealsResponseDto);
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dealsResponseDtoList, dealsPage.getTotalElements()));
        }catch(Exception e){
            logger.error("Exception in getAllDeals", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> deleteDeals(UUID individualId) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try{
            Deals deals = dealsRepository.findById(individualId).orElseThrow(() -> new RuntimeException("Deals not found"));
            deals.setStatus(AccountStatus.INACTIVE);
            dealsRepository.save(deals);
            logger.info("[correlationId:{}] deleteDeals success", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
        }catch(Exception e){
            logger.error("Exception in deleteDeals", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }


    @Override
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(UUID individualId) {
        logger.info("[correlationId:{}] getDocuments called", MDC.get("correlationId"));
        BaseResponse<List<DocumentResponseDto>> responseObj = new BaseResponse<>();
        try {
            // Optimized: Single query to get all documents for the individual
            List<DocumentResponseDto> responseDto = documentRepository.findByEntityId(individualId.toString()).stream().map(document -> {
                DocumentResponseDto documentResponseDto = new DocumentResponseDto();
                documentResponseDto.setDocumentType(document.getDocumentType());
                documentResponseDto.setUploadedAt(document.getUploadedAt());
                documentResponseDto.setDocumentMimeType(document.getMimeType());
                documentResponseDto.setNotes(document.getNotes());
                documentResponseDto.setDocumentId(document.getDocumentId().toString());
                documentResponseDto.setDocumentName(document.getOriginalFilename());
                documentResponseDto.setCategory(document.getDocumentCategory().getValue());
                return documentResponseDto;
            }).collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto,responseDto.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getDocuments: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> uploadDocument(DocumentRequestDto requestDto, UUID individualId) {
        logger.info("[correlationId:{}] uploadDocument called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            final String currentUsername = jwtUserExtractor.extractCurrentUsername();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(currentUsername);
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            AdminUser agent = adminUser.get();
            requestDto.setUploadedBy(agent.getId());
            requestDto.setUploadedByRole(UserRole.fromValue(agent.getRole()));
            ResponseEntity<ResponseDto<List<Document>>> response = documentService.uploadKYCDocuments(requestDto.getFiles(), individualId.toString(), DocumentEntityType.INDIVIDUAL, DocumentType.fromValue(requestDto.getDocumentType()), requestDto.getUploadedBy(), requestDto.getUploadedByRole(), requestDto.getNotes(), DocumentCategory.KYC_DOCUMENTS);
            if(response.getBody() != null && response.getBody().getErrorCode() != null){
                return responseObj.render(responseObj.formErrorResponse(response.getBody().getMessage()));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document uploaded successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in uploadDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<Resource> downloadDocument(String documentId) {
        logger.info("[correlationId:{}] downloadDocument called", MDC.get("correlationId"));
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(UUID.fromString(documentId));
            if(documentOpt.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Document document = documentOpt.get();
            InputStream downloadUrl = s3Service.downloadFile(document.getS3Key());
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"").header("Access-Control-Expose-Headers", "content-disposition")
            .contentType(MediaType.parseMediaType(document.getMimeType())).body(new InputStreamResource(downloadUrl));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in downloadDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> deleteDocument(String documentId) {
        logger.info("[correlationId:{}] deleteDocument called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(UUID.fromString(documentId));
            if(documentOpt.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            s3Service.deleteFile(documentOpt.get().getS3Key());
            if(documentOpt.get().getDocumentType().equals(DocumentType.PAN_CARD) || documentOpt.get().getDocumentType().equals(DocumentType.AADHAAR_CARD)){
                s3Service.deleteFile(documentOpt.get().getS3Key().replace("masked", "original"));
            }
            documentRepository.delete(documentOpt.get());
            logger.info("[correlationId:{}] Document deleted successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document deleted successfully"));
        }
        catch (Exception e) {
            logger.error("[correlationId:{}] Exception in deleteDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    /**
     * Map Document entity to DocumentResponseDto
     */
    private DocumentResponseDto mapDocumentToResponseDto(Document document) {
        DocumentResponseDto documentResponseDto = new DocumentResponseDto();
        documentResponseDto.setDocumentType(document.getDocumentType());
        documentResponseDto.setUploadedAt(document.getUploadedAt());
        documentResponseDto.setDocumentMimeType(document.getMimeType());
        documentResponseDto.setNotes(document.getNotes());
        documentResponseDto.setDocumentId(document.getDocumentId().toString());
        documentResponseDto.setDocumentName(document.getOriginalFilename());
        documentResponseDto.setCategory(document.getDocumentCategory().getValue());
        return documentResponseDto;
    }
    
    /**
     * Maps Deal entity to DealsResponseDto
     */
    private DealsResponseDto mapDealToResponseDto(Deals deal) {
        DealsResponseDto dealsResponseDto = new DealsResponseDto();
        dealsResponseDto.setIndividualId(deal.getIndividualId());
        dealsResponseDto.setFirstName(deal.getFirstName());
        dealsResponseDto.setLastName(deal.getLastName());
        dealsResponseDto.setFullName(deal.getFullName());
        dealsResponseDto.setEmail(deal.getEmail());
        dealsResponseDto.setPhone(deal.getPhone());
        dealsResponseDto.setDateOfBirth(deal.getDateOfBirth());
        dealsResponseDto.setGender(deal.getGender());
        dealsResponseDto.setPanNumber(deal.getPanNumber());
        dealsResponseDto.setAadhaarNumber(deal.getAadhaarNumber());
        dealsResponseDto.setAddress(deal.getAddress());
        dealsResponseDto.setCity(deal.getCity());
        dealsResponseDto.setState(deal.getState());
        dealsResponseDto.setPincode(deal.getPincode());
        dealsResponseDto.setAccountType(deal.getAccountType().getValue());
        dealsResponseDto.setAccountStatus(deal.getStatus().getValue());
        dealsResponseDto.setEmployeeNumber(deal.getEmployeeNumber());
        dealsResponseDto.setRelationship(deal.getRelationship());
        dealsResponseDto.setDesignation(deal.getDesignation());
        dealsResponseDto.setDateOfJoining(deal.getDateOfJoining());
        dealsResponseDto.setIsPrimaryMember(deal.getIsPrimaryMember());
        dealsResponseDto.setUsername(deal.getUsername());
        dealsResponseDto.setPasswordHash(deal.getPasswordHash());
        dealsResponseDto.setPreferredLanguage(deal.getPreferredLanguage());
        dealsResponseDto.setLeadId(deal.getLeadId());
        dealsResponseDto.setCustId(deal.getCustId());
        dealsResponseDto.setMaritalStatus(deal.getMaritalStatus());
        dealsResponseDto.setSumInsured(deal.getSumInsured());
        dealsResponseDto.setCreatedAt(deal.getCreatedAt());
        dealsResponseDto.setUpdatedAt(deal.getUpdatedAt());
        return dealsResponseDto;
    }
    
    /**
     * Maps Policy entity to PolicyResponseDto
     * Uses individual repository lookup (for backward compatibility)
     */
    private PolicyResponseDto mapPolicyToResponseDto(Policy policy) {
        return mapPolicyToResponseDto(policy, null);
    }
    
    /**
     * Maps Policy entity to PolicyResponseDto with pre-fetched provider names map
     * This overload prevents N+1 queries by using a batch-fetched provider map
     */
    private PolicyResponseDto mapPolicyToResponseDto(Policy policy, Map<UUID, String> providerNamesMap) {
        PolicyResponseDto policyResponseDto = new PolicyResponseDto();
        policyResponseDto.setPolicyId(policy.getPolicyId());
        policyResponseDto.setPolicyNumber(policy.getPolicyNumber());
        policyResponseDto.setPrimaryIndividualId(policy.getPrimaryIndividualId());
        
        // Use provider map if available, otherwise fallback to individual query
        if (providerNamesMap != null && policy.getInsuranceProviderId() != null) {
            String providerName = providerNamesMap.get(policy.getInsuranceProviderId());
            if (providerName != null) {
                policyResponseDto.setInsuranceProvider(providerName);
            } else {
                // Fallback if provider not found in map
                policyResponseDto.setInsuranceProvider(
                    insuranceProviderRepository.findById(policy.getInsuranceProviderId())
                        .map(InsuranceProvider::getProviderName)
                        .orElse("Unknown Provider")
                );
            }
        } else {
            // Fallback for backward compatibility
            policyResponseDto.setInsuranceProvider(
                insuranceProviderRepository.findById(policy.getInsuranceProviderId())
                    .orElseThrow(() -> new RuntimeException("Insurance provider not found"))
                    .getProviderName()
            );
        }
        
        policyResponseDto.setInsuranceProductId(policy.getInsuranceProductId());
        policyResponseDto.setOrganizationId(policy.getOrganizationId());
        policyResponseDto.setProductType(policy.getProductType().name());
        policyResponseDto.setCoverageType(policy.getCoverageType().name());
        policyResponseDto.setStatus(policy.getStatus().name());
        policyResponseDto.setCoveredIndividuals(policy.getCoveredIndividuals());
        policyResponseDto.setSumInsured(policy.getSumInsured());
        policyResponseDto.setPremiumAmount(policy.getPremiumAmount());
        policyResponseDto.setStartDate(policy.getStartDate());
        policyResponseDto.setEndDate(policy.getEndDate());
        policyResponseDto.setRenewalDate(policy.getRenewalDate());
        policyResponseDto.setLeadId(policy.getLeadId());
        policyResponseDto.setCreatedAt(policy.getCreatedAt());
        policyResponseDto.setUpdatedAt(policy.getUpdatedAt());
        return policyResponseDto;
    }

    @Override
    public ResponseEntity<ResponseDto<String>> uploadPolicyWithDetails(PolicyUploadRequestDto requestDto) {
        logger.info("[correlationId:{}] uploadPolicyWithDetails called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            // Get current user
            final String currentUsername = jwtUserExtractor.extractCurrentUsername();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(currentUsername);
            if(adminUser.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            AdminUser agent = adminUser.get();
            Deals primaryIndividual = dealsRepository.findById(requestDto.getIndividualId()).orElseThrow(() -> new RuntimeException("Primary individual not found"));
            // Create and save dependents first
            List<UUID> coveredIndividualIds = new ArrayList<>();
            if (requestDto.getDependents() != null && !requestDto.getDependents().isEmpty()) {
                for (DealsRequestDto dependentDto : requestDto.getDependents()) {
                    // Create dependent as a new Deals entity
                    Deals dependent = new Deals();
                    dependent.setFirstName(dependentDto.getFirstName());
                    dependent.setLastName(dependentDto.getLastName());
                    dependent.setFullName(dependentDto.getFullName());
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
                    dependent.setDesignation(dependentDto.getDesignation());
                    dependent.setDateOfJoining(dependentDto.getDateOfJoining());
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
            
            // Create Policy entity
            Policy policy = new Policy();
            policy.setPolicyNumber(requestDto.getPolicyNumber());
            policy.setPrimaryIndividualId(requestDto.getIndividualId());
            policy.setInsuranceProviderId(insuranceProviderRepository.findByProviderCode(requestDto.getProviderCode()).orElseThrow(() -> new RuntimeException("Insurance provider not found")).getProviderId());
            policy.setOrganizationId(UUID.randomUUID()); // Default organization ID
            policy.setProductType(ProductType.valueOf(requestDto.getProductType()));
            policy.setCoverageType(CoverageType.valueOf(requestDto.getCoverageType()));
            policy.setStatus(PolicyStatus.valueOf(requestDto.getStatus()));
            policy.setCoveredIndividuals(coveredIndividualIds);
            policy.setSumInsured(requestDto.getSumInsured());
            policy.setPremiumAmount(requestDto.getPremiumAmount());
            policy.setStartDate(requestDto.getStartDate());
            policy.setEndDate(requestDto.getEndDate());
            policy.setRenewalDate(requestDto.getRenewalDate());
            policy.setNetAmount(requestDto.getNetAmount());
            policy.setGst(requestDto.getGst());
            policy.setLeadId(requestDto.getIndividualId()); // Use individualId as leadId
            if (requestDto.getPaymentFrequency() != null && !requestDto.getPaymentFrequency().isEmpty()) {
                policy.setPaymentFrequency(PaymentFrequency.fromValue(requestDto.getPaymentFrequency()));
            } else {
                policy.setPaymentFrequency(PaymentFrequency.YEARLY);
            }
            policy.setCreatedAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());
            // Save policy to database
            Policy savedPolicy = policyRepository.save(policy);
            logger.info("[correlationId:{}] Policy saved with ID: {}", MDC.get("correlationId"), savedPolicy.getPolicyId());

            if (requestDto.getNominees() != null && !requestDto.getNominees().isEmpty()) {
                List<Nominee> nomineeEntities = new ArrayList<>();
                for (NomineeRequestDto nomineeDto : requestDto.getNominees()) {
                    try {
                        Nominee nominee = mapNomineeDtoToEntity(nomineeDto, savedPolicy);
                        if (nominee != null) {
                            nomineeEntities.add(nominee);
                        }
                    } catch (IllegalArgumentException ex) {
                        logger.warn("[correlationId:{}] Skipping nominee due to invalid data: {}", MDC.get("correlationId"), ex.getMessage());
                    }
                }
                if (!nomineeEntities.isEmpty()) {
                    nomineeRepository.saveAll(nomineeEntities);
                    savedPolicy.getNominees().addAll(nomineeEntities);
                    logger.info("[correlationId:{}] Saved {} nominee(s) for policy {}", MDC.get("correlationId"), nomineeEntities.size(), savedPolicy.getPolicyNumber());
                }
            }

            if (savedPolicy.getProductType() == ProductType.MOTOR) {
                MotorPolicyDetailsRequestDto motorDetailsDto = requestDto.getMotorDetails();
                if (motorDetailsDto != null) {
                    try {
                        MotorPolicyDetails motorDetails = mapMotorPolicyDetailsDto(motorDetailsDto, savedPolicy);
                        motorPolicyDetailsRepository.save(motorDetails);
                        savedPolicy.setMotorPolicyDetails(motorDetails);
                    } catch (IllegalArgumentException ex) {
                        logger.warn("[correlationId:{}] Skipping motor policy details due to invalid data: {}", MDC.get("correlationId"), ex.getMessage());
                    }
                }
            }
            
            // Upload documents if provided
            if (requestDto.getFiles() != null && requestDto.getFiles().length > 0) {
                DocumentRequestDto documentRequest = new DocumentRequestDto();
                documentRequest.setFiles(requestDto.getFiles());
                documentRequest.setDocumentType(requestDto.getDocumentType());
                documentRequest.setNotes(requestDto.getNotes());
                documentRequest.setUploadedBy(agent.getId());
                documentRequest.setUploadedByRole(com.vimainsurance.vimaadmin.enums.UserRole.fromValue(agent.getRole()));
                
                ResponseEntity<ResponseDto<List<Document>>> documentResponse = documentService.uploadKYCDocuments(
                    documentRequest.getFiles(), 
                    requestDto.getIndividualId().toString(), 
                    com.vimainsurance.vimaadmin.enums.DocumentEntityType.POLICY, 
                    com.vimainsurance.vimaadmin.enums.DocumentType.fromValue(requestDto.getDocumentType()), 
                    documentRequest.getUploadedBy(), 
                    documentRequest.getUploadedByRole(), 
                    documentRequest.getNotes(),
                    com.vimainsurance.vimaadmin.enums.DocumentCategory.POLICY_DOCUMENTS
                );
                savedPolicy.setDocument(documentResponse.getBody().getPayload().get(0));
                policyRepository.save(savedPolicy);
                if(documentResponse.getBody() != null && documentResponse.getBody().getErrorCode() != null){
                    return responseObj.render(responseObj.formErrorResponse(documentResponse.getBody().getMessage()));
                }
            }
            
            
            
            // Send Slack notification only in production
            if (EnvironmentUtil.isProductionEnvironment(environment)) {
                try {
                    String slackMessage = buildSlackNotificationMessage(savedPolicy, primaryIndividual, agent);
                    slackNotificationUtil.sendSlackMessage("New Policy Issued!", slackMessage, true);
                } catch (Exception slackException) {
                    logger.warn("[correlationId:{}] Failed to send Slack notification: {}", MDC.get("correlationId"), slackException.getMessage());
                    // Don't fail the request if Slack notification fails
                }
            } else {
                logger.debug("[correlationId:{}] Skipping Slack notification (not in production environment)", MDC.get("correlationId"));
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Policy created and documents uploaded successfully"));
        } catch (Exception e) {
            logger.error("Exception in uploadPolicyWithDetails", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    private String buildSlackNotificationMessage(Policy policy, Deals primaryIndividual, AdminUser agent) {
        StringBuilder message = new StringBuilder();
        
        // Get insurance provider name
        String insurerName = "Unknown";
        try {
            insurerName = insuranceProviderRepository.findById(policy.getInsuranceProviderId())
                    .map(InsuranceProvider::getProviderName)
                    .orElse("Unknown");
        } catch (Exception e) {
            logger.warn("Failed to fetch insurance provider name: {}", e.getMessage());
        }
        
        // Format product type to readable text
        String productTypeDisplay = formatProductType(policy.getProductType().name());
        
        // Client name
        String clientName = primaryIndividual.getFullName();
        
        // Build message with emojis
        message.append(":adult::skin-tone-4: Client: ").append(clientName).append("\n");
        message.append(":package: Policy Type: ").append(productTypeDisplay).append("\n");
        message.append(":office: Insurer: ").append(insurerName);
        message.append(": Policy Number: ").append(policy.getPolicyNumber());
        
        return message.toString();
    }
    
    private String formatProductType(String productType) {
        // Convert enum names to readable format
        // e.g., TERM_LIFE -> Term Life Insurance, HEALTH -> Health Insurance
        if (productType == null || productType.isEmpty()) {
            return "Insurance";
        }
        
        String formatted = productType.replace("_", " ");
        String[] words = formatted.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
        }
        
        // Add "Insurance" suffix if not already present
        if (!result.toString().toLowerCase().contains("insurance")) {
            result.append(" Insurance");
        }
        
        return result.toString();
    }

    private Nominee mapNomineeDtoToEntity(NomineeRequestDto dto, Policy policy) {
        if (dto == null) {
            throw new IllegalArgumentException("Nominee details cannot be null");
        }
        if (dto.getFullName() == null || dto.getFullName().isBlank()) {
            throw new IllegalArgumentException("Nominee full name is required");
        }
        if (dto.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Nominee date of birth is required");
        }
        if (dto.getGender() == null || dto.getGender().isBlank()) {
            throw new IllegalArgumentException("Nominee gender is required");
        }
        if (dto.getRelationship() == null || dto.getRelationship().isBlank()) {
            throw new IllegalArgumentException("Nominee relationship is required");
        }

        Nominee nominee = new Nominee();
        nominee.setPolicy(policy);
        nominee.setFirstName(dto.getFirstName());
        nominee.setLastName(dto.getLastName());
        nominee.setFullName(dto.getFullName());
        nominee.setDateOfBirth(dto.getDateOfBirth());
        nominee.setGender(dto.getGender());
        nominee.setRelationship(dto.getRelationship());
        nominee.setNomineePercentage(dto.getNomineePercentage() != null ? dto.getNomineePercentage() : BigDecimal.valueOf(100.00));
        nominee.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE);
        return nominee;
    }

    private MotorPolicyDetails mapMotorPolicyDetailsDto(MotorPolicyDetailsRequestDto dto, Policy policy) {
        if (dto == null) {
            throw new IllegalArgumentException("Motor policy details cannot be null");
        }
        if (dto.getVehicleRegistrationNumber() == null || dto.getVehicleRegistrationNumber().isBlank()) {
            throw new IllegalArgumentException("Vehicle registration number is required");
        }
        MotorPolicyDetails details = new MotorPolicyDetails();
        details.setPolicy(policy);
        details.setVehicleRegistrationNumber(dto.getVehicleRegistrationNumber());
        details.setVehicleMake(dto.getVehicleMake());
        details.setVehicleModel(dto.getVehicleModel());
        details.setVehicleType(dto.getVehicleType());
        details.setManufacturingYear(dto.getManufacturingYear());
        details.setRegistrationDate(dto.getRegistrationDate());
        details.setIdvValue(dto.getIdvValue());
        return details;
    }

    @Override
    public ResponseEntity<ResponseDto<DealsDashboardResponseDto>> getDashboardMetrics() {
        logger.info("[correlationId:{}] getDashboardMetrics called", MDC.get("correlationId"));
        BaseResponse<DealsDashboardResponseDto> responseObj = new BaseResponse<>();
        try {
            Long totalCustomers = dealsRepository.countByIsPrimaryMemberTrueAndOrganizationIsNull();
            if (totalCustomers == null) {
                totalCustomers = 0L;
            }

            Long totalActivePolicies = policyRepository.countByStatus(PolicyStatus.ACTIVE);
            if (totalActivePolicies == null) {
                totalActivePolicies = 0L;
            }

            // Use aggregation queries instead of loading all active policies into memory
            // This prevents OutOfMemoryError when there are many active policies
            BigDecimal totalCoverage = policyRepository.sumSumInsuredByStatus(PolicyStatus.ACTIVE);
            if (totalCoverage == null) {
                totalCoverage = BigDecimal.ZERO;
            }

            BigDecimal totalPremium = policyRepository.sumPremiumAmountByStatus(PolicyStatus.ACTIVE);
            if (totalPremium == null) {
                totalPremium = BigDecimal.ZERO;
            }

            DealsDashboardResponseDto responseDto = new DealsDashboardResponseDto(
                totalCustomers,
                totalCoverage,
                totalActivePolicies,
                totalPremium
            );

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto, 1));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getDashboardMetrics: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<DealsResponseDto>>> getAllWithFilters(String search, String status, String productType, int page, int rec, String sortBy, String sortDirection) {
        logger.info("[correlationId:{}] Deals getAllWithFilters called with filters - search: {}, status: {}, productType: {}, sortBy: {}, sortDirection: {}", 
                   MDC.get("correlationId"), search, status, productType, sortBy, sortDirection);
        BaseResponse<List<DealsResponseDto>> responseObj = new BaseResponse<>();
        try {
            // Handle special case for getting all deals without pagination
            // WARNING: This can cause memory issues with large datasets - consider adding a maximum limit
            if (page == -1 && rec == -1) {
                // Use a reasonable maximum limit to prevent memory issues
                int maxLimit = 10000; // Maximum records to fetch
                PageRequest maxPageRequest = PageRequest.of(0, maxLimit, createSort(sortBy, sortDirection));
                Page<Deals> dealsPage = dealsRepository.findAllPrimaryMembers(
                    productType != null && !productType.trim().isEmpty() ? ProductType.fromValue(productType).getValue() : null,
                    maxPageRequest
                );
                
                if (dealsPage.getTotalElements() > maxLimit) {
                    logger.warn("[correlationId:{}] Total deals ({}) exceeds maximum limit ({}). Only returning first {} records.", 
                        MDC.get("correlationId"), dealsPage.getTotalElements(), maxLimit, maxLimit);
                }
                
                List<Deals> dealsList = dealsPage.getContent();
                
                // Optimized: Batch fetch all policies at once with limit per deal
                List<UUID> individualIds = dealsList.stream()
                    .map(Deals::getIndividualId)
                    .toList();
                
                Map<UUID, List<PolicyResponseDto>> policiesMap = new HashMap<>();
                
                if (!individualIds.isEmpty()) {
                    List<Policy> allPoliciesRaw = policyRepository.findByPrimaryIndividualIdIn(individualIds);
                    
                    // Filter by productType if provided
                    List<Policy> allPolicies;
                    if (productType != null && !productType.trim().isEmpty()) {
                        try {
                            ProductType productTypeEnum = ProductType.fromValue(productType);
                            allPolicies = allPoliciesRaw.stream()
                                .filter(policy -> policy.getProductType() == productTypeEnum)
                                .collect(Collectors.toList());
                        } catch (IllegalArgumentException e) {
                            logger.warn("[correlationId:{}] Invalid productType: {}", MDC.get("correlationId"), productType);
                            allPolicies = allPoliciesRaw;
                        }
                    } else {
                        allPolicies = allPoliciesRaw;
                    }
                    final List<Policy> finalAllPolicies = allPolicies;
                    
                    // Batch fetch insurance providers to avoid N+1 queries
                    Set<UUID> providerIds = finalAllPolicies.stream()
                        .map(Policy::getInsuranceProviderId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                    
                    final Map<UUID, String> finalProviderNamesMap;
                    if (!providerIds.isEmpty()) {
                        List<InsuranceProvider> providers = insuranceProviderRepository.findAllById(providerIds);
                        finalProviderNamesMap = providers.stream()
                            .collect(Collectors.toMap(
                                InsuranceProvider::getProviderId,
                                InsuranceProvider::getProviderName,
                                (existing, replacement) -> existing
                            ));
                    } else {
                        finalProviderNamesMap = new HashMap<>();
                    }
                    
                    // Limit policies per deal and map to DTOs with provider map
                    final int maxPoliciesPerDeal = 50; // Limit policies per deal to prevent excessive memory usage
                    final Map<UUID, List<PolicyResponseDto>> tempPoliciesMap = finalAllPolicies.stream()
                        .collect(Collectors.groupingBy(
                            Policy::getPrimaryIndividualId,
                            Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                    .limit(maxPoliciesPerDeal)
                                    .map(policy -> mapPolicyToResponseDto(policy, finalProviderNamesMap))
                                    .collect(Collectors.toList())
                            )
                        ));
                    policiesMap = tempPoliciesMap;
                    
                    // Filter deals by productType if provided - only include deals that have at least one policy with the productType
                    if (productType != null && !productType.trim().isEmpty()) {
                        dealsList = dealsList.stream()
                            .filter(deal -> tempPoliciesMap.containsKey(deal.getIndividualId()) && !tempPoliciesMap.get(deal.getIndividualId()).isEmpty())
                            .collect(Collectors.toList());
                    }
                }
                
                List<DealsResponseDto> dealsResponseDtoList = new ArrayList<>();
                for(Deals deal : dealsList){
                    DealsResponseDto dealsResponseDto = mapDealToResponseDto(deal);
                    dealsResponseDto.setPolicies(policiesMap.getOrDefault(deal.getIndividualId(), new ArrayList<>()));
                    dealsResponseDtoList.add(dealsResponseDto);
                }
                
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dealsResponseDtoList, dealsPage.getTotalElements()));
            }
            
            // Create sort object
            Sort sort = createSort(sortBy, sortDirection);
            PageRequest pageRequest = PageRequest.of(page, rec, sort);
            
            // Validate productType string if provided
            String productTypeValue = null;
            if (productType != null && !productType.trim().isEmpty()) {
                try {
                    ProductType productTypeEnum = ProductType.fromValue(productType);
                    productTypeValue = productTypeEnum.getValue(); // Use the enum's string value
                } catch (IllegalArgumentException e) {
                    logger.warn("[correlationId:{}] Invalid productType: {}, ignoring filter", MDC.get("correlationId"), productType);
                    productTypeValue = null;
                }
            }
            
            Page<Deals> dealsPage;
            
            // Determine which query method to use based on search and status filters
            if (search != null && !search.trim().isEmpty() && status != null && !status.trim().isEmpty()) {
                // Both search and status filter
                dealsPage = dealsRepository.searchDealsByStatus(status, search, productTypeValue, pageRequest);
            } else if (search != null && !search.trim().isEmpty()) {
                // Only search filter
                dealsPage = dealsRepository.searchDeals(search, productTypeValue, pageRequest);
            } else if (status != null && !status.trim().isEmpty()) {
                // Only status filter
                dealsPage = dealsRepository.findAllByStatusWithPagination(status, productTypeValue, pageRequest);
            } else {
                // No filters - get all primary members
                dealsPage = dealsRepository.findAllPrimaryMembers(productTypeValue, pageRequest);
            }

            if (dealsPage.isEmpty()) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new ArrayList<>(), 0));
            }
            
            // Optimized: Batch fetch all policies at once with limit per deal
            List<UUID> individualIds = dealsPage.getContent().stream()
                .map(Deals::getIndividualId)
                .toList();
            
            Map<UUID, List<PolicyResponseDto>> policiesMap = new HashMap<>();
            if (!individualIds.isEmpty()) {
                List<Policy> allPolicies = policyRepository.findByPrimaryIndividualIdIn(individualIds);
                
                // Batch fetch insurance providers to avoid N+1 queries
                Set<UUID> providerIds = allPolicies.stream()
                    .map(Policy::getInsuranceProviderId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                
                final Map<UUID, String> finalProviderNamesMap;
                if (!providerIds.isEmpty()) {
                    List<InsuranceProvider> providers = insuranceProviderRepository.findAllById(providerIds);
                    finalProviderNamesMap = providers.stream()
                        .collect(Collectors.toMap(
                            InsuranceProvider::getProviderId,
                            InsuranceProvider::getProviderName,
                            (existing, replacement) -> existing
                        ));
                } else {
                    finalProviderNamesMap = new HashMap<>();
                }
                
                // Limit policies per deal to prevent excessive memory usage
                final int maxPoliciesPerDeal = 50;
                policiesMap = allPolicies.stream()
                    .collect(Collectors.groupingBy(
                        Policy::getPrimaryIndividualId,
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> list.stream()
                                .limit(maxPoliciesPerDeal)
                                .map(policy -> mapPolicyToResponseDto(policy, finalProviderNamesMap))
                                .collect(Collectors.toList())
                        )
                    ));
            }
            
            // Map deals to response DTOs with pre-fetched policies
            // Use ArrayList directly instead of LinkedHashSet for better memory efficiency
            List<DealsResponseDto> dealsResponseList = new ArrayList<>(dealsPage.getContent().size());
            for (Deals deal : dealsPage) {
                DealsResponseDto dealsResponseDto = mapDealToResponseDto(deal);
                dealsResponseDto.setPolicies(policiesMap.getOrDefault(deal.getIndividualId(), new ArrayList<>()));
                dealsResponseList.add(dealsResponseDto);
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dealsResponseList, dealsPage.getTotalElements()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Deals getAllWithFilters: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    /**
     * Create Sort object based on sortBy and sortDirection parameters
     */
    private Sort createSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt"); // Default sort
        }
        
        // Map frontend field names to entity field names
        String entityField = mapSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        return Sort.by(direction, entityField);
    }
    
    /**
     * Map frontend field names to entity field names for sorting
     */
    private String mapSortField(String frontendField) {
        return switch (frontendField.toLowerCase()) {
            case "firstname", "first_name", "name" -> "firstName";
            case "lastname", "last_name" -> "lastName";
            case "fullname", "full_name" -> "fullName";
            case "email" -> "email";
            case "phone" -> "phone";
            case "pannumber", "pan_number", "pan" -> "panNumber";
            case "aadharnumber", "aadhaar_number", "aadhar" -> "aadhaarNumber";
            case "employeenumber", "employee_number", "employeeid", "employee_id" -> "employeeNumber";
            case "city" -> "city";
            case "state" -> "state";
            case "pincode", "pin_code", "pin" -> "pincode";
            case "status", "accountstatus", "account_status" -> "status";
            case "accounttype", "account_type" -> "accountType";
            case "dateofbirth", "date_of_birth", "dob" -> "dateOfBirth";
            case "dateofjoining", "date_of_joining", "doj" -> "dateOfJoining";
            case "createdat", "created_at", "created" -> "createdAt";
            case "updatedat", "updated_at", "updated", "lastactivity", "last_activity" -> "updatedAt";
            default -> "updatedAt"; // Default fallback
        };
    }
    
}
