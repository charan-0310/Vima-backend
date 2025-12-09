package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.BulkEmployeeDeletionRequestDto;
import com.vimainsurance.vimaadmin.dto.CsvUploadResponseDto;
import com.vimainsurance.vimaadmin.dto.CsvValidationResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.OrganizationEmployeeDto;
import com.vimainsurance.vimaadmin.dto.OrganizationRequestDto;
import com.vimainsurance.vimaadmin.dto.OrganizationResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.Industry;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IOrganizationService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.CsvDealsReaderUtil;

@Service
public class OrganizationServiceImpl implements IOrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);

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

    @Override
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
            organizationRepository.save(org);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization create: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> update(OrganizationRequestDto requestDto) {
        logger.info("[correlationId:{}] Organization update called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (requestDto.getOrganizationId() == null) {
                return responseObj.render(responseObj.formErrorResponse("Organization ID is required"));
            }
            Optional<Organization> opt = organizationRepository.findById(requestDto.getOrganizationId());
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            Organization org = opt.get();
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
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Organization update: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
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
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
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
            
            Page<Organization> organizationPage;
            
            // Determine which query method to use based on search and status filters
            if (search != null && !search.trim().isEmpty() && status != null && !status.trim().isEmpty()) {
                // Both search and status filter
                organizationPage = organizationRepository.searchOrganizationsByStatus(status, search, pageRequest);
            } else if (search != null && !search.trim().isEmpty()) {
                // Only search filter
                organizationPage = organizationRepository.searchOrganizations(search, pageRequest);
            } else if (status != null && !status.trim().isEmpty()) {
                // Only status filter
                organizationPage = organizationRepository.findAllByStatusWithPagination(status, pageRequest);
            } else {
                // No filters - get all
                organizationPage = organizationRepository.findAll(pageRequest);
            }

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
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
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
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> uploadDocument(DocumentRequestDto requestDto, UUID organizationId) {
        logger.info("[correlationId:{}] uploadDocument called for organization {}", MDC.get("correlationId"), organizationId);
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
            if(responseBody != null && responseBody.getErrorCode() != null){
                return responseObj.render(responseObj.formErrorResponse(responseBody.getMessage()));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document uploaded successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in uploadDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<Resource> downloadDocument(String documentId) {
        logger.info("[correlationId:{}] downloadDocument called for document {}", MDC.get("correlationId"), documentId);
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(UUID.fromString(documentId));
            if(documentOpt.isEmpty()){
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
    public ResponseEntity<ResponseDto<String>> deleteDocument(String documentId) {
        logger.info("[correlationId:{}] deleteDocument called for document {}", MDC.get("correlationId"), documentId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(UUID.fromString(documentId));
            if(documentOpt.isEmpty()){
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            Document document = documentOpt.get();
            s3Service.deleteFile(document.getS3Key());
            // Handle special case for PAN/AADHAAR cards that have original and masked versions
            if(document.getDocumentType().equals(DocumentType.PAN_CARD) || document.getDocumentType().equals(DocumentType.AADHAAR_CARD)){
                s3Service.deleteFile(document.getS3Key().replace("masked", "original"));
            }
            documentRepository.delete(document);
            logger.info("[correlationId:{}] Document deleted successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document deleted successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in deleteDocument: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
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
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<OrganizationEmployeeDto>>> getEmployees(UUID organizationId) {
        logger.info("[correlationId:{}] getEmployees called for organization {}", MDC.get("correlationId"), organizationId);
        BaseResponse<List<OrganizationEmployeeDto>> responseObj = new BaseResponse<>();
        try {
            List<Deals> dealsList = dealsRepository.findByOrganizationId(organizationId);
            List<OrganizationEmployeeDto> responseDto = dealsList.stream()
                .filter(deal -> Boolean.TRUE.equals(deal.getIsPrimaryMember()))
                .map(this::mapToOrganizationEmployeeDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto, responseDto.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<ResponseDto<OrganizationEmployeeDto>> getEmployee(UUID individualId, UUID organizationId) {
        logger.info("[correlationId:{}] getEmployee called for individualId: {}, organizationId: {}", 
            MDC.get("correlationId"), individualId.toString(), organizationId);
        BaseResponse<OrganizationEmployeeDto> responseObj = new BaseResponse<>();
        try {
            // Validate organization
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            
            // Find employee by employee number and organization
            Optional<Deals> employeeOpt = dealsRepository.findByIndividualIdAndOrganizationId(individualId, organizationId);
            
            if (employeeOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(
                    String.format("Employee individual not found for organization")));
            }
            
            Deals employee = employeeOpt.get();
            
            // Ensure it's a primary member (employee)
            if (!Boolean.TRUE.equals(employee.getIsPrimaryMember())) {
                return responseObj.render(responseObj.formErrorResponse(
                    String.format("Unauthorized access")));
            }
            
            OrganizationEmployeeDto responseDto = mapToOrganizationEmployeeDto(employee);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getEmployee: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
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
                return responseObj.render(responseObj.formErrorResponse(
                    String.format("Employee individual not found for organization")));
            }
            
            Deals employee = employeeOpt.get();
            
            // Ensure it's a primary member (employee)
            if (!Boolean.TRUE.equals(employee.getIsPrimaryMember())) {
                return responseObj.render(responseObj.formErrorResponse(
                    String.format("Unauthorized access")));
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
        dto.setOrganizationName(deal.getOrganization().getOrganizationName());
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
    public ResponseEntity<ResponseDto<CsvUploadResponseDto>> uploadDealsFromCsv(MultipartFile file, UUID organizationId) {
        logger.info("[correlationId:{}] uploadDealsFromCsv called with organizationId: {}", MDC.get("correlationId"), organizationId);
        BaseResponse<CsvUploadResponseDto> responseObj = new BaseResponse<>();
        
        try {
            // Validate file and organization using shared validation logic
            List<String> fileErrors = new ArrayList<>();
            Organization organization = validateFileAndOrganization(file, organizationId, fileErrors);
            
            if (organization == null) {
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(
                    0, 0, fileErrors.size(), 
                    fileErrors, 
                    "File upload failed",
                    0, 0
                );
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(
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
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(
                    parseResult.getTotalRows(), 
                    0, 
                    parseResult.getErrorCount(),
                    parseResult.getErrors(),
                    "Failed to parse any valid records from CSV",
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, "Failed to parse CSV file", errorResponse);
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
                
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(
                    parseResult.getTotalRows(),
                    0,
                    validationErrors.size(),
                    validationErrors,
                    String.format("Validation failed. %d error(s) found. Please fix errors and try again.", validationErrors.size()),
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(
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
            CsvUploadResponseDto csvResponse = new CsvUploadResponseDto(
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
            CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(0, 0, 1, 
                List.of("Transaction rolled back: " + e.getMessage()), "File upload failed - no records were saved",
                0, 0);
            ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, e.getMessage(), errorResponse);
            return responseObj.render(response);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<CsvUploadResponseDto>> deleteEmployeesFromCsv(MultipartFile file, UUID organizationId) {
        logger.info("[correlationId:{}] deleteEmployeesFromCsv called for organizationId: {}", 
            MDC.get("correlationId"), organizationId);
        BaseResponse<CsvUploadResponseDto> responseObj = new BaseResponse<>();
        
        try {
            // Validate file and organization using shared validation logic
            List<String> fileErrors = new ArrayList<>();
            Organization organization = validateFileAndOrganization(file, organizationId, fileErrors);
            
            if (organization == null) {
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(
                    0, 0, fileErrors.size(), 
                    fileErrors, 
                    "File deletion failed",
                    0, 0
                );
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(
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
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(
                    parseResult.getTotalRows(), 
                    0, 
                    parseResult.getErrorCount(),
                    parseResult.getErrors(),
                    "Failed to parse any valid records from CSV",
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, "Failed to parse CSV file", errorResponse);
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
                
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(
                    parseResult.getTotalRows(),
                    0,
                    validationErrors.size(),
                    validationErrors,
                    String.format("Validation failed. %d error(s) found. Please fix errors and try again.", validationErrors.size()),
                    parseResult.getTotalEmployees(),
                    parseResult.getTotalDependents()
                );
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(
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
            CsvUploadResponseDto csvResponse = new CsvUploadResponseDto(
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
            CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(0, 0, 1, 
                List.of("Transaction rolled back: " + e.getMessage()), "File deletion failed - no records were deleted",
                0, 0);
            ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, e.getMessage(), errorResponse);
            return responseObj.render(response);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
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
                return responseObj.render(responseObj.formErrorResponse(
                    String.format("Employee with ID %s not found for organization %s", employeeId, organization.getOrganizationName())));
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
    public ResponseEntity<ResponseDto<String>> bulkDeleteEmployees(BulkEmployeeDeletionRequestDto requestDto, UUID organizationId) {
        logger.info("[correlationId:{}] bulkDeleteEmployees called for {} employees, organizationId: {}", 
            MDC.get("correlationId"), requestDto.getEmployeeIds() != null ? requestDto.getEmployeeIds().size() : 0, organizationId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Validate request
            if (requestDto == null || requestDto.getEmployeeIds() == null || requestDto.getEmployeeIds().isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Employee IDs list cannot be empty"));
            }
            
            // Validate organization
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            
            List<String> employeeIds = requestDto.getEmployeeIds();
            LocalDateTime now = LocalDateTime.now();
            
            // Batch query to find all employees at once (optimized)
            List<Deals> employees = dealsRepository.findByEmployeeNumberInAndOrganizationId(
                employeeIds, organizationId);
            
            if (employees.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(
                    "No employees found with the provided employee IDs for this organization"));
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
}


