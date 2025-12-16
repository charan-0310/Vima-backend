package com.vimainsurance.vimaadmin.service.serviceimpl;

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
import com.vimainsurance.vimaadmin.enums.ConfirmationMethod;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import com.vimainsurance.vimaadmin.mapper.EndorsementMapper;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.service.IEndorsementService;
import com.vimainsurance.vimaadmin.util.Constants;

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

            // Fetch AdminUser entities if provided
            AdminUser approvedBy = null;
            if (requestDto.getApprovedBy() != null) {
                Optional<AdminUser> approvedByOpt = adminUserRepository.findById(requestDto.getApprovedBy());
                if (approvedByOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Approved by user not found"));
                }
                approvedBy = approvedByOpt.get();
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
            Endorsement endorsement = EndorsementMapper.mapToEntity(requestDto, organization, document, approvedBy, uploadedBy);
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
            AdminUser approvedBy = endorsement.getApprovedBy();
            if (requestDto.getApprovedBy() != null) {
                if (endorsement.getApprovedBy() == null || !requestDto.getApprovedBy().equals(endorsement.getApprovedBy().getId())) {
                    Optional<AdminUser> approvedByOpt = adminUserRepository.findById(requestDto.getApprovedBy());
                    if (approvedByOpt.isEmpty()) {
                        return responseObj.render(responseObj.formErrorResponse("Approved by user not found"));
                    }
                    approvedBy = approvedByOpt.get();
                }
            }

            AdminUser uploadedBy = endorsement.getUploadedBy();
            if (requestDto.getUploadedBy() != null) {
                if (endorsement.getUploadedBy() == null || !requestDto.getUploadedBy().equals(endorsement.getUploadedBy().getId())) {
                    Optional<AdminUser> uploadedByOpt = adminUserRepository.findById(requestDto.getUploadedBy());
                    if (uploadedByOpt.isEmpty()) {
                        return responseObj.render(responseObj.formErrorResponse("Uploaded by user not found"));
                    }
                    uploadedBy = uploadedByOpt.get();
                }
            }

            // Update entity from DTO
            EndorsementMapper.updateEntityFromDto(endorsement, requestDto, organization, document, approvedBy, uploadedBy);
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
            UUID organizationId, String organizationName, String status, String endorsementType,
            int page, int size, String sortBy, String sortDirection) {
        logger.info("[correlationId:{}] Endorsement getAllWithFilters called - organizationId: {}, organizationName: {}, status: {}, endorsementType: {}, page: {}, size: {}",
                MDC.get("correlationId"), organizationId, organizationName, status, endorsementType, page, size);
        BaseResponse<List<EndorsementResponseDto>> responseObj = new BaseResponse<>();
        try {
            Sort sort = createSort(sortBy, sortDirection);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            Page<Endorsement> endorsementPage;

            // Priority: organizationName search takes precedence if provided
            if (organizationName != null && !organizationName.trim().isEmpty()) {
                String searchName = organizationName.trim();
                
                if (status != null && endorsementType != null) {
                    AccountStatus accountStatus = AccountStatus.fromValue(status);
                    EndorsementType type = EndorsementType.fromValue(endorsementType);
                    endorsementPage = endorsementRepository.findByOrganizationNameAndStatusAndEndorsementType(
                            searchName, accountStatus, type, pageRequest);
                } else if (status != null) {
                    AccountStatus accountStatus = AccountStatus.fromValue(status);
                    endorsementPage = endorsementRepository.findByOrganizationNameAndStatus(
                            searchName, accountStatus, pageRequest);
                } else if (endorsementType != null) {
                    EndorsementType type = EndorsementType.fromValue(endorsementType);
                    endorsementPage = endorsementRepository.findByOrganizationNameAndEndorsementType(
                            searchName, type, pageRequest);
                } else {
                    endorsementPage = endorsementRepository.findByOrganizationName(searchName, pageRequest);
                }
            } else if (organizationId != null && status != null && endorsementType != null) {
                AccountStatus accountStatus = AccountStatus.fromValue(status);
                EndorsementType type = EndorsementType.fromValue(endorsementType);
                endorsementPage = endorsementRepository.findByOrganizationIdAndStatusAndEndorsementType(
                        organizationId, accountStatus, type, pageRequest);
            } else if (organizationId != null && status != null) {
                AccountStatus accountStatus = AccountStatus.fromValue(status);
                List<Endorsement> list = endorsementRepository.findByOrganizationIdAndStatus(organizationId, accountStatus);
                endorsementPage = createPageFromList(list, pageRequest);
            } else if (organizationId != null) {
                endorsementPage = endorsementRepository.findByOrganization_OrganizationId(organizationId, pageRequest);
            } else if (status != null) {
                AccountStatus accountStatus = AccountStatus.fromValue(status);
                endorsementPage = endorsementRepository.findByStatus(accountStatus, pageRequest);
            } else if (endorsementType != null) {
                EndorsementType type = EndorsementType.fromValue(endorsementType);
                endorsementPage = endorsementRepository.findByEndorsementType(type, pageRequest);
            } else {
                endorsementPage = endorsementRepository.findAll(pageRequest);
            }

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
    public ResponseEntity<ResponseDto<String>> approve(UUID endorsementId, UUID approvedBy, String confirmationMethod) {
        logger.info("[correlationId:{}] Endorsement approve called for {}", MDC.get("correlationId"), endorsementId);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<Endorsement> opt = endorsementRepository.findById(endorsementId);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }

            // Fetch AdminUser entity
            AdminUser approvedByUser = null;
            if (approvedBy != null) {
                Optional<AdminUser> approvedByOpt = adminUserRepository.findById(approvedBy);
                if (approvedByOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Approved by user not found"));
                }
                approvedByUser = approvedByOpt.get();
            }

            Endorsement endorsement = opt.get();
            endorsement.setStatus(AccountStatus.APPROVED);
            endorsement.setApprovedBy(approvedByUser);
            endorsement.setApprovedAt(LocalDateTime.now());

            if (confirmationMethod != null) {
                endorsement.setConfirmationMethod(ConfirmationMethod.fromValue(confirmationMethod));
            }

            endorsement.setUpdatedAt(LocalDateTime.now());
            endorsementRepository.save(endorsement);

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Endorsement approved successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid confirmation method value: {}", MDC.get("correlationId"), confirmationMethod);
            return responseObj.render(responseObj.formErrorResponse("Invalid confirmation method value: " + confirmationMethod));
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

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    /**
     * Helper method to create Page from List (for cases where repository doesn't support pagination)
     */
    private Page<Endorsement> createPageFromList(List<Endorsement> list, PageRequest pageRequest) {
        int start = (int) pageRequest.getOffset();
        int end = Math.min((start + pageRequest.getPageSize()), list.size());
        List<Endorsement> pageContent = start < list.size() ? list.subList(start, end) : new ArrayList<>();
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageRequest, list.size());
    }
}

