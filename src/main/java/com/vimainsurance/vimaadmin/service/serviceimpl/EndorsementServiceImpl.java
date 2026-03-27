package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.EmployeeOnboardingRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeeOnboardingResponseDto;
import com.vimainsurance.vimaadmin.dto.EndorsementRequestDto;
import com.vimainsurance.vimaadmin.dto.EndorsementResponseDto;
import com.vimainsurance.vimaadmin.dto.HealthIdUploadDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.DealEndorsement;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import com.vimainsurance.vimaadmin.mapper.EndorsementMapper;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.service.ICdBalanceService;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;
import com.vimainsurance.vimaadmin.service.IEndorsementService;
import com.vimainsurance.vimaadmin.service.ILifeEventEndorsementService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.specification.EndorsementSpecification;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.EnvironmentUtil;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.KeyCloakUtil;
import com.vimainsurance.vimaadmin.util.OrganizationAccessHelper;
import com.vimainsurance.vimaadmin.util.PasswordGenerator;
import com.vimainsurance.vimaadmin.util.TenantContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EndorsementServiceImpl implements IEndorsementService {

    private static final Logger logger = LoggerFactory.getLogger(EndorsementServiceImpl.class);

    @Autowired
    private IEndorsementRepository endorsementRepository;

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Autowired
    private IDocumentRepository documentRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IDealEndorsementRepository dealEndorsementRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private IDocumentService documentService;

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Autowired(required = false)
    private OrganizationAccessHelper organizationAccessHelper;

    @Autowired
    private IS3Service s3Service;

    @Autowired
    private KeyCloakUtil keycloakUtil;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private IEmployeePolicyMapService employeePolicyMapService;

    @Autowired(required = false)
    private ILifeEventEndorsementService lifeEventEndorsementService;

    @Autowired(required = false)
    private ICdBalanceService cdBalanceService;

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "CREATE")
    public ResponseEntity<ResponseDto<String>> create(EndorsementRequestDto requestDto) {
        logger.info("[correlationId:{}] Endorsement create called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (requestDto.getOrganizationId() != null) {
                if (organizationAccessHelper != null) {
                    organizationAccessHelper.validateAndSetContext(requestDto.getOrganizationId());
                }
            }
            Optional<Organization> orgOpt = organizationRepository.findById(requestDto.getOrganizationId());
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            Organization organization = orgOpt.get();

            // Validate document if provided
            Document document = null;
            if (requestDto.getDocumentId() != null) {
                Optional<Document> docOpt = documentRepository.findById(requestDto.getDocumentId());
                if (docOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Document not found"));
                }
                document = docOpt.get();
            }

        

            AdminUser uploadedBy = null;
            if (requestDto.getUploadedBy() != null) {
                Optional<AdminUser> uploadedByOpt = adminUserRepository.findById(requestDto.getUploadedBy());
                if (uploadedByOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Uploaded by user not found"));
                }
                uploadedBy = uploadedByOpt.get();
            }

            // Map DTO to entity
            Endorsement endorsement = EndorsementMapper.mapToEntity(requestDto, organization, document, uploadedBy);
            endorsementRepository.save(endorsement);

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.SAVE_SUCCESS));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid enum value in Endorsement create: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Invalid status value: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement create: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_CREATED));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> update(EndorsementRequestDto requestDto) {
        logger.info("[correlationId:{}] Endorsement update called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (requestDto.getEndorsementId() == null) {
                return responseObj.render(responseObj.formErrorResponse("Endorsement ID is required"));
            }

            Optional<Endorsement> opt = endorsementRepository.findById(requestDto.getEndorsementId());
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            UUID orgId = opt.get().getOrganization().getOrganizationId();
            if (organizationAccessHelper != null) {
                organizationAccessHelper.validateAndSetContext(orgId);
            } else if (jwtUserExtractor != null) {
                jwtUserExtractor.validateOrganizationAccess(orgId);
                com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setOrganizationId(orgId);
            }
            Endorsement endorsement = opt.get();

            // Validate organization if provided
            Organization organization = endorsement.getOrganization();
            if (requestDto.getOrganizationId() != null && !requestDto.getOrganizationId().equals(endorsement.getOrganization().getOrganizationId())) {
                Optional<Organization> orgOpt = organizationRepository.findById(requestDto.getOrganizationId());
                if (orgOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Organization not found"));
                }
                organization = orgOpt.get();
            }

            // Validate document if provided
            Document document = endorsement.getDocument();
            if (requestDto.getDocumentId() != null) {
                if (endorsement.getDocument() == null || !requestDto.getDocumentId().equals(endorsement.getDocument().getDocumentId())) {
                    Optional<Document> docOpt = documentRepository.findById(requestDto.getDocumentId());
                    if (docOpt.isEmpty()) {
                        return responseObj.render(responseObj.formErrorResponse("Document not found"));
                    }
                    document = docOpt.get();
                }
            }

            // Fetch AdminUser entities if provided
            String approvedBy = endorsement.getApprovedBy();
            if (requestDto.getApprovedBy() != null) {
                approvedBy = requestDto.getApprovedBy();
            }


            AdminUser uploadedBy = endorsement.getUploadedBy();
            if (requestDto.getUploadedBy() != null) {
                Optional<AdminUser> uploadedByOpt = adminUserRepository.findById(requestDto.getUploadedBy());
                if (uploadedByOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Uploaded by user not found"));
                }
                uploadedBy = uploadedByOpt.get();
            }
            try {
                AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(endorsement));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Could not serialize endorsement for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
            }
            // Update entity from DTO
            EndorsementMapper.updateEntityFromDto(endorsement, requestDto, organization, document, uploadedBy);
            endorsementRepository.save(endorsement);
            com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setNewSnapshotEntity(endorsement);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid enum value in Endorsement update: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Invalid enum value: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement update: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> delete(UUID endorsementId) {
        logger.info("[correlationId:{}] Endorsement delete called for {}", MDC.get("correlationId"), endorsementId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            endorsementRepository.delete(opt.get());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement delete: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.DELETE_FAILED));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<EndorsementResponseDto>> getById(UUID endorsementId) {
        logger.info("[correlationId:{}] Endorsement getById called for {}", MDC.get("correlationId"), endorsementId);
        BaseResponse<EndorsementResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            UUID orgId = opt.get().getOrganization().getOrganizationId();
            if (organizationAccessHelper != null) {
                organizationAccessHelper.validateAndSetContext(orgId);
            } else if (jwtUserExtractor != null) {
                jwtUserExtractor.validateOrganizationAccess(orgId);
                com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setOrganizationId(orgId);
            }
            EndorsementResponseDto dto = EndorsementMapper.mapToResponseDto(opt.get());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement getById: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getAll() {
        logger.info("[correlationId:{}] Endorsement getAll called", MDC.get("correlationId"));
        BaseResponse<List<EndorsementResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<Endorsement> list = endorsementRepository.findAll();
            List<EndorsementResponseDto> out = new ArrayList<>();
            for (Endorsement endorsement : list) {
                out.add(EndorsementMapper.mapToResponseDto(endorsement));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement getAll: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByOrganizationId(UUID organizationId) {
        logger.info("[correlationId:{}] Endorsement getByOrganizationId called for {}", MDC.get("correlationId"), organizationId);
        BaseResponse<List<EndorsementResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<Endorsement> list = endorsementRepository.findByOrganization_OrganizationId(organizationId);
            List<EndorsementResponseDto> out = new ArrayList<>();
            for (Endorsement endorsement : list) {
                out.add(EndorsementMapper.mapToResponseDto(endorsement));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement getByOrganizationId: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByStatus(String status) {
        logger.info("[correlationId:{}] Endorsement getByStatus called for {}", MDC.get("correlationId"), status);
        BaseResponse<List<EndorsementResponseDto>> responseObj = new BaseResponse<>();
        try {
            AccountStatus accountStatus = AccountStatus.fromValue(status);
            List<Endorsement> list = endorsementRepository.findByStatus(accountStatus);
            List<EndorsementResponseDto> out = new ArrayList<>();
            for (Endorsement endorsement : list) {
                out.add(EndorsementMapper.mapToResponseDto(endorsement));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid status value: {}", MDC.get("correlationId"), status);
            return responseObj.render(responseObj.formErrorResponse("Invalid status value: " + status));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement getByStatus: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getByEndorsementType(String endorsementType) {
        logger.info("[correlationId:{}] Endorsement getByEndorsementType called for {}", MDC.get("correlationId"), endorsementType);
        BaseResponse<List<EndorsementResponseDto>> responseObj = new BaseResponse<>();
        try {
            EndorsementType type = EndorsementType.fromValue(endorsementType);
            List<Endorsement> list = endorsementRepository.findByEndorsementType(type);
            List<EndorsementResponseDto> out = new ArrayList<>();
            for (Endorsement endorsement : list) {
                out.add(EndorsementMapper.mapToResponseDto(endorsement));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid endorsement type value: {}", MDC.get("correlationId"), endorsementType);
            return responseObj.render(responseObj.formErrorResponse("Invalid endorsement type value: " + endorsementType));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement getByEndorsementType: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EndorsementResponseDto>>> getAllWithFilters(
            UUID organizationId, String organizationName, String status, String endorsementType, String uploadedBy,
            UUID splitGroupId, Long policyId, String fromDate, String toDate, int page, int size, String sortBy, String sortDirection) {
        logger.info("[correlationId:{}] Endorsement getAllWithFilters called - organizationId: {}, organizationName: {}, status: {}, endorsementType: {}, page: {}, size: {}",
                MDC.get("correlationId"), organizationId, organizationName, status, endorsementType, page, size);
        BaseResponse<List<EndorsementResponseDto>> responseObj = new BaseResponse<>();
        try {
            LocalDateTime fromDateTime = null;
            LocalDateTime toDateTime = null;
            
            if(status != null && !status.trim().isEmpty()) {
                status = status.toUpperCase().trim();
            }
            
            EndorsementType type = null;
            if (endorsementType != null && !endorsementType.trim().isEmpty()) {
                type = EndorsementType.fromValue(endorsementType);
            }
            if(fromDate != null) {
                fromDateTime = LocalDate.parse(fromDate).atStartOfDay();
            }
            if(toDate != null) {
                toDateTime = LocalDate.parse(toDate).atStartOfDay();
            }
            // Multi-tenant: restrict by organization IDs from JWT
            Map<String, List<String>> tenantMap = TenantContext.getCurrentTenant();
            List<String> orgIds = (tenantMap != null) ? tenantMap.get("organizationIds") : null;

            // Ensure orgIds is a mutable non-null list so later code can add provided organizationId
            if (orgIds == null) {
                orgIds = new ArrayList<>();
            }

            // If a specific organizationId param is provided, include it if not already present
            if (organizationId != null && !orgIds.contains(organizationId.toString())) {
                orgIds.add(organizationId.toString());
            }

            List<UUID> organizationIds = new ArrayList<>();
            for (String orgIdStr : orgIds) {
                try {
                    organizationIds.add(UUID.fromString(orgIdStr));
                } catch (IllegalArgumentException iae) {
                    logger.warn("[correlationId:{}] Skipping invalid organizationId from tenant context: {}", MDC.get("correlationId"), orgIdStr);
                }
            }
            logger.info("####################### ORGANIZATION IDS: " + organizationIds.toString());
            // Build specification with all filters
            Specification<Endorsement> spec = EndorsementSpecification.withFilters(
                organizationIds,
                organizationName,
                status,
                type,
                uploadedBy,
                splitGroupId,
                policyId,
                fromDateTime,
                toDateTime
            );
            
            // Create sort and page request
            Sort sort = createSort(sortBy, sortDirection);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            
            // Execute query using specification
            Page<Endorsement> endorsementPage = endorsementRepository.findAll(spec, pageRequest);

            // Map to DTOs
            List<EndorsementResponseDto> out = new ArrayList<>();
            for (Endorsement endorsement : endorsementPage.getContent()) {
                out.add(EndorsementMapper.mapToResponseDto(endorsement));
            }

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, endorsementPage.getTotalElements()));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid enum value in getAllWithFilters: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse("Invalid enum value: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement getAllWithFilters: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "APPROVE")
    public ResponseEntity<ResponseDto<String>> approve(MultipartFile[] files, EndorsementRequestDto requestDto) {
        logger.info("[correlationId:{}] Endorsement approve called for {}", MDC.get("correlationId"), requestDto.getEndorsementId());
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if(files != null && files.length > 3) {
                return responseObj.render(responseObj.formErrorResponse("Maximum 3 files are allowed"));
            }
            Optional<Endorsement> opt = endorsementRepository.findById(requestDto.getEndorsementId());
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            Optional<Organization> orgOpt = organizationRepository.findById(requestDto.getOrganizationId());
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            UUID orgId = orgOpt.get().getOrganizationId();
            if (organizationAccessHelper != null) {
                organizationAccessHelper.validateAndSetContext(orgId);
            } else if (jwtUserExtractor != null) {
                jwtUserExtractor.validateOrganizationAccess(orgId);
                com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setOrganizationId(orgId);
            }
            Organization organization = orgOpt.get();
            AdminUser uploadedBy = null;
            if (EnvironmentUtil.isProductionEnvironment(environment)) {
                String username = jwtUserExtractor.getCurrentUsername();
                Optional<AdminUser> uploadedByOpt = adminUserRepository.findByUsername(username);
            if (uploadedByOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Uploaded by user not found"));
            }
            uploadedBy = uploadedByOpt.get();
            }
            Endorsement endorsement = opt.get();
            if(endorsement.getStatus().equals(AccountStatus.COMPLETED)) {
                return responseObj.render(responseObj.formErrorResponse("Endorsement already completed"));
            }
            if(files != null && files.length > 0) {
            for(MultipartFile file : files) {
                ResponseEntity<ResponseDto<String>> documentResponse = documentService.uploadDocument(file, DocumentType.ENDORSEMENT.toString(), DocumentCategory.ENDORSEMENT_DOCUMENTS.toString(), DocumentEntityType.ORGANIZATION.toString(), endorsement.getEndorsementId().toString(), "Supporting Documents");
                if(documentResponse.getBody() != null && documentResponse.getBody().getErrorCode() != null){
                    return responseObj.render(responseObj.formErrorResponse(documentResponse.getBody().getMessage()));
                }
            }
           }
            EndorsementMapper.updateEntityFromDto(endorsement, requestDto, organization, null, uploadedBy);

            // Policy-based endorsement splitting may store people across multiple split endorsements.
            // Build a scoped set of endorsement IDs (current + same split group) and resolve deals from:
            // 1) customers.endorsement_id and 2) deal_endorsements join table.
            List<UUID> scopedEndorsementIds = new ArrayList<>();
            scopedEndorsementIds.add(endorsement.getEndorsementId());
            if (endorsement.getSplitGroupId() != null) {
                List<Endorsement> splitGroup = endorsementRepository.findBySplitGroupId(endorsement.getSplitGroupId());
                if (splitGroup != null && !splitGroup.isEmpty()) {
                    scopedEndorsementIds = splitGroup.stream()
                            .map(Endorsement::getEndorsementId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                }
            }

            Map<UUID, Deals> dealsById = new java.util.LinkedHashMap<>();
            for (UUID eid : scopedEndorsementIds) {
                dealsRepository.findByEndorsementId(eid)
                        .forEach(d -> {
                            if (d != null && d.getIndividualId() != null) {
                                dealsById.put(d.getIndividualId(), d);
                            }
                        });
                List<DealEndorsement> links = dealEndorsementRepository.findByEndorsement_EndorsementId(eid);
                if (links != null) {
                    links.stream()
                            .map(DealEndorsement::getDeal)
                            .filter(Objects::nonNull)
                            .filter(d -> d.getIndividualId() != null)
                            .forEach(d -> dealsById.put(d.getIndividualId(), d));
                }
            }
            List<Deals> deals = new ArrayList<>(dealsById.values());
            if(deals.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No deals found to approve"));
            }
            if(!deals.stream().anyMatch(deal -> deal.getStatus().equals(AccountStatus.PENDING_APPROVAL) || deal.getStatus().equals(AccountStatus.PENDING_EXIT))) {
                return responseObj.render(responseObj.formErrorResponse("No deals found to approve"));
            }
         
            deals.stream().filter(deal -> deal.getStatus().equals(AccountStatus.PENDING_APPROVAL)).forEach(deal -> {
                deal.setStatus(AccountStatus.ACTIVE);
                deal.setUpdatedAt(LocalDateTime.now());
                dealsRepository.save(deal);
            });
            deals.stream().filter(deal -> deal.getStatus().equals(AccountStatus.PENDING_EXIT)).forEach(deal -> {
                deal.setStatus(AccountStatus.INACTIVE);
                deal.setUpdatedAt(LocalDateTime.now());
                dealsRepository.save(deal);
            });
            endorsement.setApprovedAt(LocalDateTime.now());
            endorsement.setStatus(AccountStatus.COMPLETED);
            endorsement.setUpdatedAt(LocalDateTime.now());
            endorsementRepository.save(endorsement);

            if (cdBalanceService != null && requestDto.getCdBalanceEntries() != null && !requestDto.getCdBalanceEntries().isEmpty()) {
                String performedBy = requestDto.getApprovedBy() != null && !requestDto.getApprovedBy().isBlank()
                        ? requestDto.getApprovedBy()
                        : jwtUserExtractor.extractCurrentUsername();
                cdBalanceService.recordEndorsementCdBalanceEntries(
                        endorsement.getEndorsementId(),
                        organization.getOrganizationId(),
                        requestDto.getCdBalanceEntries(),
                        endorsement.getEndorsementType(),
                        performedBy);
            }

            if (employeePolicyMapService != null) {
                if (endorsement.getEndorsementType() == EndorsementType.ADDITION || endorsement.getEndorsementType() == EndorsementType.INITIAL_UPLOAD) {
                    employeePolicyMapService.createMappingsFromEndorsement(endorsement.getEndorsementId(), endorsement.getEndorsementType().name());
                } else if (endorsement.getEndorsementType() == EndorsementType.DELETION) {
                    employeePolicyMapService.cancelMappingsFromEndorsement(endorsement.getEndorsementId());
                }
            }
            if (lifeEventEndorsementService != null && endorsement.getLifeEventType() != null && !endorsement.getLifeEventType().isBlank()) {
                lifeEventEndorsementService.onLifeEventEndorsementApproved(endorsement);
            }

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Endorsement approved successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("[correlationId:{}] Endorsement approve rejected: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement approve: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to approve endorsement!"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> reject(UUID endorsementId) {
        logger.info("[correlationId:{}] Endorsement reject called for {}", MDC.get("correlationId"), endorsementId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            Endorsement endorsement = opt.get();
            try {
                AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(endorsement));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Could not serialize endorsement for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
            }
            endorsement.setStatus(AccountStatus.REJECTED);
            endorsement.setUpdatedAt(LocalDateTime.now());
            endorsementRepository.save(endorsement);
            com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setNewSnapshotEntity(endorsement);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Endorsement rejected successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement reject: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to reject endorsement!"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> getPendingCount() {
        logger.info("[correlationId:{}] Endorsement getPendingCount called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Map<String, List<String>> tenantMap = TenantContext.getCurrentTenant();
            List<String> orgIds = (tenantMap != null) ? tenantMap.get("organizationIds") : null;
            List<UUID> organizationIds = new ArrayList<>();
            for (String orgIdStr : orgIds) {
                try {
                    organizationIds.add(UUID.fromString(orgIdStr));
                } catch (IllegalArgumentException iae) {
                    logger.warn("[correlationId:{}] Skipping invalid organizationId from tenant context: {}", MDC.get("correlationId"), orgIdStr);
                }
            }
            Specification<Endorsement> spec = EndorsementSpecification.countPendingByOrganizationIds(organizationIds);
            List<Endorsement> endorsementList = endorsementRepository.findAll(spec);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Pending count: " + endorsementList.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement getPendingCount: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "APPROVE")
    public ResponseEntity<ResponseDto<String>> confirm(UUID endorsementId) {
        logger.info("[correlationId:{}] Endorsement confirm called for {}", MDC.get("correlationId"), endorsementId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            Optional<Organization> orgOpt = organizationRepository.findById(opt.get().getOrganization().getOrganizationId());
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            UUID orgId = orgOpt.get().getOrganizationId();
            if (organizationAccessHelper != null) {
                organizationAccessHelper.validateAndSetContext(orgId);
            } else if (jwtUserExtractor != null) {
                jwtUserExtractor.validateOrganizationAccess(orgId);
                com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setOrganizationId(orgId);
            }
            // Check if there are any deals that need confirmation
            List<Deals> deals = dealsRepository.findByEndorsementId(endorsementId);
            if(deals.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No deals found to confirm"));
            }
            
            boolean hasPendingStatus = deals.stream()
                .anyMatch(deal -> deal.getStatus().equals(AccountStatus.APPROVED) || 
                                 deal.getStatus().equals(AccountStatus.LEAVING));
            
            if(!hasPendingStatus) {
                return responseObj.render(responseObj.formErrorResponse("No deals found to confirm"));
            }
            
            // Activation Logic: SQL Update (without date check)
            // UPDATE customers SET status='ACTIVE', updated_at=CURRENT_TIMESTAMP 
            // WHERE status='APPROVED' AND endorsement_id = :endorsementId
            LocalDateTime updatedAt = LocalDateTime.now();
            
            int activatedCount = dealsRepository.activateApprovedDealsForConfirm(
                endorsementId,
                AccountStatus.APPROVED,
                AccountStatus.ACTIVE,
                updatedAt
            );
            
            // Deactivation Logic: SQL Update (without date check)
            // UPDATE customers SET status='INACTIVE', updated_at=CURRENT_TIMESTAMP 
            // WHERE status='LEAVING' AND endorsement_id = :endorsementId
            int deactivatedCount = dealsRepository.deactivateLeavingDealsForConfirm(
                endorsementId,
                AccountStatus.LEAVING,
                AccountStatus.INACTIVE,
                updatedAt
            );
            
            // Update endorsement status to COMPLETED
            Optional<Endorsement> endorsementOpt = endorsementRepository.findById(endorsementId);
            if (endorsementOpt.isPresent()) {
                Endorsement endorsement = endorsementOpt.get();
                endorsement.setStatus(AccountStatus.COMPLETED);
                endorsement.setUpdatedAt(updatedAt);
                endorsementRepository.save(endorsement);
            }
            
            if (activatedCount > 0 || deactivatedCount > 0) {
                logger.info("[correlationId:{}] Activated {} deals and deactivated {} deals for endorsement {}", 
                    MDC.get("correlationId"), activatedCount, deactivatedCount, endorsementId);
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS,
                "Endorsement confirmed successfully. Activated: " + activatedCount + ", Deactivated: " + deactivatedCount));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement confirm: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to confirm endorsement!"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "BULK_APPROVE")
    public ResponseEntity<ResponseDto<String>> confirmSchedule() {
        logger.info("[correlationId:{}] Endorsement confirmSchedule called for all deals", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
            LocalDateTime updatedAt = LocalDateTime.now();
            
            // First, get the deals that will be activated (before updating)
            List<Deals> dealsToActivate = dealsRepository.findByStatusAndDateOfJoining(
                AccountStatus.APPROVED, 
                currentDate
            );
            
            // Get the deals that will be deactivated (before updating)
            List<Deals> dealsToDeactivate = dealsRepository.findByStatusAndDateOfExit(
                AccountStatus.LEAVING, 
                currentDate
            );
            
            // Extract unique endorsement IDs from deals that will be activated
            Set<UUID> activationEndorsementIds = dealsToActivate.stream()
                .map(Deals::getEndorsementId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            // Extract unique endorsement IDs from deals that will be deactivated
            Set<UUID> deactivationEndorsementIds = dealsToDeactivate.stream()
                .map(Deals::getEndorsementId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            // Perform the actual activation/deactivation
            // Activation Logic: SQL Update
            // UPDATE customers SET status='ACTIVE', updated_at=CURRENT_TIMESTAMP 
            // WHERE status='APPROVED' AND date_of_joining <= CURRENT_DATE
            int activatedCount = dealsRepository.activateAllApprovedDealsByDate(
                AccountStatus.APPROVED,
                AccountStatus.ACTIVE,
                currentDate,
                updatedAt
            );
            
            // Deactivation Logic: SQL Update
            // UPDATE customers SET status='INACTIVE', updated_at=CURRENT_TIMESTAMP 
            // WHERE status='LEAVING' AND date_of_exit <= CURRENT_DATE
            int deactivatedCount = dealsRepository.deactivateAllLeavingDealsByDate(
                AccountStatus.LEAVING,
                AccountStatus.INACTIVE,
                currentDate,
                updatedAt
            );
            
            // Combine all unique endorsement IDs that need to be updated
            Set<UUID> allEndorsementIds = new HashSet<>();
            allEndorsementIds.addAll(activationEndorsementIds);
            allEndorsementIds.addAll(deactivationEndorsementIds);
            
            // Update each endorsement to COMPLETED (similar to lines 578-584)
            int completedEndorsementsCount = 0;
            for (UUID endorsementId : allEndorsementIds) {
                Optional<Endorsement> endorsementOpt = endorsementRepository.findById(endorsementId);
                if (endorsementOpt.isPresent()) {
                    Endorsement endorsement = endorsementOpt.get();
                    // Only update if status is APPROVED (to avoid updating already completed ones)
                    if (endorsement.getStatus() == AccountStatus.APPROVED) {
                        endorsement.setStatus(AccountStatus.COMPLETED);
                        endorsement.setUpdatedAt(updatedAt);
                        endorsementRepository.save(endorsement);
                        completedEndorsementsCount++;
                    }
                }
            }
            
            if (activatedCount > 0 || deactivatedCount > 0) {
                logger.info("[correlationId:{}] Activated {} deals and deactivated {} deals based on schedule. " +
                    "Completed {} endorsements", 
                    MDC.get("correlationId"), activatedCount, deactivatedCount, completedEndorsementsCount);
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, 
                "Schedule confirmed successfully. Activated: " + activatedCount + 
                ", Deactivated: " + deactivatedCount + 
                ", Completed endorsements: " + completedEndorsementsCount));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement confirmSchedule: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to confirm schedule endorsement!"));
        }
    }
    


    @Override
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(String endorsementId, int page, int rec) {
        BaseResponse<List<DocumentResponseDto>> responseObj = new BaseResponse<>();
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(UUID.fromString(endorsementId));
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            UUID orgId = opt.get().getOrganization().getOrganizationId();
            if (organizationAccessHelper != null) {
                organizationAccessHelper.validateAndSetContext(orgId);
            } else if (jwtUserExtractor != null) {
                jwtUserExtractor.validateOrganizationAccess(orgId);
                com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setOrganizationId(orgId);
            }
            if (page == -1 && rec == -1) {
                logger.info("[correlationId:{}] Getting all documents for entity: {}", MDC.get("correlationId"), endorsementId);
                List<Document> documents = documentRepository.findByEntityId(endorsementId);
                List<DocumentResponseDto> documentResponseDtos = documents.stream()
                    .map(document -> new DocumentResponseDto(document.getDocumentId().toString(), document.getDocumentType(), document.getUploadedAt(), document.getMimeType(), document.getNotes(), document.getOriginalFilename(), document.getDocumentCategory().toString(), DocumentServiceImpl.formatFileSize(document.getFileSize())))
                    .collect(Collectors.toList());
                if(opt.get().getEnrollmentWindow() != null) {
                    UUID enrollmentWindowId = opt.get().getEnrollmentWindow().getId();
                    List<Document> enrollmentWindowDocuments = documentRepository.findByEntityId(enrollmentWindowId.toString());
                    documentResponseDtos.addAll(enrollmentWindowDocuments.stream()
                        .map(document -> new DocumentResponseDto(document.getDocumentId().toString(), document.getDocumentType(), document.getUploadedAt(), document.getMimeType(), document.getNotes(), document.getOriginalFilename(), document.getDocumentCategory().toString(), DocumentServiceImpl.formatFileSize(document.getFileSize())))
                        .collect(Collectors.toList()));
                }        
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, documentResponseDtos, documents.size()));
            }
            Page<Document> documents = documentRepository.findByEntityId(endorsementId, PageRequest.of(page, rec));
            List<DocumentResponseDto> documentResponseDtos = documents.getContent().stream()
                .map(document -> new DocumentResponseDto(document.getDocumentId().toString(), document.getDocumentType(), document.getUploadedAt(), document.getMimeType(), document.getNotes(), document.getOriginalFilename(), document.getDocumentCategory().toString(), DocumentServiceImpl.formatFileSize(document.getFileSize())))
                .collect(Collectors.toList());

            if(opt.get().getEnrollmentWindow() != null) {
                UUID enrollmentWindowId = opt.get().getEnrollmentWindow().getId();
                List<Document> enrollmentWindowDocuments = documentRepository.findByEntityId(enrollmentWindowId.toString());
                documentResponseDtos.addAll(enrollmentWindowDocuments.stream()
                    .map(document -> new DocumentResponseDto(document.getDocumentId().toString(), document.getDocumentType(), document.getUploadedAt(), document.getMimeType(), document.getNotes(), document.getOriginalFilename(), document.getDocumentCategory().toString(), DocumentServiceImpl.formatFileSize(document.getFileSize())))
                    .collect(Collectors.toList()));
            }    
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, documentResponseDtos, documents.getTotalElements()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error getting documents: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse("Error getting documents: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<Resource> downloadDocument(UUID endorsementId,String documentId) {
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Optional<Organization> orgOpt = organizationRepository.findById(opt.get().getOrganization().getOrganizationId());
            if (orgOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            UUID orgId = orgOpt.get().getOrganizationId();
            if (organizationAccessHelper != null) {
                organizationAccessHelper.validateAndSetContext(orgId);
            } else if (jwtUserExtractor != null) {
                jwtUserExtractor.validateOrganizationAccess(orgId);
                com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setOrganizationId(orgId);
            }
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
            logger.error("[correlationId:{}] Error downloading document: {}", MDC.get("correlationId"), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "SUBMIT")
    public ResponseEntity<ResponseDto<EmployeeOnboardingResponseDto>> employeeOnboarding(UUID endorsementId) {
        EmployeeOnboardingRequestDto requestDto = new EmployeeOnboardingRequestDto();
        requestDto.setEndorsementId(endorsementId);
        return employeeOnboarding(requestDto);
    }

    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "SUBMIT")
    public ResponseEntity<ResponseDto<EmployeeOnboardingResponseDto>> employeeOnboarding(EmployeeOnboardingRequestDto requestDto) {
        logger.info("[correlationId:{}] Endorsement employeeOnboarding called for endorsementId: {}, individualIds: {}", 
            MDC.get("correlationId"), requestDto.getEndorsementId(), requestDto.getIndividualIds());
        BaseResponse<EmployeeOnboardingResponseDto> responseObj = new BaseResponse<>();
        try {
            // Validate that at least one parameter is provided
            if (requestDto.getEndorsementId() == null && 
                (requestDto.getIndividualIds() == null || requestDto.getIndividualIds().isEmpty())) {
                return responseObj.render(responseObj.formErrorResponse("Either endorsementId or individualIds must be provided"));
            }

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);
            List<String> successUsers = new ArrayList<>();
            List<String> failedUsers = new ArrayList<>();
            
            List<Deals> deals;
            Organization organization;
            
            // Handle endorsementId case
            if (requestDto.getEndorsementId() != null) {
                Optional<Endorsement> opt = endorsementRepository.findById(requestDto.getEndorsementId());
                if (opt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
                }
                if (!opt.get().getStatus().equals(AccountStatus.COMPLETED)) {
                    return responseObj.render(responseObj.formErrorResponse(200, "Endorsement not completed yet"));
                }
                UUID endorsementOrgId = opt.get().getOrganization().getOrganizationId();
                if (organizationAccessHelper != null) {
                    organizationAccessHelper.validateAndSetContext(endorsementOrgId);
                } else if (jwtUserExtractor != null) {
                    jwtUserExtractor.validateOrganizationAccess(endorsementOrgId);
                    com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setOrganizationId(endorsementOrgId);
                }
                Optional<Organization> orgOpt = organizationRepository.findById(endorsementOrgId);
                if (orgOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Organization not found"));
                }
                organization = orgOpt.get();
                deals = dealsRepository.findByEndorsementId(requestDto.getEndorsementId());
            } else {
                // Handle individualIds case
                deals = dealsRepository.findByIndividualIdIn(requestDto.getIndividualIds());
                if (deals.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse(200, "No deals found for provided individualIds"));
                }
                // Get organization from the first deal (assuming all deals belong to same organization)
                UUID organizationId = deals.get(0).getOrganization() != null 
                    ? deals.get(0).getOrganization().getOrganizationId() 
                    : null;
                if (organizationId == null) {
                    return responseObj.render(responseObj.formErrorResponse("Deals must belong to an organization"));
                }
                if (organizationAccessHelper != null) {
                    organizationAccessHelper.validateAndSetContext(organizationId);
                } else if (jwtUserExtractor != null) {
                    jwtUserExtractor.validateOrganizationAccess(organizationId);
                    com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setOrganizationId(organizationId);
                }
                Optional<Organization> orgOpt = organizationRepository.findById(organizationId);
                if (orgOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Organization not found"));
                }
                organization = orgOpt.get();
                
                // Validate all deals belong to the same organization
                boolean allSameOrg = deals.stream()
                    .allMatch(deal -> deal.getOrganization() != null && 
                        deal.getOrganization().getOrganizationId().equals(organizationId));
                if (!allSameOrg) {
                    return responseObj.render(responseObj.formErrorResponse("All deals must belong to the same organization"));
                }
            }
            
            if(!deals.stream().anyMatch(deal -> deal.getStatus().equals(AccountStatus.ACTIVE))) {
                return responseObj.render(responseObj.formErrorResponse(200, "No active deals found to onboard"));
            }
            if(deals.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(200,"No deals found to onboard"));
            }
            
            String orgGroupName = "ORG_" + organization.getOrganizationName().trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");
            String groupId = keycloakUtil.getGroupIdByName(orgGroupName);
            if(groupId == null || groupId.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No groups found"));
            }
            
            // Process deals for onboarding
            processDealsForOnboarding(deals, orgGroupName, successCount, failedCount, successUsers, failedUsers);
            
            if(failedCount.get() > 0) {
                return responseObj.render(responseObj.formErrorResponse("Employee onboarding failed for some users. Failed: " + failedCount.get() + ", Failed users: " + failedUsers.toString()));
            }
            EmployeeOnboardingResponseDto employeeOnboardingResponseDto = new EmployeeOnboardingResponseDto();
            employeeOnboardingResponseDto.setSuccessUsers(successUsers);
            employeeOnboardingResponseDto.setFailedUsers(failedUsers);
            employeeOnboardingResponseDto.setSuccessCount(successCount.get());
            employeeOnboardingResponseDto.setFailedCount(failedCount.get());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, employeeOnboardingResponseDto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement employeeOnboarding: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to complete employee onboarding!"));
        }
    }

    /**
     * Helper method to process deals for onboarding
     */
    private void processDealsForOnboarding(List<Deals> deals, String orgName, 
            AtomicInteger successCount, AtomicInteger failedCount, 
            List<String> successUsers, List<String> failedUsers) {
        deals.stream().forEach(deal -> {
            try {
                if(deal.getRelationship().equals("SELF")) {
                    String password = PasswordGenerator.generateRandomPassword();
                    keycloakUtil.createUser(deal.getFullName(), deal.getEmail().toLowerCase(), deal.getEmail().toLowerCase(), 
                        "ROLE_EMPLOYEE", Arrays.asList(orgName), true, password, deal.getIndividualId().toString());
                    emailService.sendWelcomeEmail(deal.getEmail().toLowerCase(), deal.getFullName(), deal.getEmail().toLowerCase(), password);
                    successCount.incrementAndGet();
                    successUsers.add(deal.getEmail());
                } 
            } catch (Exception e) {
                failedCount.incrementAndGet();
                logger.error("[correlationId:{}] Error creating user for deal: {}", MDC.get("correlationId"), e.getMessage());
                failedUsers.add(deal.getEmail() != null ? deal.getEmail() : deal.getFullName() != null ? deal.getFullName() : "Unknown");
            }
        });
    }

    
    /**
     * Helper method to create Sort object
     */
    private Sort createSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "createdAt";
        }
        if (sortDirection == null || sortDirection.trim().isEmpty()) {
            sortDirection = "DESC";
        }
        if(sortBy.equalsIgnoreCase("organizationName")) {
            sortBy = "organization.organizationName";
        }
        if(sortBy.equalsIgnoreCase("uploadedByName")) {
            sortBy = "uploadedBy.username";
        }


        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "HEALTH_ID_UPLOAD", action = "BULK_UPDATE")
    public ResponseEntity<ResponseDto<List<HealthIdUploadDto>>> uploadHealthIds(UUID endorsementId, List<HealthIdUploadDto> healthIdList) {
        logger.info("[correlationId:{}] Upload health IDs called for endorsement: {}", MDC.get("correlationId"), endorsementId);

        try {
            // Step 1: Check if endorsement exists and get organization
            Endorsement endorsement = endorsementRepository.findByEndorsementId(endorsementId)
                    .orElseThrow(() -> new IllegalArgumentException("Endorsement not found with ID: " + endorsementId));

            if (endorsement.getOrganization() == null) {
                logger.error("[correlationId:{}] Organization not found for endorsement: {}", MDC.get("correlationId"), endorsementId);
                return ResponseEntity.badRequest()
                        .body(new ResponseDto<>(400, "Organization not found for this endorsement"));
            }

            UUID organizationId = endorsement.getOrganization().getOrganizationId();
            logger.info("[correlationId:{}] Processing {} health ID records for organization: {}",
                    MDC.get("correlationId"), healthIdList.size(), organizationId);

            int updatedCount = 0;
            List<HealthIdUploadDto> invalidCustomers = new ArrayList<>();
            List<Deals> validCustomers = new ArrayList<>();

            // Validate all records first (no updates yet)
            for (HealthIdUploadDto healthIdDto : healthIdList) {
                try {
                    // Employee ID (primary's for dependents) must not be blank
                    if (healthIdDto.getEmployeeId() == null || healthIdDto.getEmployeeId().isBlank()) {
                        healthIdDto.setErrorReason("Employee ID is required");
                        invalidCustomers.add(healthIdDto);
                        logger.warn("[correlationId:{}] Employee ID is blank for name:{}, relationship:{}",
                                MDC.get("correlationId"), healthIdDto.getName(), healthIdDto.getRelationship());
                        continue;
                    }

                    String normalizedRelationship = normalizeRelationshipForLookup(healthIdDto.getRelationship());
                    Optional<Deals> customerOpt = dealsRepository.findByNameAndEmployeeNumberAndRelationshipAndOrganizationIdForEndorsement(
                            healthIdDto.getName(), healthIdDto.getEmployeeId(), normalizedRelationship, organizationId, endorsementId);

                    // Fallback for SELF/Employee: if not found in endorsement, try organization scope.
                    // Employees may exist as primaryIndividual of dependents in endorsement but not be directly linked.
                    if (customerOpt.isEmpty() && ("SELF".equalsIgnoreCase(normalizedRelationship) || "EMPLOYEE".equalsIgnoreCase(normalizedRelationship))) {
                        customerOpt = dealsRepository.findByNameAndEmployeeNumberAndRelationshipAndOrganizationId(
                                healthIdDto.getName(), healthIdDto.getEmployeeId(), normalizedRelationship, organizationId);
                    }

                    if (customerOpt.isPresent()) {
                        Deals customer = customerOpt.get();

                        boolean relationshipMatches = customer.getRelationship() != null &&
                                normalizeRelationshipForLookup(customer.getRelationship()).equalsIgnoreCase(normalizedRelationship);
                        boolean nameMatches = matchesName(customer, healthIdDto.getName());

                        if (relationshipMatches && nameMatches) {
                            validCustomers.add(customer);
                        } else {
                            String reason = !nameMatches && !relationshipMatches
                                    ? "Name and relationship do not match database records"
                                    : !nameMatches
                                            ? "Name does not match database record (expected format may differ)"
                                            : "Relationship does not match database record";
                            healthIdDto.setErrorReason(reason);
                            invalidCustomers.add(healthIdDto);
                            logger.warn("[correlationId:{}] Validation failed for employeeId:{}, relationship:{}, name:{}",
                                    MDC.get("correlationId"),
                                    healthIdDto.getEmployeeId(),
                                    healthIdDto.getRelationship(),
                                    healthIdDto.getName());
                        }
                    } else {
                        healthIdDto.setErrorReason("Employee not found for employeeId " + healthIdDto.getEmployeeId()
                                + ", relationship " + healthIdDto.getRelationship()
                                + ", name \"" + healthIdDto.getName()
                                + "\" in this endorsement or organization");
                        invalidCustomers.add(healthIdDto);
                        logger.warn("[correlationId:{}] Employee not found for employeeId:{}, relationship:{}, name:{}",
                                MDC.get("correlationId"),
                                healthIdDto.getEmployeeId(),
                                healthIdDto.getRelationship(),
                                healthIdDto.getName());
                    }
                } catch (Exception e) {
                    healthIdDto.setErrorReason("Validation error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                    invalidCustomers.add(healthIdDto);
                    logger.error("[correlationId:{}] Error validating health ID for employeeId:{} - name:{}",
                            MDC.get("correlationId"), healthIdDto.getEmployeeId(), healthIdDto.getName(), e);
                }
            }

            // If any invalid, return without updating anyone
            if (!invalidCustomers.isEmpty()) {
                logger.info("[correlationId:{}] Health ID upload aborted due to invalid records. Count: {}",
                        MDC.get("correlationId"), invalidCustomers.size());
                return ResponseEntity.badRequest()
                        .body(new ResponseDto<>("Health ID upload failed", invalidCustomers, invalidCustomers.size()));
            }
            // Step 3: All valid - proceed to update
            // Perform updates only when all are valid
            for (int i = 0; i < healthIdList.size(); i++) {
                Deals customer = validCustomers.get(i);
                HealthIdUploadDto healthIdDto = healthIdList.get(i);
                customer.setHealthId(healthIdDto.getHealthId());
                customer.setUpdatedAt(LocalDateTime.now());
                dealsRepository.save(customer);
                updatedCount++;
            }

            logger.info("[correlationId:{}] Health ID upload completed - Updated: {}",
                    MDC.get("correlationId"), updatedCount);

            return ResponseEntity.ok()
                    .body(new ResponseDto<>("Health IDs uploaded successfully", null, updatedCount));


        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Validation error: {}", MDC.get("correlationId"), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error uploading health IDs for endorsement: {}",
                    MDC.get("correlationId"), endorsementId, e);
            return ResponseEntity.internalServerError()
                    .body(new ResponseDto<>(500, "Failed to upload health IDs: " + e.getMessage()));
        }
    }

    @Override
    @AuditedOperation(schemaName = "cpc", tableName = "endorsements", entityType = "ENDORSEMENT", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> deactivateEndorsement(UUID endorsementId) {
        logger.info("[correlationId:{}] Deactivate endorsement called for endorsementId: {}", MDC.get("correlationId"), endorsementId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if(opt.isEmpty()) {
                return responseObj.render(responseObj.formSuccessResponse("Endorsement not found or already deactivated.s"));
            }
            Endorsement endorsement = opt.get();
            try {
                AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(endorsement));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Could not serialize endorsement for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
            }
            if(endorsement.getStatus() == AccountStatus.INACTIVE) {
                return responseObj.render(responseObj.formSuccessResponse("Endorsement is already deactivated."));
            }
            endorsement.setStatus(AccountStatus.INACTIVE);
            endorsement.setUpdatedAt(LocalDateTime.now());
            endorsementRepository.save(endorsement);
            com.vimainsurance.vimaadmin.audit.AuditContextSupplier.setNewSnapshotEntity(endorsement);
            return responseObj.render(responseObj.formSuccessResponse("Deactivation of endorsement is completed successfully."));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement deactivateEndorsement: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to deactivate endorsement!"));
        }
    }
    /**
     * Normalizes relationship for lookup: "Employee" (case insensitive) is treated as "SELF".
     */
    private String normalizeRelationshipForLookup(String relationship) {
        if (relationship == null || relationship.isBlank()) {
            return relationship;
        }
        return "employee".equalsIgnoreCase(relationship.trim()) ? "SELF" : relationship.trim();
    }

    /**
     * Checks if the deal's name matches the given name.
     * Handles both fullName (EmployeeToDeals) and firstName+lastName (CsvDealsReaderUtil) storage.
     */
    private boolean matchesName(Deals customer, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalizedName = name.trim();
        if (customer.getFullName() != null && customer.getFullName().trim().equalsIgnoreCase(normalizedName)) {
            return true;
        }
        String first = customer.getFirstName() != null ? customer.getFirstName().trim() : "";
        String last = customer.getLastName() != null ? customer.getLastName().trim() : "";
        String firstLast = (first + " " + last).trim();
        if (!firstLast.isBlank() && firstLast.equalsIgnoreCase(normalizedName)) {
            return true;
        }
        if (!first.isBlank() && first.equalsIgnoreCase(normalizedName)) {
            return true;
        }
        if (!last.isBlank() && last.equalsIgnoreCase(normalizedName)) {
            return true;
        }
        return false;
    }

}

