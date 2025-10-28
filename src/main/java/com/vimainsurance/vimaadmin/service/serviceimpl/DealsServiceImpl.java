package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.DealsDashboardResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IDealsService;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;


import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStream;
import java.time.LocalDateTime;

import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.UserRole;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.MDC;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
    
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

    @Override
    public ResponseEntity<ResponseDto<String>> createDeals(DealsRequestDto dealsRequestDto) {
        logger.info("[correlationId:{}] createDeals called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try{
            Deals deals = new Deals();
            deals.setFirstName(dealsRequestDto.getFirstName());
            deals.setLastName(dealsRequestDto.getLastName());
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
            deals.setIsPrimaryMember(dealsRequestDto.getIsPrimaryMember());
            deals.setUsername(dealsRequestDto.getUsername());
            deals.setPasswordHash(dealsRequestDto.getPasswordHash());
            deals.setPreferredLanguage(dealsRequestDto.getPreferredLanguage());
            deals.setLeadId(dealsRequestDto.getLeadId());
            deals.setCustId(dealsRequestDto.getCustId());
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
            DealsResponseDto dealsResponseDto = new DealsResponseDto();
            dealsResponseDto.setIndividualId(deals.getIndividualId());
            dealsResponseDto.setFirstName(deals.getFirstName());
            dealsResponseDto.setLastName(deals.getLastName());
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
            dealsResponseDto.setIsPrimaryMember(deals.getIsPrimaryMember());
            dealsResponseDto.setUsername(deals.getUsername());
            dealsResponseDto.setPasswordHash(deals.getPasswordHash());
            dealsResponseDto.setPreferredLanguage(deals.getPreferredLanguage());
            dealsResponseDto.setLeadId(deals.getLeadId());
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
            List<Deals> deals = dealsRepository.findAll();
            List<DealsResponseDto> dealsResponseDtoList = new ArrayList<>();
            // Optimized: Batch fetch all policies at once
            List<UUID> individualIds = deals.stream()
                .map(Deals::getIndividualId)
                .toList();
            
            Map<UUID, List<PolicyResponseDto>> policiesMap = new HashMap<>();
            if (!individualIds.isEmpty()) {
                // Single query to get all policies for all individuals
                List<Policy> allPolicies = policyRepository.findByPrimaryIndividualIdIn(individualIds);
                policiesMap = allPolicies.stream()
                    .collect(Collectors.groupingBy(
                        Policy::getPrimaryIndividualId,
                        Collectors.mapping(policy -> mapPolicyToResponseDto(policy), Collectors.toList())
                    ));
            }
            
            // Map deals to response DTOs with pre-fetched policies
            for(Deals deal : deals){
                if(deal.getIsPrimaryMember()){
                    DealsResponseDto dealsResponseDto = mapDealToResponseDto(deal);
                    dealsResponseDto.setPolicies(policiesMap.getOrDefault(deal.getIndividualId(), new ArrayList<>()));
                    dealsResponseDtoList.add(dealsResponseDto);
                }
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dealsResponseDtoList));
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
        logger.info("[correlationId:{}] uploadDocument called", MDC.get("correlationId"));
        BaseResponse<List<DocumentResponseDto>> responseObj = new BaseResponse<>();
        try {
            // Optimized: Single query to get all documents for the individual
            List<DocumentResponseDto> responseDto = documentRepository.findByEntityIdAndCategoryIn(individualId.toString(), List.of(DocumentCategory.KYC_DOCUMENTS, DocumentCategory.POLICY_DOCUMENTS, DocumentCategory.CLAIM_DOCUMENTS, DocumentCategory.OTHER)).stream().map(document -> {
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
            String uploadedBy = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(uploadedBy);
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
        dealsResponseDto.setIsPrimaryMember(deal.getIsPrimaryMember());
        dealsResponseDto.setUsername(deal.getUsername());
        dealsResponseDto.setPasswordHash(deal.getPasswordHash());
        dealsResponseDto.setPreferredLanguage(deal.getPreferredLanguage());
        dealsResponseDto.setLeadId(deal.getLeadId());
        dealsResponseDto.setCustId(deal.getCustId());
        dealsResponseDto.setCreatedAt(deal.getCreatedAt());
        dealsResponseDto.setUpdatedAt(deal.getUpdatedAt());
        return dealsResponseDto;
    }
    
    /**
     * Maps Policy entity to PolicyResponseDto
     */
    private PolicyResponseDto mapPolicyToResponseDto(Policy policy) {
        PolicyResponseDto policyResponseDto = new PolicyResponseDto();
        policyResponseDto.setPolicyId(policy.getPolicyId());
        policyResponseDto.setPolicyNumber(policy.getPolicyNumber());
        policyResponseDto.setPrimaryIndividualId(policy.getPrimaryIndividualId());
        policyResponseDto.setInsuranceProvider(insuranceProviderRepository.findById(policy.getInsuranceProviderId()).orElseThrow(() -> new RuntimeException("Insurance provider not found")).getProviderName());
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
            String uploadedBy = SecurityContextHolder.getContext().getAuthentication().getName();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(uploadedBy);
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
            policy.setLeadId(requestDto.getIndividualId()); // Use individualId as leadId
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
                
                if(documentResponse.getBody() != null && documentResponse.getBody().getErrorCode() != null){
                    return responseObj.render(responseObj.formErrorResponse(documentResponse.getBody().getMessage()));
                }
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Policy created and documents uploaded successfully"));
        } catch (Exception e) {
            logger.error("Exception in uploadPolicyWithDetails", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<DealsDashboardResponseDto>> getDashboardMetrics() {
        logger.info("[correlationId:{}] getDashboardMetrics called", MDC.get("correlationId"));
        BaseResponse<DealsDashboardResponseDto> responseObj = new BaseResponse<>();
        try {
            // Get total customers (primary members only)
            Long totalCustomers = dealsRepository.countByIsPrimaryMemberTrue();
            
            // Get total active policies
            Long totalActivePolicies = policyRepository.countByStatus(PolicyStatus.ACTIVE);
            
            // Get total coverage and premium from active policies
            List<Policy> activePolicies = policyRepository.findByStatus(PolicyStatus.ACTIVE);
            
            BigDecimal totalCoverage = activePolicies.stream()
                .map(Policy::getSumInsured)
                .reduce(BigDecimal.ZERO, BigDecimal::add);  
            
            BigDecimal totalPremium = activePolicies.stream()
                .map(Policy::getPremiumAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new DealsDashboardResponseDto(totalCustomers, totalCoverage, totalActivePolicies, totalPremium), 1));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getDashboardMetrics: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

}
