package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.BulkEmployeeDeletionRequestDto;
import com.vimainsurance.vimaadmin.dto.CsvValidationResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.ManualAddEmployeesRequestDto;
import com.vimainsurance.vimaadmin.dto.ManualDeleteEmployeesRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeeUploadDto;
import com.vimainsurance.vimaadmin.exception.OrganizationAccessDeniedException;
import com.vimainsurance.vimaadmin.dto.EmployeeUploadResponse;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeDto;
import com.vimainsurance.vimaadmin.dto.OrganizationRequestDto;
import com.vimainsurance.vimaadmin.dto.OrganizationResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.DealEndorsement;
import com.vimainsurance.vimaadmin.entity.CostSharingRule;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.CoverageCategory;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.EmployerShareType;
import com.vimainsurance.vimaadmin.enums.Industry;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.ICostSharingRuleRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.FeatureFlagService;
import com.vimainsurance.vimaadmin.service.IOrganizationService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.specification.OrganizationSpecification;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.CsvDealsReaderUtil;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.KeyCloakUtil;
import com.vimainsurance.vimaadmin.util.OrganizationAccessHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OrganizationServiceImpl implements IOrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Autowired
    private IDocumentRepository documentRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private IDocumentService documentService;

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IS3Service s3Service;

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Autowired(required = false)
    private OrganizationAccessHelper organizationAccessHelper;

    @Autowired
    private Environment environment;

    @Autowired
    private IDealEndorsementRepository dealEndorsementRepository;

    @Autowired
    private IPolicyRepository policyRepository;

    @Autowired
    private KeyCloakUtil keycloakUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ICostSharingRuleRepository costSharingRuleRepository;

    @Autowired
    private FeatureFlagService featureFlagService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "organizations", entityType = "ORGANIZATION", action = "CREATE")
    public ResponseEntity<ResponseDto<String>> create(OrganizationRequestDto requestDto) {
        logger.info("[correlationId:{}] Organization create called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            // Optional uniqueness check by name
            if (requestDto.getOrganizationName() != null && organizationRepository.findByOrganizationName(requestDto.getOrganizationName()).isPresent()) {
                return responseObj.render(responseObj.formErrorResponse("Organization already exists"));
            }
            Organization org = new Organization();
            org.setOrganizationName(requestDto.getOrganizationName());
            org.setGstin(requestDto.getGstin());
            org.setPanNumber(requestDto.getPanNumber());
            org.setPrimaryContactName(requestDto.getPrimaryContactName());
            org.setPrimaryContactEmail(requestDto.getPrimaryContactEmail());
            org.setPrimaryContactPhone(requestDto.getPrimaryContactPhone());
            org.setRegisteredAddress(requestDto.getRegisteredAddress());
            if (requestDto.getStatus() != null) {
                org.setStatus(requestDto.getStatus());
            }
            if (requestDto.getIndustry() != null && !requestDto.getIndustry().trim().isEmpty()) {
                org.setIndustry(Industry.fromValue(requestDto.getIndustry()));
            }
            Organization savedOrg = organizationRepository.save(org);
            seedDefaultCostSharingRules(savedOrg.getOrganizationId());
            String orgGroupName = "ORG_" + savedOrg.getOrganizationName().trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
            keycloakUtil.createGroup(orgGroupName, Map.of("organization_id", List.of(savedOrg.getOrganizationId().toString())));
            try {
                featureFlagService.seedOrganizationFeaturesFromHrAdminRole(savedOrg.getOrganizationId().toString());
            } catch (Exception seedEx) {
                logger.warn("[correlationId:{}] Could not seed default org feature flags for {}: {}",
                        MDC.get("correlationId"), savedOrg.getOrganizationId(), seedEx.getMessage());
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization create: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "organizations", entityType = "ORGANIZATION", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> update(OrganizationRequestDto requestDto) {
        logger.info("[correlationId:{}] Organization update called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (requestDto.getOrganizationId() == null) {
                return responseObj.render(responseObj.formErrorResponse("Organization ID is required"));
            }
            if (organizationAccessHelper != null) {
                organizationAccessHelper.validateAndSetContext(requestDto.getOrganizationId());
            } else if (jwtUserExtractor != null) {
                jwtUserExtractor.validateOrganizationAccess(requestDto.getOrganizationId());
                AuditContextSupplier.setOrganizationId(requestDto.getOrganizationId());
            }
            Optional<Organization> opt = organizationRepository.findById(requestDto.getOrganizationId());
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            Organization org = opt.get();
            try {
                AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(org));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Could not serialize organization for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
            }
            if (requestDto.getOrganizationName() != null) org.setOrganizationName(requestDto.getOrganizationName());
            if (requestDto.getGstin() != null) org.setGstin(requestDto.getGstin());
            if (requestDto.getPanNumber() != null) org.setPanNumber(requestDto.getPanNumber());
            if (requestDto.getPrimaryContactName() != null) org.setPrimaryContactName(requestDto.getPrimaryContactName());
            if (requestDto.getPrimaryContactEmail() != null) org.setPrimaryContactEmail(requestDto.getPrimaryContactEmail());
            if (requestDto.getPrimaryContactPhone() != null) org.setPrimaryContactPhone(requestDto.getPrimaryContactPhone());
            if (requestDto.getStatus() != null) org.setStatus(requestDto.getStatus());
            if (requestDto.getRegisteredAddress() != null) org.setRegisteredAddress(requestDto.getRegisteredAddress());
            if (requestDto.getIndustry() != null && !requestDto.getIndustry().trim().isEmpty()) {
                org.setIndustry(Industry.fromValue(requestDto.getIndustry()));
            }
            org.setUpdatedAt(java.time.LocalDateTime.now());
            organizationRepository.save(org);
            AuditContextSupplier.setNewSnapshotEntity(org);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization update: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "organizations", entityType = "ORGANIZATION", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> delete(UUID organizationId) {
        logger.info("[correlationId:{}] Organization delete called for {}", MDC.get("correlationId"), organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Organization> opt = organizationRepository.findById(organizationId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            Organization org = opt.get();
            org.setStatus("INACTIVE");
            org.setUpdatedAt(java.time.LocalDateTime.now());
            organizationRepository.save(org);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization delete: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error Occured while deleting organization"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<OrganizationResponseDto>> getById(UUID organizationId) {
        logger.info("[correlationId:{}] Organization getById called for {}", MDC.get("correlationId"), organizationId);
        BaseResponse<OrganizationResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<Organization> opt = organizationRepository.findById(organizationId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, mapToResponseDto(opt.get())));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization getById: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAll() {
        logger.info("[correlationId:{}] Organization getAll called", MDC.get("correlationId"));
        BaseResponse<List<OrganizationResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<Organization> list = organizationRepository.findAll();
            List<OrganizationResponseDto> out = new ArrayList<>();
            for (Organization org : list) {
                out.add(mapToResponseDto(org));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization getAll: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAllActive() {
        logger.info("[correlationId:{}] Organization getAllActive called", MDC.get("correlationId"));
        BaseResponse<List<OrganizationResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<Organization> list = organizationRepository.findAllByStatus("ACTIVE");
            List<OrganizationResponseDto> out = new ArrayList<>();
            for (Organization org : list) {
                out.add(mapToResponseDto(org));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization getAllActive: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<OrganizationResponseDto>>> getAllWithFilters(String search, String status, int page, int rec, String sortBy, String sortDirection) {
        logger.info("[correlationId:{}] Organization getAllWithFilters called with filters - search: {}, status: {}, sortBy: {}, sortDirection: {}", 
                   MDC.get("correlationId"), search, status, sortBy, sortDirection);
        BaseResponse<List<OrganizationResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<UUID> organizationIds = jwtUserExtractor.getCurrentOrganizations().stream().map(UUID::fromString).collect(Collectors.toList());
            // Handle special case for getting all organizations without pagination
            // WARNING: This can cause memory issues with large datasets - consider adding a maximum limit
            if (page == -1 && rec == -1) {
                // Use a reasonable maximum limit to prevent memory issues
                int maxLimit = 10000; // Maximum records to fetch
                PageRequest maxPageRequest = PageRequest.of(0, maxLimit, createSort(sortBy, sortDirection));
                Page<Organization> orgPage = organizationRepository.findAll(maxPageRequest);
                
                if (orgPage.getTotalElements() > maxLimit) {
                    logger.warn("[correlationId:{}] Total organizations ({}) exceeds maximum limit ({}). Only returning first {} records.", 
                        MDC.get("correlationId"), orgPage.getTotalElements(), maxLimit, maxLimit);
                }
                
                List<OrganizationResponseDto> out = new ArrayList<>();
                for (Organization org : orgPage.getContent()) {
                    out.add(mapToResponseDto(org));
                }
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, orgPage.getTotalElements()));
            }
            
            // Create sort object
            Sort sort = createSort(sortBy, sortDirection);
            PageRequest pageRequest = PageRequest.of(page, rec, sort);
            
            // Build specification with filters
            Specification<Organization> spec = OrganizationSpecification.withFilters(organizationIds, search, status);
            
            // Execute query using specification
            Page<Organization> organizationPage = organizationRepository.findAll(spec, pageRequest);

            LinkedHashSet<OrganizationResponseDto> organizationResponseSet = new LinkedHashSet<>();
            if (organizationPage.isEmpty()) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, new ArrayList<>(), 0));
            }
            
            for (Organization org : organizationPage) {
                organizationResponseSet.add(mapToResponseDto(org));
            }
            
            List<OrganizationResponseDto> uniqueList = new ArrayList<>(organizationResponseSet);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, uniqueList, organizationPage.getTotalElements()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization getAllWithFilters: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error Occured while getting organizations"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> uploadLogo(MultipartFile file, UUID organizationId) {
        logger.info("[correlationId:{}] uploadLogo called for organization {}", MDC.get("correlationId"), organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            // Verify organization exists
            Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            // TODO: Implement logo upload logic (similar to document upload but for logo field)
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Logo upload functionality to be implemented"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in uploadLogo: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error Occured while uploading logo"));
        }
    }

    @Override
    @AuditedOperation(schemaName = "document", tableName = "documents", entityType = "ORGANIZATION_DOCUMENT", action = "CREATE")
    public ResponseEntity<ResponseDto<String>> uploadDocument(DocumentRequestDto requestDto, UUID organizationId) {
        logger.info("[correlationId:{}] uploadDocument called for organization {}", MDC.get("correlationId"), organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            String uploadedBy = jwtUserExtractor.getCurrentUsername();
            Optional<AdminUser> adminUser = adminUserRepository.findByUsername(uploadedBy);
            if (adminUser.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Agent not found"));
            }
            AdminUser agent = adminUser.get();
            requestDto.setUploadedBy(agent.getId());
            requestDto.setUploadedByRole(UserRole.fromValue(agent.getRole()));
            ResponseEntity<ResponseDto<List<Document>>> response = documentService.uploadKYCDocuments(
                requestDto.getFiles(),
                organizationId.toString(),
                DocumentEntityType.ORGANIZATION,
                DocumentType.fromValue(requestDto.getDocumentType()),
                requestDto.getUploadedBy(),
                requestDto.getUploadedByRole(),
                requestDto.getNotes(),
                DocumentCategory.fromValue(requestDto.getDocumentCategory())
            );
            ResponseDto<List<Document>> responseBody = response.getBody();
            if (responseBody != null && responseBody.getErrorCode() != null) {
                return responseObj.render(responseObj.formErrorResponse("Error occurred while uploading document: " + responseBody.getMessage()));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document uploaded successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in uploadDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error occurred while uploading document"));
        }
    }

    @Override
    public ResponseEntity<Resource> downloadDocument(UUID organizationId, String documentId) {
        logger.info("[correlationId:{}] downloadDocument called for document {}", MDC.get("correlationId"), documentId);
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(UUID.fromString(documentId));
            if (documentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Document document = documentOpt.get();
            InputStream downloadUrl = s3Service.downloadFile(document.getS3Key());
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .header("Access-Control-Expose-Headers", "content-disposition")
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .body(new InputStreamResource(downloadUrl));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in downloadDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Override
    @AuditedOperation(schemaName = "document", tableName = "documents", entityType = "ORGANIZATION_DOCUMENT", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> deleteDocument(UUID organizationId, String documentId) {
        logger.info("[correlationId:{}] deleteDocument called for document {}", MDC.get("correlationId"), documentId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(UUID.fromString(documentId));
            if (documentOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            Document document = documentOpt.get();
            if (policyRepository.existsByDocument_DocumentId(document.getDocumentId())) {
                return responseObj.render(responseObj.formErrorResponse("Document is mapped with policy #" + policyRepository.findByDocument_DocumentId(document.getDocumentId()).get().getPolicyNumber() + " and cannot be deleted"));
            }
            s3Service.deleteFile(document.getS3Key());
            if (document.getDocumentType().equals(DocumentType.PAN_CARD) || document.getDocumentType().equals(DocumentType.AADHAAR_CARD)) {
                s3Service.deleteFile(document.getS3Key().replace("masked", "original"));
            }
            documentRepository.delete(document);
            logger.info("[correlationId:{}] Document deleted successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document deleted successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in deleteDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error occurred while deleting document"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(UUID organizationId) {
        logger.info("[correlationId:{}] getDocuments called for organization {}", MDC.get("correlationId"), organizationId);
        BaseResponse<List<DocumentResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<DocumentResponseDto> responseDto = documentRepository
                .findByEntityId(
                    organizationId.toString()
                )
                .stream()
                .map(document -> {
                    DocumentResponseDto documentResponseDto = new DocumentResponseDto();
                    documentResponseDto.setDocumentType(document.getDocumentType());
                    documentResponseDto.setUploadedAt(document.getUploadedAt());
                    documentResponseDto.setDocumentMimeType(document.getMimeType());
                    documentResponseDto.setNotes(document.getNotes());
                    documentResponseDto.setDocumentId(document.getDocumentId().toString());
                    documentResponseDto.setDocumentName(document.getOriginalFilename());
                    documentResponseDto.setCategory(document.getDocumentCategory().getValue());
                    return documentResponseDto;
                })
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto, responseDto.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getDocuments: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error occurred while getting documents"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployees(UUID organizationId) {
        logger.info("[correlationId:{}] getEmployees called for organization {}", MDC.get("correlationId"), organizationId);
        BaseResponse<List<OrganizationEmployeeDto>> responseObj = new BaseResponse<>();
        try {
            List<Deals> dealsList = dealsRepository.findByOrganizationId(organizationId);
            List<Deals> employees = dealsList.stream()
                .filter(deal -> Boolean.TRUE.equals(deal.getIsPrimaryMember()))
                .collect(Collectors.toList());
            
            // Fetch all dependents for all employees in one query for better performance
            List<UUID> employeeIds = employees.stream()
                .map(Deals::getIndividualId)
                .collect(Collectors.toList());
            
            Map<UUID, Integer> dependentCountMap = new HashMap<>();
            if (!employeeIds.isEmpty()) {
                List<Deals> allDependents = dealsRepository.findByPrimaryIndividualIdIn(employeeIds);
                dependentCountMap = allDependents.stream()
                    .filter(dependent -> dependent.getPrimaryIndividual() != null)
                    .collect(Collectors.groupingBy(
                        dependent -> dependent.getPrimaryIndividual().getIndividualId(),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));
            }
            
            final Map<UUID, Integer> finalDependentCountMap = dependentCountMap;
            List<OrganizationEmployeeDto> responseDto = employees.stream()
                .map(deal -> {
                    OrganizationEmployeeDto dto = mapToOrganizationEmployeeDto(deal);
                    dto.setDependentCount(finalDependentCountMap.getOrDefault(deal.getIndividualId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto, responseDto.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error occurred while getting employees"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<OrganizationEmployeeDto>> getEmployee(UUID individualId, UUID organizationId) {
        logger.info("[correlationId:{}] getEmployee called for individualId: {}, organizationId: {}",
            MDC.get("correlationId"), individualId.toString(), organizationId);
        BaseResponse<OrganizationEmployeeDto> responseObj = new BaseResponse<>();
        try {
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            
            // Find employee by employee number and organization
            Optional<Deals> employeeOpt = dealsRepository.findByIndividualIdAndOrganizationId(individualId, organizationId);
            
            if (employeeOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(String.format("Employee individual not found for organization")));
            }
            
            Deals employee = employeeOpt.get();
            
            // Ensure it's a primary member (employee)
            if (!Boolean.TRUE.equals(employee.getIsPrimaryMember())) {
                return responseObj.render(responseObj.formErrorResponse("Unauthorized access"));
            }
            
            OrganizationEmployeeDto responseDto = mapToOrganizationEmployeeDto(employee);
            // Count dependents for this employee
            List<Deals> dependents = dealsRepository.findByPrimaryIndividual(employee);
            responseDto.setDependentCount(dependents.size());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getEmployee: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error Occured while getting employee"));
        }
    }
    
    @Override
    public ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployeeDependents(UUID individualId, UUID organizationId) {
        logger.info("[correlationId:{}] getEmployeeDependents called for employeeId: {}, organizationId: {}", 
            MDC.get("correlationId"), individualId.toString(), organizationId);
        BaseResponse<List<OrganizationEmployeeDto>> responseObj = new BaseResponse<>();
        try {
            // Validate organization
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            
            // Find employee by employee number and organization
            Optional<Deals> employeeOpt = dealsRepository.findByIndividualIdAndOrganizationId(individualId, organizationId);
            
            if (employeeOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(String.format("Employee individual not found for organization")));
            }
            
            Deals employee = employeeOpt.get();
            
            // Ensure it's a primary member (employee)
            if (!Boolean.TRUE.equals(employee.getIsPrimaryMember())) {
                return responseObj.render(responseObj.formErrorResponse(String.format("Unauthorized access")));
            }
            
            // Find all dependents for this employee
            List<Deals> dependents = dealsRepository.findByPrimaryIndividual(employee);
            
            // Map to DTOs
            List<OrganizationEmployeeDto> responseDtoList = dependents.stream()
                .map(this::mapToOrganizationEmployeeDto)
                .collect(Collectors.toList());
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDtoList, responseDtoList.size()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getEmployeeDependents: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Maps Deals entity to OrganizationEmployeeDto with only necessary fields
     */
    private OrganizationEmployeeDto mapToOrganizationEmployeeDto(Deals deal) {
        OrganizationEmployeeDto dto = new OrganizationEmployeeDto();
        dto.setIndividualId(deal.getIndividualId());
        dto.setFirstName(deal.getFirstName());
        dto.setLastName(deal.getLastName());
        dto.setEmail(deal.getEmail());
        dto.setPhone(deal.getPhone());
        dto.setEmployeeNumber(deal.getEmployeeNumber());
        dto.setDesignation(deal.getDesignation());
        dto.setDateOfJoining(deal.getDateOfJoining());
        dto.setStatus(deal.getStatus() != null ? deal.getStatus().name() : null);
        dto.setDateOfBirth(deal.getDateOfBirth());
        dto.setGender(deal.getGender());
        dto.setIsPrimaryMember(deal.getIsPrimaryMember());
        dto.setRelationship(deal.getRelationship());
        dto.setActualRelationship(deal.getActualRelationship());
        dto.setOrganizationName(deal.getOrganization().getOrganizationName());
        dto.setFullName(deal.getFullName());
        dto.setSumInsured(deal.getSumInsured());
        dto.setHealthId(deal.getHealthId());
        // Self-enrollment: expose actual enrollment status (ACTIVE, APPROVED, PENDING, etc.); admin-enrolled: null
        if (deal.getEnrollmentWindow() != null && deal.getEnrollmentStatus() != null) {
            dto.setEnrollementStatus(deal.getEnrollmentStatus().name());
        } else {
            dto.setEnrollementStatus(deal.getEnrollmentWindow() != null ? "PENDING" : null);
        }
        return dto;
    }

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
    
    private String mapSortField(String frontendField) {
        return switch (frontendField.toLowerCase()) {
            case "organizationname", "organization_name", "name" -> "organizationName";
            case "gstin" -> "gstin";
            case "pannumber", "pan_number", "pan" -> "panNumber";
            case "contactname", "primarycontactname", "primary_contact_name" -> "primaryContactName";
            case "email", "primarycontactemail", "primary_contact_email" -> "primaryContactEmail";
            case "phone", "primarycontactphone", "primary_contact_phone" -> "primaryContactPhone";
            case "status" -> "status";
            case "industry" -> "industry";
            case "createdat", "created_at", "created" -> "createdAt";
            case "updatedat", "updated_at", "updated", "lastactivity", "last_activity" -> "updatedAt";
            default -> "updatedAt"; // Default fallback
        };
    }

    private void seedDefaultCostSharingRules(UUID organizationId) {
        LocalDate effectiveFrom = LocalDate.now();
        List<CostSharingRule> defaults = buildDefaultCostSharingRules(organizationId, effectiveFrom);
        for (CostSharingRule rule : defaults) {
            boolean exists = costSharingRuleRepository.existsByCompanyIdAndPlanTypeAndCoverageCategoryAndEffectiveFrom(
                    rule.getCompanyId(),
                    rule.getPlanType(),
                    rule.getCoverageCategory(),
                    rule.getEffectiveFrom());
            if (exists) {
                continue;
            }
            try {
                costSharingRuleRepository.save(rule);
            } catch (DataIntegrityViolationException ex) {
                logger.info("[correlationId:{}] Default cost-sharing rule already exists for org {} plan {} category {} effectiveFrom {}",
                        MDC.get("correlationId"),
                        organizationId,
                        rule.getPlanType(),
                        rule.getCoverageCategory(),
                        rule.getEffectiveFrom());
            }
        }
    }

    private List<CostSharingRule> buildDefaultCostSharingRules(UUID organizationId, LocalDate effectiveFrom) {
        return List.of(
                defaultRule(organizationId, "GMC", CoverageCategory.SELF, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GMC", CoverageCategory.SPOUSE, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GMC", CoverageCategory.CHILD, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GMC", CoverageCategory.PARENT, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GPA", CoverageCategory.ALL_DEPENDENTS, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "GTL", CoverageCategory.ALL_DEPENDENTS, BigDecimal.valueOf(100), effectiveFrom),
                defaultRule(organizationId, "TOP_UP", CoverageCategory.SELF, BigDecimal.ZERO, effectiveFrom),
                defaultRule(organizationId, "SUPER_TOP_UP", CoverageCategory.SELF, BigDecimal.ZERO, effectiveFrom));
    }

    private CostSharingRule defaultRule(
            UUID organizationId,
            String planType,
            CoverageCategory coverageCategory,
            BigDecimal employerShareValue,
            LocalDate effectiveFrom) {
        return CostSharingRule.builder()
                .companyId(organizationId)
                .planType(planType)
                .coverageCategory(coverageCategory)
                .employerShareType(EmployerShareType.PERCENTAGE)
                .employerShareValue(employerShareValue)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .isDeleted(false)
                .build();
    }

    private OrganizationResponseDto mapToResponseDto(Organization org) {
        OrganizationResponseDto dto = new OrganizationResponseDto();
        dto.setOrganizationId(org.getOrganizationId());
        dto.setOrganizationName(org.getOrganizationName());
        dto.setGstin(org.getGstin());
        dto.setPanNumber(org.getPanNumber());
        dto.setPrimaryContactName(org.getPrimaryContactName());
        dto.setPrimaryContactEmail(org.getPrimaryContactEmail());
        dto.setPrimaryContactPhone(org.getPrimaryContactPhone());
        dto.setStatus(org.getStatus());
        dto.setCreatedAt(org.getCreatedAt());
        dto.setUpdatedAt(org.getUpdatedAt());
        dto.setRegisteredAddress(org.getRegisteredAddress());
        dto.setIndustry(org.getIndustry() != null ? org.getIndustry().getValue() : null);
        dto.setEmployeesCount(dealsRepository.countByOrganizationId(org.getOrganizationId()));
        dto.setPolicyCount(policyRepository.countByOrganizationId(org.getOrganizationId()));
        dto.setTotalPremiumAmount(policyRepository.sumPremiumAmountByOrganizationId(org.getOrganizationId()));
        return dto;
    }

    /**
     * Validates CSV file without processing it
     * @param file CSV file to validate
     * @param organizationId Organization ID
     * @param operation Operation type: "upload" or "delete"
     * @return Validation response with errors and warnings
     */
    @Override
    public ResponseEntity<ResponseDto<CsvValidationResponseDto>> validateCsv(MultipartFile file, UUID organizationId, String operation) {
        logger.info("[correlationId:{}] validateCsv called with organizationId: {}, operation: {}", 
            MDC.get("correlationId"), organizationId, operation);
        BaseResponse<CsvValidationResponseDto> responseObj = new BaseResponse<>();
        
        try {
            // Validate file
            List<String> validationErrors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            if (file == null || file.isEmpty()) {
                CsvValidationResponseDto errorResponse = new CsvValidationResponseDto(
                    0, 0, 1, 
                    List.of("File is empty or not provided"), 
                    "File validation failed", 
                    false
                );
                ResponseDto<CsvValidationResponseDto> response = new ResponseDto<>(0, "File is empty or not provided", errorResponse);
                return responseObj.render(response);
            }
            
            // Validate file type
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".CSV") || !filename.endsWith(".xlsx") && !filename.endsWith(".XLSX") || !filename.endsWith(".xls") && !filename.endsWith(".XLS"))) {
                CsvValidationResponseDto errorResponse = new CsvValidationResponseDto(
                    0, 0, 1, 
                    List.of("Invalid file type. Only CSV files are allowed"), 
                    "File validation failed", 
                    false
                );
                ResponseDto<CsvValidationResponseDto> response = new ResponseDto<>(0, "Invalid file type. Only CSV files are allowed", errorResponse);
                return responseObj.render(response);
            }
            
            // Validate operation type
            if (operation == null || (!operation.equalsIgnoreCase("upload") && !operation.equalsIgnoreCase("delete"))) {
                CsvValidationResponseDto errorResponse = new CsvValidationResponseDto(
                    0, 0, 1, 
                    List.of("Invalid operation type. Must be 'upload' or 'delete'"), 
                    "File validation failed", 
                    false
                );
                ResponseDto<CsvValidationResponseDto> response = new ResponseDto<>(0, "Invalid operation type", errorResponse);
                return responseObj.render(response);
            }
            
            // Validate and fetch organization
            Organization organization = null;
            if (organizationId != null) {
                Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
                if (orgOpt.isEmpty()) {
                    CsvValidationResponseDto errorResponse = new CsvValidationResponseDto(
                        0, 0, 1, 
                        List.of("Organization not found with ID: " + organizationId), 
                        "File validation failed", 
                        false
                    );
                    ResponseDto<CsvValidationResponseDto> response = new ResponseDto<>(0, "Organization not found", errorResponse);
                    return responseObj.render(response);
                }
                organization = orgOpt.get();
                logger.info("[correlationId:{}] Organization found: {}", MDC.get("correlationId"), organization.getOrganizationName());
            }
            
            // Parse CSV file
            CsvDealsReaderUtil.CsvParseResult parseResult = CsvDealsReaderUtil.parseGroupedCsvToDeals(file);
            validationErrors.addAll(parseResult.getErrors());
            
            if (parseResult.getDeals().isEmpty() && !parseResult.getErrors().isEmpty()) {
                CsvValidationResponseDto errorResponse = new CsvValidationResponseDto(
                    parseResult.getTotalRows(), 
                    0, 
                    parseResult.getErrorCount(),
                    validationErrors,
                    List.of(),
                    "Failed to parse any valid records from CSV",
                    false,
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<CsvValidationResponseDto> response = new ResponseDto<>(0, "Failed to parse CSV file", errorResponse);
                return responseObj.render(response);
            }
            
            // Perform operation-specific validation
            if ("upload".equalsIgnoreCase(operation)) {
                validateForUpload(parseResult.getDeals(), organization, validationErrors, warnings);
            } else if ("delete".equalsIgnoreCase(operation)) {
                validateForDelete(parseResult.getDeals(), organization, validationErrors, warnings);
            }
            
            // Calculate validation results
            int validRows = parseResult.getDeals().size() - validationErrors.size() + parseResult.getErrors().size();
            int invalidRows = validationErrors.size();
            boolean isValid = invalidRows == 0;
            
            String message = isValid 
                ? String.format("CSV validation passed. %d valid rows found", validRows)
                : String.format("CSV validation failed. %d errors found", invalidRows);
            
            CsvValidationResponseDto validationResponse = new CsvValidationResponseDto(
                parseResult.getTotalRows(),
                validRows,
                invalidRows,
                validationErrors,
                warnings,
                message,
                isValid,
                parseResult.getTotalEmployees(),
                parseResult.getTotalDependents()
            );
            

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, validationResponse));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in validateCsv: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Validates CSV data for upload operation
     */
    private void validateForUpload(List<Deals> deals, Organization organization, 
                                   List<String> validationErrors, List<String> warnings) {
        if (organization == null) {
            validationErrors.add("Organization is required for upload operation");
            return;
        }
        
        Set<String> employeeIdsInBatch = new LinkedHashSet<>();
        List<String> employeeIdsToCheck = new ArrayList<>();
        
        // First pass: collect all primary employee IDs
        for (Deals deal : deals) {
            if (Boolean.TRUE.equals(deal.getIsPrimaryMember()) && deal.getEmployeeNumber() != null) {
                String employeeId = deal.getEmployeeNumber();
                
                // Check for duplicate in current batch
                if (employeeIdsInBatch.contains(employeeId)) {
                    validationErrors.add(String.format("Duplicate employee ID %s found in CSV file", employeeId));
                    continue;
                }
                employeeIdsInBatch.add(employeeId);
                employeeIdsToCheck.add(employeeId);
            }
        }
        
        // Batch check for existing employee IDs in database
        if (!employeeIdsToCheck.isEmpty()) {
            List<Deals> existingDeals = dealsRepository.findByEmployeeNumberInAndOrganizationId(
                employeeIdsToCheck, organization.getOrganizationId());
            
            Map<String, Deals> existingDealsMap = existingDeals.stream()
                .collect(Collectors.toMap(Deals::getEmployeeNumber, d -> d, (d1, d2) -> d1));
            
            for (String employeeId : employeeIdsToCheck) {
                if (existingDealsMap.containsKey(employeeId)) {
                    validationErrors.add(String.format("Employee ID %s already exists for organization %s", 
                        employeeId, organization.getOrganizationName()));
                }
            }
        }
    }
    
    /**
     * Validates CSV data for delete operation
     * Validates both primary employees and dependent deletions
     */
    private void validateForDelete(List<Deals> deals, Organization organization, 
                                   List<String> validationErrors, List<String> warnings) {
        if (organization == null) {
            validationErrors.add("Organization is required for delete operation");
            return;
        }
        
        Set<String> employeesToDelete = new LinkedHashSet<>();
        List<String> employeeIdsToCheck = new ArrayList<>();
        
        // Collect employee IDs to delete (primary employees)
        for (Deals deal : deals) {
            if (Boolean.TRUE.equals(deal.getIsPrimaryMember()) && deal.getEmployeeNumber() != null) {
                String employeeId = deal.getEmployeeNumber();
                employeesToDelete.add(employeeId);
                employeeIdsToCheck.add(employeeId);
            }
        }
        
        // Batch check for existing employees in database
        if (!employeeIdsToCheck.isEmpty()) {
            List<Deals> existingDeals = dealsRepository.findByEmployeeNumberInAndOrganizationId(
                employeeIdsToCheck, organization.getOrganizationId());
            
            Map<String, Deals> existingDealsMap = existingDeals.stream()
                .collect(Collectors.toMap(Deals::getEmployeeNumber, d -> d, (d1, d2) -> d1));
            
            // Validate primary employees exist
            for (String employeeId : employeesToDelete) {
                if (!existingDealsMap.containsKey(employeeId)) {
                    validationErrors.add(String.format("Employee ID %s not found in database for deletion", employeeId));
                }
            }
            
            // Validate dependent deletions
            for (Deals deal : deals) {
                if (Boolean.FALSE.equals(deal.getIsPrimaryMember()) && deal.getEmployeeNumber() != null) {
                    String employeeId = deal.getEmployeeNumber();
                    
                    // Skip if Self is being deleted (dependents will be deleted automatically)
                    if (employeesToDelete.contains(employeeId)) {
                        continue;
                    }
                    
                    // Check if this specific dependent is marked for individual deletion
                    if (deal.getStatus() == AccountStatus.INACTIVE) {
                        Deals primaryDeal = existingDealsMap.get(employeeId);
                        if (primaryDeal != null) {
                            List<Deals> existingDependents = dealsRepository.findByPrimaryIndividual(primaryDeal);
                            // Find matching dependent by relationship and name
                            boolean dependentFound = false;
                            for (Deals existingDependent : existingDependents) {
                                if (existingDependent.getRelationship() != null && 
                                    existingDependent.getRelationship().equalsIgnoreCase(deal.getRelationship()) &&
                                    existingDependent.getFirstName() != null &&
                                    existingDependent.getFirstName().equalsIgnoreCase(deal.getFirstName())) {
                                    dependentFound = true;
                                    break;
                                }
                            }
                            if (!dependentFound) {
                                validationErrors.add(String.format("Dependent %s (%s) not found for employee %s", 
                                    deal.getFirstName(), deal.getRelationship(), employeeId));
                            }
                        } else {
                            validationErrors.add(String.format("Employee ID %s not found for dependent deletion", employeeId));
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Validates file and organization (shared validation logic)
     */
    private Organization validateFileAndOrganization(MultipartFile file, UUID organizationId, 
                                                      List<String> errors) {
        // Validate file
        if (file == null || file.isEmpty()) {
            errors.add("File is empty or not provided");
            return null;
        }
        
        // Validate file type
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".CSV"))) {
            errors.add("Invalid file type. Only CSV files are allowed");
            return null;
        }
        
        // Validate and fetch organization
        if (organizationId == null) {
            errors.add("Organization ID is required");
            return null;
        }
        
        Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
        if (orgOpt.isEmpty()) {
            errors.add("Organization not found with ID: " + organizationId);
            return null;
        }
        
        return orgOpt.get();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "EMPLOYEE_UPLOAD", action = "BULK_CREATE")
    public ResponseEntity<ResponseDto<EmployeeUploadResponse>> uploadDealsFromCsv(MultipartFile file, UUID organizationId) {
        logger.info("[correlationId:{}] uploadDealsFromCsv called with organizationId: {}", MDC.get("correlationId"), organizationId);
        BaseResponse<EmployeeUploadResponse> responseObj = new BaseResponse<>();
        
        try {
            // Validate file and organization using shared validation logic
            List<String> fileErrors = new ArrayList<>();
            Organization organization = validateFileAndOrganization(file, organizationId, fileErrors);
            
            if (organization == null) {
                EmployeeUploadResponse errorResponse = new EmployeeUploadResponse(
                    0, 0, fileErrors.size(), 
                    fileErrors, 
                    "File upload failed",
                    0, 0
                );
                ResponseDto<EmployeeUploadResponse> response = new ResponseDto<>(
                    0, 
                    fileErrors.isEmpty() ? "Validation failed" : fileErrors.get(0), 
                    errorResponse
                );
                return responseObj.render(response);
            }
            
            logger.info("[correlationId:{}] Organization found: {}", MDC.get("correlationId"), organization.getOrganizationName());
            
            // Parse CSV file with grouped structure (employee_id with Self + dependents)
            CsvDealsReaderUtil.CsvParseResult parseResult = CsvDealsReaderUtil.parseGroupedCsvToDeals(file);
            
            if (parseResult.getDeals().isEmpty() && !parseResult.getErrors().isEmpty()) {
                // All rows failed to parse
                EmployeeUploadResponse errorResponse = new EmployeeUploadResponse(
                    parseResult.getTotalRows(), 
                    0, 
                    parseResult.getErrorCount(),
                    parseResult.getErrors(),
                    "Failed to parse any valid records from CSV",
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<EmployeeUploadResponse> response = new ResponseDto<>(0, "Failed to parse CSV file", errorResponse);
                logger.error("[correlationId:{}] Failed to parse CSV file: {}", MDC.get("correlationId"), response.getMessage());
                return responseObj.render(response);
            }
            
            // STANDARD VALIDATION: Validate ALL records first before processing
            List<String> validationErrors = new ArrayList<>(parseResult.getErrors());
            List<String> warnings = new ArrayList<>();
            
            // Use shared validation logic for upload - validates all records
            validateForUpload(parseResult.getDeals(), organization, validationErrors, warnings);
            
            // STANDARD PRACTICE: If ANY validation errors exist, return early without processing
            if (!validationErrors.isEmpty()) {
                logger.warn("[correlationId:{}] CSV validation failed with {} errors. Upload aborted.", 
                    MDC.get("correlationId"), validationErrors.size());
                
                EmployeeUploadResponse errorResponse = new EmployeeUploadResponse(
                    parseResult.getTotalRows(),
                    0,
                    validationErrors.size(),
                    validationErrors,
                    String.format("Validation failed. %d error(s) found. Please fix errors and try again.", validationErrors.size()),
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<EmployeeUploadResponse> response = new ResponseDto<>(
                    0, 
                    "CSV validation failed. Please fix errors and try again.", 
                    errorResponse
                );
                return responseObj.render(response);
            }
            
            // All validation passed - proceed with processing
            logger.info("[correlationId:{}] CSV validation passed. Proceeding with upload of {} records.", 
                MDC.get("correlationId"), parseResult.getDeals().size());
            
            // Prepare deals for batch save
            List<Deals> dealsToSave = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            // Set up all deals for saving
            for (Deals deal : parseResult.getDeals()) {
                // Set status to ACTIVE for all new additions
                deal.setStatus(AccountStatus.PENDING);
                
                // Set timestamps
                deal.setCreatedAt(now);
                deal.setUpdatedAt(now);
                
                // Set organization
                deal.setOrganization(organization);
                
                dealsToSave.add(deal);
            }
            
            // Save all deals in batches for better performance with large datasets
            // JPA batch insert is configured in application properties
            int batchSize = 500; // Process in batches of 500
            int totalSaved = 0;
            
            logger.info("[correlationId:{}] Saving {} new deals in batches of {}", 
                MDC.get("correlationId"), dealsToSave.size(), batchSize);
            
            // Save new deals
            for (int i = 0; i < dealsToSave.size(); i += batchSize) {
                int end = Math.min(i + batchSize, dealsToSave.size());
                List<Deals> batch = dealsToSave.subList(i, end);
                dealsRepository.saveAll(batch);
                totalSaved += batch.size();
                
                if (i + batchSize < dealsToSave.size()) {
                    logger.debug("[correlationId:{}] Saved batch: {}/{} deals", 
                        MDC.get("correlationId"), totalSaved, dealsToSave.size());
                }
            }
            
            logger.info("[correlationId:{}] Successfully saved {} deals in batches", 
                MDC.get("correlationId"), totalSaved);
            
            // Prepare success response
            String message = String.format("Successfully added %d records from CSV", totalSaved);
            EmployeeUploadResponse csvResponse = new EmployeeUploadResponse(
                parseResult.getTotalRows(),
                totalSaved,
                0,
                parseResult.getErrors(),
                message,
                parseResult.getTotalEmployees(),
                parseResult.getTotalDependents()
            );
            
            logger.info("[correlationId:{}] CSV upload completed: {} records saved successfully", 
                MDC.get("correlationId"), dealsToSave.size());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, csvResponse));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in uploadDealsFromCsv: {}", MDC.get("correlationId"), e.getMessage(), e);
            // Transaction will automatically rollback due to @Transactional annotation
            EmployeeUploadResponse errorResponse = new EmployeeUploadResponse(0, 0, 1, 
                List.of("Transaction rolled back: " + e.getMessage()), "File upload failed - no records were saved",
                0, 0);
            ResponseDto<EmployeeUploadResponse> response = new ResponseDto<>(0, e.getMessage(), errorResponse);
            return responseObj.render(response);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "EMPLOYEE_DELETE", action = "BULK_UPDATE")
    public ResponseEntity<ResponseDto<EmployeeUploadResponse>> deleteEmployeesFromCsv(MultipartFile file, UUID organizationId) {
        logger.info("[correlationId:{}] deleteEmployeesFromCsv called for organizationId: {}", 
            MDC.get("correlationId"), organizationId);
        BaseResponse<EmployeeUploadResponse> responseObj = new BaseResponse<>();
        
        try {
            // Validate file and organization using shared validation logic
            List<String> fileErrors = new ArrayList<>();
            Organization organization = validateFileAndOrganization(file, organizationId, fileErrors);
            
            if (organization == null) {
                EmployeeUploadResponse errorResponse = new EmployeeUploadResponse(
                    0, 0, fileErrors.size(), 
                    fileErrors, 
                    "File deletion failed",
                    0, 0
                );
                ResponseDto<EmployeeUploadResponse> response = new ResponseDto<>(
                    0, 
                    fileErrors.isEmpty() ? "Validation failed" : fileErrors.get(0), 
                    errorResponse
                );
                return responseObj.render(response);
            }
            
            logger.info("[correlationId:{}] Organization found: {}", MDC.get("correlationId"), organization.getOrganizationName());
            
            // Parse CSV file with grouped structure
            CsvDealsReaderUtil.CsvParseResult parseResult = CsvDealsReaderUtil.parseGroupedCsvToDeals(file);
            
            if (parseResult.getDeals().isEmpty() && !parseResult.getErrors().isEmpty()) {
                EmployeeUploadResponse errorResponse = new EmployeeUploadResponse(
                    parseResult.getTotalRows(), 
                    0, 
                    parseResult.getErrorCount(),
                    parseResult.getErrors(),
                    "Failed to parse any valid records from CSV",
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<EmployeeUploadResponse> response = new ResponseDto<>(0, "Failed to parse CSV file", errorResponse);
                logger.error("[correlationId:{}] Failed to parse CSV file: {}", MDC.get("correlationId"), response.getMessage());
                return responseObj.render(response);
            }
            
            // STANDARD VALIDATION: Validate ALL records first before processing
            List<String> validationErrors = new ArrayList<>(parseResult.getErrors());
            List<String> warnings = new ArrayList<>();
            
            // Use shared validation logic for delete - validates all records
            validateForDelete(parseResult.getDeals(), organization, validationErrors, warnings);
            
            // STANDARD PRACTICE: If ANY validation errors exist, return early without processing
            if (!validationErrors.isEmpty()) {
                logger.warn("[correlationId:{}] CSV validation failed with {} errors. Deletion aborted.", 
                    MDC.get("correlationId"), validationErrors.size());
                
                EmployeeUploadResponse errorResponse = new EmployeeUploadResponse(
                    parseResult.getTotalRows(),
                    0,
                    validationErrors.size(),
                    validationErrors,
                    String.format("Validation failed. %d error(s) found. Please fix errors and try again.", validationErrors.size()),
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<EmployeeUploadResponse> response = new ResponseDto<>(
                    0, 
                    "CSV validation failed. Please fix errors and try again.", 
                    errorResponse
                );
                return responseObj.render(response);
            }
            
            // All validation passed - proceed with processing deletions
            logger.info("[correlationId:{}] CSV validation passed. Proceeding with deletion of {} records.", 
                MDC.get("correlationId"), parseResult.getDeals().size());
            
            // Process deletions - collect deals to delete from database
            List<Deals> dealsToDelete = new ArrayList<>();
            
            // Track which employees (Self) are being deleted - their dependents will also be deleted
            Set<String> employeesToDelete = new LinkedHashSet<>();
            
            // First pass: identify deletions from CSV
            for (Deals deal : parseResult.getDeals()) {
                if (Boolean.TRUE.equals(deal.getIsPrimaryMember()) && deal.getEmployeeNumber() != null) {
                    employeesToDelete.add(deal.getEmployeeNumber());
                    logger.info("[correlationId:{}] Employee {} marked for deletion in CSV", 
                        MDC.get("correlationId"), deal.getEmployeeNumber());
                }
            }
            
            // Collect all employee IDs to check in database
            List<String> employeeIdsToCheck = new ArrayList<>();
            for (Deals deal : parseResult.getDeals()) {
                if (Boolean.TRUE.equals(deal.getIsPrimaryMember()) && deal.getEmployeeNumber() != null) {
                    employeeIdsToCheck.add(deal.getEmployeeNumber());
                }
            }
            
            // Batch check for existing employees in database
            if (!employeeIdsToCheck.isEmpty()) {
                logger.info("[correlationId:{}] Checking {} employee IDs for deletion in database", 
                    MDC.get("correlationId"), employeeIdsToCheck.size());
                
                List<Deals> existingDeals = dealsRepository.findByEmployeeNumberInAndOrganizationId(
                    employeeIdsToCheck, organization.getOrganizationId());
                
                Map<String, Deals> existingDealsMap = existingDeals.stream()
                    .collect(Collectors.toMap(Deals::getEmployeeNumber, d -> d, (d1, d2) -> d1));
                
                // Process Self deletions
                for (String employeeId : employeesToDelete) {
                    Deals existingDeal = existingDealsMap.get(employeeId);
                    if (existingDeal != null) {
                        // Self is being deleted - delete Self + ALL dependents
                        dealsToDelete.add(existingDeal);
                        
                        // Get ALL existing dependents to delete
                        List<Deals> existingDependents = dealsRepository.findByPrimaryIndividual(existingDeal);
                        dealsToDelete.addAll(existingDependents);
                        logger.info("[correlationId:{}] Employee {} and {} dependents will be deleted (Self deletion)", 
                            MDC.get("correlationId"), employeeId, existingDependents.size());
                    }
                }
                
                // Process individual dependent deletions (validation already passed)
                for (Deals deal : parseResult.getDeals()) {
                    if (Boolean.FALSE.equals(deal.getIsPrimaryMember()) && deal.getEmployeeNumber() != null) {
                        String employeeId = deal.getEmployeeNumber();
                        
                        // Skip if Self is being deleted (already handled above)
                        if (employeesToDelete.contains(employeeId)) {
                            continue;
                        }
                        
                        // Check if this specific dependent is marked for individual deletion
                        if (deal.getStatus() == AccountStatus.INACTIVE) {
                            Deals primaryDeal = existingDealsMap.get(employeeId);
                            if (primaryDeal != null) {
                                List<Deals> existingDependents = dealsRepository.findByPrimaryIndividual(primaryDeal);
                                // Find matching dependent by relationship and name (already validated)
                                for (Deals existingDependent : existingDependents) {
                                    if (existingDependent.getRelationship() != null && 
                                        existingDependent.getRelationship().equalsIgnoreCase(deal.getRelationship()) &&
                                        existingDependent.getFirstName() != null &&
                                        existingDependent.getFirstName().equalsIgnoreCase(deal.getFirstName())) {
                                        // Found matching dependent - add to deletion list
                                        dealsToDelete.add(existingDependent);
                                        logger.info("[correlationId:{}] Dependent {} ({}) will be deleted", 
                                            MDC.get("correlationId"), deal.getFirstName(), deal.getRelationship());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Delete all records from database in batches
            int batchSize = 500;
            int totalDeleted = 0;
            
            logger.info("[correlationId:{}] Deleting {} records from database in batches of {}", 
                MDC.get("correlationId"), dealsToDelete.size(), batchSize);
            
            for (int i = 0; i < dealsToDelete.size(); i += batchSize) {
                int end = Math.min(i + batchSize, dealsToDelete.size());
                List<Deals> batch = dealsToDelete.subList(i, end);
                batch.forEach(deal -> deal.setStatus(AccountStatus.PENDING_DELETE));
                dealsRepository.saveAll(batch);
                totalDeleted += batch.size();
                
                if (i + batchSize < dealsToDelete.size()) {
                    logger.debug("[correlationId:{}] Deleted batch: {}/{} records", 
                        MDC.get("correlationId"), totalDeleted, dealsToDelete.size());
                }
            }
            
            logger.info("[correlationId:{}] Successfully deleted {} records in batches", 
                MDC.get("correlationId"), totalDeleted);
            
            // Prepare success response
            String message = String.format("Successfully deleted %d records from CSV", totalDeleted);
            EmployeeUploadResponse csvResponse = new EmployeeUploadResponse(
                parseResult.getTotalRows(),
                totalDeleted,
                0,
                parseResult.getErrors(),
                message,
                parseResult.getTotalEmployees(),
                parseResult.getTotalDependents()
            );
            
            logger.info("[correlationId:{}] CSV deletion completed: {} records deleted successfully", 
                MDC.get("correlationId"), totalDeleted);
            
            return responseObj.render(responseObj.formSuccessResponse("CSV deletion completed successfully", csvResponse));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in deleteEmployeesFromCsv: {}", MDC.get("correlationId"), e.getMessage(), e);
            EmployeeUploadResponse errorResponse = new EmployeeUploadResponse(0, 0, 1, 
                List.of("Transaction rolled back: " + e.getMessage()), "File deletion failed - no records were deleted",
                0, 0);
            ResponseDto<EmployeeUploadResponse> response = new ResponseDto<>(0, e.getMessage(), errorResponse);
            return responseObj.render(response);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "EMPLOYEE", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> deleteEmployee(String employeeId, UUID organizationId) {
        logger.info("[correlationId:{}] deleteEmployee called for employeeId: {}, organizationId: {}", 
            MDC.get("correlationId"), employeeId, organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Validate organization
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            Organization organization = orgOpt.get();
            
            // Find employee by employee number
            Optional<Deals> employeeOpt = dealsRepository.findByEmployeeNumberAndOrganizationId(
                employeeId, organizationId);
            
            if (employeeOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(String.format("Employee with ID %s not found for organization %s", employeeId, organization.getOrganizationName())));
            }
            
            Deals employee = employeeOpt.get();
            
            // Mark employee as INACTIVE
            employee.setStatus(AccountStatus.INACTIVE);
            employee.setUpdatedAt(LocalDateTime.now());
            dealsRepository.save(employee);
            
            // Mark all dependents as INACTIVE
            List<Deals> dependents = dealsRepository.findByPrimaryIndividual(employee);
            for (Deals dependent : dependents) {
                dependent.setStatus(AccountStatus.INACTIVE);
                dependent.setUpdatedAt(LocalDateTime.now());
            }
            if (!dependents.isEmpty()) {
                dealsRepository.saveAll(dependents);
                logger.info("[correlationId:{}] Marked employee {} and {} dependents as INACTIVE", 
                    MDC.get("correlationId"), employeeId, dependents.size());
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, 
                String.format("Employee %s and %d dependents marked as deleted", employeeId, dependents.size())));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in deleteEmployee: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "EMPLOYEE", action = "BULK_DELETE")
    public ResponseEntity<ResponseDto<String>> bulkDeleteEmployees(BulkEmployeeDeletionRequestDto requestDto, UUID organizationId) {
        logger.info("[correlationId:{}] bulkDeleteEmployees called for {} employees, organizationId: {}", 
            MDC.get("correlationId"), requestDto.getEmployeeId() != null ? 1 : 0, organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Validate request
            if (requestDto == null || requestDto.getEmployeeId() == null) {
                return responseObj.render(responseObj.formErrorResponse("Employee IDs list cannot be empty"));
            }
            
            // Validate organization
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            
            List<String> employeeIds = Arrays.asList(requestDto.getEmployeeId());
            LocalDateTime now = LocalDateTime.now();
            
            // Batch query to find all employees at once (optimized)
            List<Deals> employees = dealsRepository.findByEmployeeNumberInAndOrganizationId(
                employeeIds, organizationId);
            
            if (employees.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No employees found with the provided employee IDs for this organization"));
            }
            
            // Create a map for fast lookup
            Map<String, Deals> employeesMap = employees.stream()
                .collect(Collectors.toMap(Deals::getEmployeeNumber, d -> d, (d1, d2) -> d1));
            
            // Track results
            int deletedCount = 0;
            int dependentsDeletedCount = 0;
            List<String> notFoundIds = new ArrayList<>();
            List<Deals> allDealsToUpdate = new ArrayList<>();
            
            // Process each employee ID
            for (String employeeId : employeeIds) {
                Deals employee = employeesMap.get(employeeId);
                
                if (employee == null) {
                    notFoundIds.add(employeeId);
                    continue;
                }
                
                // Mark employee as INACTIVE
                employee.setStatus(AccountStatus.INACTIVE);
                employee.setUpdatedAt(now);
                allDealsToUpdate.add(employee);
                deletedCount++;
                
                // Mark all dependents as INACTIVE
                List<Deals> dependents = dealsRepository.findByPrimaryIndividual(employee);
                for (Deals dependent : dependents) {
                    dependent.setStatus(AccountStatus.INACTIVE);
                    dependent.setUpdatedAt(now);
                    allDealsToUpdate.add(dependent);
                    dependentsDeletedCount++;
                }
            }
            
            // Batch update all deals (employees + dependents)
            if (!allDealsToUpdate.isEmpty()) {
                int batchSize = 500;
                for (int i = 0; i < allDealsToUpdate.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, allDealsToUpdate.size());
                    List<Deals> batch = allDealsToUpdate.subList(i, end);
                    dealsRepository.saveAll(batch);
                }
            }
            
            // Prepare response message
            StringBuilder message = new StringBuilder();
            message.append(String.format("Successfully deleted %d employees and %d dependents", 
                deletedCount, dependentsDeletedCount));
            
            if (!notFoundIds.isEmpty()) {
                message.append(String.format(". %d employee ID(s) not found: %s", 
                    notFoundIds.size(), String.join(", ", notFoundIds)));
            }
            
            logger.info("[correlationId:{}] Bulk deletion completed: {} employees, {} dependents deleted. {} not found", 
                MDC.get("correlationId"), deletedCount, dependentsDeletedCount, notFoundIds.size());
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, message.toString()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in bulkDeleteEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<String>> manualDeleteEmployees(UUID organizationId, ManualDeleteEmployeesRequestDto requestDto) {
        logger.info("[correlationId:{}] manualDeleteEmployees called for {} employees, organizationId: {}",
            MDC.get("correlationId"), requestDto != null && requestDto.getEmployeeIds() != null ? requestDto.getEmployeeIds().size() : 0, organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();

        try {
            if (requestDto == null || requestDto.getEmployeeIds() == null || requestDto.getEmployeeIds().isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Employee IDs list cannot be empty"));
            }

            jwtUserExtractor.validateOrganizationAccess(organizationId);
            Organization organization = organizationRepository.findByOrganizationId(organizationId).orElseThrow(() -> new RuntimeException("Organization not found"));
            String username = jwtUserExtractor.getCurrentUsername();
            AdminUser adminUser = adminUserRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Admin user not found"));

            EmployeeUploadResponse result = employeeService.deleteEmployeeManual(requestDto.getEmployeeIds(), organization, adminUser);

            if (result.getSuccessCount() == 0) {
                String errorMsg = result.getMessage() != null ? result.getMessage() : "No employees were submitted for deletion";
                if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                    errorMsg = String.join("; ", result.getErrors());
                }
                return responseObj.render(responseObj.formErrorResponse(errorMsg));
            }

            String message = result.getMessage() != null ? result.getMessage() : Constants.SUCCESS;
            logger.info("[correlationId:{}] Manual deletion (endorsement) completed: {} employees, {} dependents",
                MDC.get("correlationId"), result.getTotalEmployees(), result.getTotalDependents());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, message));

        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied for organizationId: {}", MDC.get("correlationId"), organizationId);
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in manualDeleteEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<EmployeeUploadResponse>> uploadEmployees(List<EmployeeUploadDto> employeeUploadDtoList, UUID organizationId, String uploadType, MultipartFile file, List<Long> policyIds) {
    logger.info("[correlationId:{}] uploadEmployees called for {} employees, organizationId: {}", 
        MDC.get("correlationId"), employeeUploadDtoList.size(), organizationId);
    BaseResponse<EmployeeUploadResponse> responseObj = new BaseResponse<>();
    try {
        if (policyIds == null || policyIds.isEmpty()) {
            return responseObj.render(responseObj.formErrorResponse(400, "At least one policy must be selected"));
        }
        com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setActionSource(com.vimainsurance.vimaadmin.audit.ActionSource.BULK);
        Organization organization = organizationRepository.findByOrganizationId(organizationId).orElseThrow(() -> new RuntimeException("Organization not found"));
        EmployeeUploadResponse employeeUploadResponse = new EmployeeUploadResponse();
        AdminUser adminuser = null;
            String username = jwtUserExtractor.getCurrentUsername();
            adminuser = adminUserRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Admin user not found"));
        com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setCurrentUserId(adminuser != null ? adminuser.getId() : null);
        employeeUploadResponse = employeeService.uploadEmployees(employeeUploadDtoList, organization, adminuser, file, uploadType, policyIds);
        String responseMessage = (employeeUploadResponse.getMessage() != null && !employeeUploadResponse.getMessage().isEmpty())
                ? employeeUploadResponse.getMessage() : Constants.SUCCESS;
        return responseObj.render(responseObj.formSuccessResponse(responseMessage, employeeUploadResponse));
    } catch (Exception e) {
        logger.error("[correlationId:{}] Exception in uploadEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
        return responseObj.render(responseObj.formErrorResponse("Error Occured while uploading employees"));
    }
    finally {
        com.vimainsurance.vimaadmin.audit.AuditContextSupplier.clearCurrentUserId();
        com.vimainsurance.vimaadmin.audit.AuditContextSupplier.clearActionSource();
    }
}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<EmployeeUploadResponse>> manualAddEmployees(UUID organizationId, ManualAddEmployeesRequestDto requestDto) {
        logger.info("[correlationId:{}] manualAddEmployees called for organizationId: {}", MDC.get("correlationId"), organizationId);
        BaseResponse<EmployeeUploadResponse> responseObj = new BaseResponse<>();
        try {
            jwtUserExtractor.validateOrganizationAccess(organizationId);
            Organization organization = organizationRepository.findByOrganizationId(organizationId).orElseThrow(() -> new RuntimeException("Organization not found"));
            AdminUser adminUser = adminUserRepository.findByUsername(jwtUserExtractor.getCurrentUsername()).orElseThrow(() -> new RuntimeException("Admin user not found"));
            List<EmployeeUploadDto> employees = requestDto != null && requestDto.getEmployees() != null ? requestDto.getEmployees() : List.of();
            if (employees.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("At least one employee is required"));
            }
            List<Long> policyIds = requestDto.getPolicyIds();
            if (policyIds == null || policyIds.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(400, "At least one policy must be selected"));
            }
            EmployeeUploadResponse result = employeeService.manualAddEmployees(employees, organization, adminUser, policyIds);
            String responseMessage = result.getMessage() != null && !result.getMessage().isEmpty() ? result.getMessage() : Constants.SUCCESS;
            return responseObj.render(responseObj.formSuccessResponse(responseMessage, result));
        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied for organizationId: {}", MDC.get("correlationId"), organizationId);
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in manualAddEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error occurred while adding employees"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<EmployeeUploadResponse>> validateEmployees(List<EmployeeUploadDto> employeeUploadDtoList, UUID organizationId) {
        logger.info("[correlationId:{}] validateEmployees called for {} employees, organizationId: {}", 
            MDC.get("correlationId"), employeeUploadDtoList.size(), organizationId);
        BaseResponse<EmployeeUploadResponse> responseObj = new BaseResponse<>();
        try {
            Organization organization = organizationRepository.findByOrganizationId(organizationId).orElseThrow(() -> new RuntimeException("Organization not found"));
            EmployeeUploadResponse employeeUploadResponse = employeeService.validateEmployee(employeeUploadDtoList, organization);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, employeeUploadResponse));
        }
        catch (Exception e) {
            logger.error("[correlationId:{}] Exception in validateEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error Occured while validating employees"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "EMPLOYEE", action = "BULK_DELETE")
    public ResponseEntity<ResponseDto<EmployeeUploadResponse>> delete(List<BulkEmployeeDeletionRequestDto> bulkEmployeeDeletionRequestDtoList, UUID organizationId, String uploadType, MultipartFile file) {
        logger.info("[correlationId:{}] delete called for {} employees, organizationId: {}",
            MDC.get("correlationId"), bulkEmployeeDeletionRequestDtoList.size(), organizationId);
        BaseResponse<EmployeeUploadResponse> responseObj = new BaseResponse<>();
        try {
            Organization organization = organizationRepository.findByOrganizationId(organizationId).orElseThrow(() -> new RuntimeException("Organization not found"));
            AdminUser adminuser = adminUserRepository.findByUsername(jwtUserExtractor.getCurrentUsername()).orElseThrow(() -> new RuntimeException("Admin user not found"));
            EmployeeUploadResponse employeeUploadResponse = employeeService.deleteEmployee(bulkEmployeeDeletionRequestDtoList, organization, adminuser, file, uploadType);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, employeeUploadResponse));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in delete: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error occurred while deleting employees"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployeesByEndorsementId(UUID endorsementId, int page, int rec) {
        logger.info("[correlationId:{}] getEmployeesByEndorsementId called for endorsementId: {}, page: {}, rec: {}", 
            MDC.get("correlationId"), endorsementId, page, rec);
        BaseResponse<List<OrganizationEmployeeDto>> responseObj = new BaseResponse<>();
        try {
            if(page == -1 && rec == -1) {
                List<DealEndorsement> dealEndorsements = dealEndorsementRepository.findByEndorsement_EndorsementId(endorsementId);
                List<Deals> employees = dealEndorsements.stream()
                    .map(DealEndorsement::getDeal)
                    .collect(Collectors.toList());
                List<OrganizationEmployeeDto> employeeDtos = employees.stream()
                    .map(this::mapToOrganizationEmployeeDto)
                    .collect(Collectors.toList());
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, employeeDtos, employees.size()));
            }
            Page<DealEndorsement> dealEndorsementsPage = dealEndorsementRepository.findByEndorsement_EndorsementId(endorsementId, PageRequest.of(page, rec));
            List<Deals> employees = dealEndorsementsPage.getContent().stream()
                .map(DealEndorsement::getDeal)
                .collect(Collectors.toList());
            List<OrganizationEmployeeDto> employeeDtos = employees.stream()
                .map(this::mapToOrganizationEmployeeDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, employeeDtos, dealEndorsementsPage.getTotalElements()));
        }
        catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getEmployeesByEndorsementId: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Error Occured while getting employees by endorsement id"));
        }
    }
  
}