package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EndorsementRequestDto;
import com.vimainsurance.vimaadmin.dto.EndorsementResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import com.vimainsurance.vimaadmin.mapper.EndorsementMapper;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.service.IEndorsementService;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.specification.EndorsementSpecification;
import com.vimainsurance.vimaadmin.util.Constants;

import org.springframework.core.env.Environment;

import com.vimainsurance.vimaadmin.entity.Deals;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.util.EnvironmentUtil;

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
    private Environment environment;

    @Autowired
    private IDocumentService documentService;

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<String>> create(EndorsementRequestDto requestDto) {
        logger.info("[correlationId:{}] Endorsement create called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            // Validate organization
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
            // Update entity from DTO
            EndorsementMapper.updateEntityFromDto(endorsement, requestDto, organization, document, uploadedBy);
            endorsementRepository.save(endorsement);

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

            EndorsementResponseDto dto = EndorsementMapper.mapToResponseDto(opt.get(), dealsRepository.countByEndorsementIdAndRelationshipSelf(opt.get().getEndorsementId()), dealsRepository.countByEndorsementIdAndRelationshipNonSelf(opt.get().getEndorsementId()));
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
                out.add(EndorsementMapper.mapToResponseDto(endorsement, dealsRepository.countByEndorsementIdAndRelationshipSelf(endorsement.getEndorsementId()), dealsRepository.countByEndorsementIdAndRelationshipNonSelf(endorsement.getEndorsementId())));
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
                out.add(EndorsementMapper.mapToResponseDto(endorsement, dealsRepository.countByEndorsementIdAndRelationshipSelf(endorsement.getEndorsementId()), dealsRepository.countByEndorsementIdAndRelationshipNonSelf(endorsement.getEndorsementId())));
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
                out.add(EndorsementMapper.mapToResponseDto(endorsement, dealsRepository.countByEndorsementIdAndRelationshipSelf(endorsement.getEndorsementId()), dealsRepository.countByEndorsementIdAndRelationshipNonSelf(endorsement.getEndorsementId())));
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
                out.add(EndorsementMapper.mapToResponseDto(endorsement, dealsRepository.countByEndorsementIdAndRelationshipSelf(endorsement.getEndorsementId()), dealsRepository.countByEndorsementIdAndRelationshipNonSelf(endorsement.getEndorsementId())));
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
            String fromDate, String toDate, int page, int size, String sortBy, String sortDirection) {
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
            
            // Build specification with all filters
            Specification<Endorsement> spec = EndorsementSpecification.withFilters(
                organizationId,
                organizationName,
                status,
                type,
                uploadedBy,
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
                out.add(EndorsementMapper.mapToResponseDto(endorsement, dealsRepository.countByEndorsementIdAndRelationshipSelf(endorsement.getEndorsementId()), dealsRepository.countByEndorsementIdAndRelationshipNonSelf(endorsement.getEndorsementId())));
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
    public ResponseEntity<ResponseDto<String>> approve(MultipartFile[] files, EndorsementRequestDto requestDto) {
        logger.info("[correlationId:{}] Endorsement approve called for {}", MDC.get("correlationId"), requestDto.getEndorsementId());
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if(files.length > 3) {
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

            Organization organization = orgOpt.get();
            AdminUser uploadedBy = null;
            if(EnvironmentUtil.isProductionEnvironment(environment)) {
            Optional<AdminUser> uploadedByOpt = adminUserRepository.findById(requestDto.getUploadedBy());
            if (uploadedByOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Uploaded by user not found"));
            }
            uploadedBy = uploadedByOpt.get();
            }
            Endorsement endorsement = opt.get();
            if(endorsement.getStatus().equals(AccountStatus.APPROVED)) {
                return responseObj.render(responseObj.formErrorResponse("Endorsement already approved"));
            }
            for(MultipartFile file : files) {
                ResponseEntity<ResponseDto<String>> documentResponse = documentService.uploadDocument(file, DocumentType.OTHER.toString(), DocumentCategory.ENDORSEMENT_DOCUMENTS.toString(), DocumentEntityType.ORGANIZATION.toString(), endorsement.getEndorsementId().toString(), "Supporting Documents");
                if(documentResponse.getBody() != null && documentResponse.getBody().getErrorCode() != null){
                    return responseObj.render(responseObj.formErrorResponse(documentResponse.getBody().getMessage()));
                }
            }
            EndorsementMapper.updateEntityFromDto(endorsement, requestDto, organization, null, uploadedBy);

            List<Deals> deals = dealsRepository.findByEndorsementId(requestDto.getEndorsementId());
            if(deals.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No deals found to approve"));
            }
            if(!deals.stream().anyMatch(deal -> deal.getStatus().equals(AccountStatus.PENDING_APPROVAL) || deal.getStatus().equals(AccountStatus.PENDING_EXIT))) {
                return responseObj.render(responseObj.formErrorResponse("No deals found to approve"));
            }
         
            deals.stream().filter(deal -> deal.getStatus().equals(AccountStatus.PENDING_APPROVAL)).forEach(deal -> {
                deal.setStatus(AccountStatus.APPROVED);
                deal.setUpdatedAt(LocalDateTime.now());
                dealsRepository.save(deal);
            });
            deals.stream().filter(deal -> deal.getStatus().equals(AccountStatus.PENDING_EXIT)).forEach(deal -> {
                deal.setStatus(AccountStatus.LEAVING);
                deal.setUpdatedAt(LocalDateTime.now());
                dealsRepository.save(deal);
            });
            endorsement.setApprovedAt(LocalDateTime.now());
            endorsement.setStatus(AccountStatus.APPROVED);
            endorsement.setUpdatedAt(LocalDateTime.now());
            endorsementRepository.save(endorsement);

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Endorsement approved successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid confirmation method value: {}", MDC.get("correlationId"), requestDto.getConfirmationMethod());
            return responseObj.render(responseObj.formErrorResponse("Invalid confirmation method value: " + requestDto.getConfirmationMethod()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement approve: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to approve endorsement!"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<String>> reject(UUID endorsementId) {
        logger.info("[correlationId:{}] Endorsement reject called for {}", MDC.get("correlationId"), endorsementId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            Endorsement endorsement = opt.get();
            endorsement.setStatus(AccountStatus.REJECTED);
            endorsement.setUpdatedAt(LocalDateTime.now());
            endorsementRepository.save(endorsement);

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Endorsement rejected successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement reject: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to reject endorsement!"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> getPendingCount() {
        logger.info("[correlationId:{}] Endorsement getPendingCount called for {}", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Long count = endorsementRepository.getPendingCount();
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Pending count: " + count));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in Endorsement getPendingCount: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
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

}

