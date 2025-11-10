package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
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
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.Industry;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IOrganizationService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.CsvDealsReaderUtil;
import com.vimainsurance.vimaadmin.dto.CsvUploadResponseDto;
import java.time.LocalDateTime;

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
            if (page == -1 && rec == -1) {
                List<Organization> orgList = organizationRepository.findAll();
                List<OrganizationResponseDto> out = new ArrayList<>();
                for (Organization org : orgList) {
                    out.add(mapToResponseDto(org));
                }
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
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
                .map(this::mapToOrganizationEmployeeDto)
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, responseDto, responseDto.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
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

    @Override
    public ResponseEntity<ResponseDto<CsvUploadResponseDto>> uploadDealsFromCsv(MultipartFile file, UUID organizationId) {
        logger.info("[correlationId:{}] uploadDealsFromCsv called with organizationId: {}", MDC.get("correlationId"), organizationId);
        BaseResponse<CsvUploadResponseDto> responseObj = new BaseResponse<>();
        
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(0, 0, 1, 
                    List.of("File is empty or not provided"), "File upload failed");
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, "File is empty or not provided", errorResponse);
                return responseObj.render(response);
            }
            
            // Validate file type
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".CSV"))) {
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(0, 0, 1, 
                    List.of("Invalid file type. Only CSV files are allowed"), "File upload failed");
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, "Invalid file type. Only CSV files are allowed", errorResponse);
                return responseObj.render(response);
            }
            
            // Validate and fetch organization if provided
            Organization organization = null;
            if (organizationId != null) {
                Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
                if (orgOpt.isEmpty()) {
                    CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(0, 0, 1, 
                        List.of("Organization not found with ID: " + organizationId), "File upload failed");
                    ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, "Organization not found", errorResponse);
                    return responseObj.render(response);
                }
                organization = orgOpt.get();
                logger.info("[correlationId:{}] Organization found: {}", MDC.get("correlationId"), organization.getOrganizationName());
            }
            
            // Parse CSV file
            CsvDealsReaderUtil.CsvParseResult parseResult = CsvDealsReaderUtil.parseCsvToDeals(file);
            
            if (parseResult.getDeals().isEmpty() && !parseResult.getErrors().isEmpty()) {
                // All rows failed to parse
                CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(
                    parseResult.getTotalRows(), 
                    0, 
                    parseResult.getErrorCount(),
                    parseResult.getErrors(),
                    "Failed to parse any valid records from CSV"
                );
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, "Failed to parse CSV file", errorResponse);
                return responseObj.render(response);
            }
            
            // Save deals to database
            int savedCount = 0;
            List<String> saveErrors = new ArrayList<>(parseResult.getErrors());
            
            for (Deals deal : parseResult.getDeals()) {
                try {
                    // Set timestamps
                    LocalDateTime now = LocalDateTime.now();
                    deal.setCreatedAt(now);
                    deal.setUpdatedAt(now);
                    
                    // Set organization if provided
                    if (organization != null) {
                        deal.setOrganization(organization);
                    }
                    
                    // Check if employee number already exists
                    Optional<Deals> existingDeal = dealsRepository.findByEmployeeNumber(deal.getEmployeeNumber());
                    if (existingDeal.isPresent()) {
                        saveErrors.add(String.format("Employee ID %s already exists", deal.getEmployeeNumber()));
                        continue;
                    }
                    
                    // Check if email already exists (if provided)
                    if (deal.getEmail() != null && !deal.getEmail().trim().isEmpty()) {
                        Optional<Deals> existingByEmail = dealsRepository.findByEmail(deal.getEmail());
                        if (existingByEmail.isPresent()) {
                            saveErrors.add(String.format("Email %s already exists for Employee ID %s", 
                                deal.getEmail(), deal.getEmployeeNumber()));
                            continue;
                        }
                    }
                    
                    dealsRepository.save(deal);
                    savedCount++;
                    
                } catch (Exception e) {
                    String errorMsg = String.format("Failed to save Employee ID %s: %s", 
                        deal.getEmployeeNumber(), e.getMessage());
                    saveErrors.add(errorMsg);
                    logger.error("Error saving deal for Employee ID {}: {}", deal.getEmployeeNumber(), e.getMessage(), e);
                }
            }
            
            // Prepare response
            String message;
            if (savedCount == parseResult.getDeals().size()) {
                message = String.format("Successfully uploaded %d records from CSV", savedCount);
            } else {
                message = String.format("Uploaded %d out of %d valid records. %d errors occurred.", 
                    savedCount, parseResult.getDeals().size(), saveErrors.size());
            }
            
            CsvUploadResponseDto csvResponse = new CsvUploadResponseDto(
                parseResult.getTotalRows(),
                savedCount,
                saveErrors.size(),
                saveErrors,
                message
            );
            
            if (savedCount > 0) {
                logger.info("[correlationId:{}] CSV upload completed: {} records saved, {} errors", 
                    MDC.get("correlationId"), savedCount, saveErrors.size());
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, csvResponse));
            } else {
                logger.warn("[correlationId:{}] CSV upload completed with no records saved", MDC.get("correlationId"));
                ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, "No records were saved", csvResponse);
                return responseObj.render(response);
            }
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in uploadDealsFromCsv: {}", MDC.get("correlationId"), e.getMessage(), e);
            CsvUploadResponseDto errorResponse = new CsvUploadResponseDto(0, 0, 1, 
                List.of("Unexpected error: " + e.getMessage()), "File upload failed");
            ResponseDto<CsvUploadResponseDto> response = new ResponseDto<>(0, e.getMessage(), errorResponse);
            return responseObj.render(response);
        }
    }
}


