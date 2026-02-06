package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowStatsDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.SelfEmployeeEnrollmentRequestDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.enums.NomineeRelationship;
import com.vimainsurance.vimaadmin.exception.OrganizationAccessDeniedException;
import com.vimainsurance.vimaadmin.mapper.EnrollmentWindowMapper;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.service.IEnrollmentWindowService;
import com.vimainsurance.vimaadmin.specification.EnrollmentWindowSpecification;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.TenantContext;
import com.vimainsurance.vimaadmin.util.TransactionUtil;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;

@Service
public class EnrollmentWindowServiceImpl implements IEnrollmentWindowService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentWindowServiceImpl.class);

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IEnrollmentWindowsRepository enrollmentWindowsRepository;

    @Autowired
    private IOrganizationRepository organizationRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Autowired
    private IEnrollmentInvitationRepository enrollmentInvitationRepository;

    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Autowired
    private IDocumentService documentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> create(EnrollmentWindowRequestDto requestDto) {
        logger.info("[correlationId:{}] EnrollmentWindow create called", MDC.get("correlationId"));
        BaseResponse<EnrollmentWindowResponseDto> responseObj = new BaseResponse<>();
        try {
            if (requestDto.getEndDate() != null && requestDto.getStartDate() != null
                    && !requestDto.getEndDate().isAfter(requestDto.getStartDate())) {
                return responseObj.render(responseObj.formErrorResponse("End date must be after start date"));
            }
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(requestDto.getOrganizationId());
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            jwtUserExtractor.validateOrganizationAccess(orgOpt.get().getOrganizationId());

            String username = jwtUserExtractor.getCurrentUsername();
            Optional<AdminUser> createdByOpt = adminUserRepository.findByUsername(username);
            if (createdByOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Current user not found"));
            }

            EnrollmentWindows entity = EnrollmentWindowMapper.mapToEntity(requestDto, orgOpt.get(), createdByOpt.get());
            entity = enrollmentWindowsRepository.save(entity);
            EnrollmentWindowResponseDto dto = EnrollmentWindowMapper.mapToResponseDto(entity);

            logger.info("[correlationId:{}] EnrollmentWindow created successfully", MDC.get("correlationId"));
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
        } catch (OrganizationAccessDeniedException e) {
            TransactionUtil.markRollbackOnly();
            logger.warn("[correlationId:{}] Organization access denied: {}", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (IllegalArgumentException e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] Invalid value in EnrollmentWindow create: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse("Invalid value: " + e.getMessage()));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] Exception in EnrollmentWindow create: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_CREATED));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> uploadEmployees(UUID windowId, List<SelfEmployeeEnrollmentRequestDto> selfEmployeeEnrollmentRequestDtos, MultipartFile file) {
        logger.info("[correlationId:{}] EnrollmentWindow uploadEmployees called for window {}", MDC.get("correlationId"), windowId);
        BaseResponse<EnrollmentWindowResponseDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentWindows window = enrollmentWindowsRepository.findById(windowId)
                    .orElse(null);
            if (window == null) {
                return responseObj.render(responseObj.formErrorResponse("Enrollment window not found"));
            }
            Organization organization = window.getOrganization();
            if (organization == null) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found for window"));
            }
            jwtUserExtractor.validateOrganizationAccess(organization.getOrganizationId());

            List<String> errors = validateSelfEmployeeEnrollmentRequest(selfEmployeeEnrollmentRequestDtos, organization.getOrganizationId());
            if (!errors.isEmpty()) {
                @SuppressWarnings("unchecked")
                ResponseDto<EnrollmentWindowResponseDto> errorDto = (ResponseDto<EnrollmentWindowResponseDto>) (ResponseDto<?>) responseObj.formErrorResponse("Validation failed", errors);
                return responseObj.render(errorDto);
            }

            createSelfEmployee(selfEmployeeEnrollmentRequestDtos, organization, window);
            logger.info("[correlationId:{}] Self employees created successfully for window {}", MDC.get("correlationId"), windowId);

            ResponseEntity<ResponseDto<String>> documentResponse = documentService.uploadDocument(file, DocumentType.SELF_ENROLLMENT.getValue(), DocumentCategory.ENDORSEMENT_DOCUMENTS.getValue(), DocumentEntityType.ORGANIZATION.getValue(), windowId.toString(), "");
            ResponseDto<String> docBody = documentResponse.getBody();
            if (docBody != null && docBody.getErrorCode() != null) {
                TransactionUtil.markRollbackOnly();
                return responseObj.render(responseObj.formErrorResponse(docBody.getMessage()));
            }

            EnrollmentWindowResponseDto dto = EnrollmentWindowMapper.mapToResponseDto(window);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
        } catch (OrganizationAccessDeniedException e) {
            TransactionUtil.markRollbackOnly();
            logger.warn("[correlationId:{}] Organization access denied: {}", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] Exception in EnrollmentWindow uploadEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_CREATED));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EnrollmentWindowResponseDto>>> getAllWithFilters(
            UUID organizationId, String status, String name, String fromDate, String toDate,
            int page, int size, String sortBy, String sortDirection) {
        logger.info("[correlationId:{}] EnrollmentWindow getAllWithFilters called", MDC.get("correlationId"));
        BaseResponse<List<EnrollmentWindowResponseDto>> responseObj = new BaseResponse<>();
        try {
            LocalDate from = fromDate != null && !fromDate.isBlank() ? LocalDate.parse(fromDate) : null;
            LocalDate to = toDate != null && !toDate.isBlank() ? LocalDate.parse(toDate) : null;

            Map<String, List<String>> tenantMap = TenantContext.getCurrentTenant();
            List<String> orgIds = (tenantMap != null) ? tenantMap.get("organizationIds") : null;
            if (orgIds == null) {
                orgIds = new ArrayList<>();
            }
            if (organizationId != null && !orgIds.contains(organizationId.toString())) {
                orgIds.add(organizationId.toString());
            }
            List<UUID> organizationIds = new ArrayList<>();
            for (String orgIdStr : orgIds) {
                try {
                    organizationIds.add(UUID.fromString(orgIdStr));
                } catch (IllegalArgumentException iae) {
                    logger.warn("[correlationId:{}] Skipping invalid organizationId: {}", MDC.get("correlationId"), orgIdStr);
                }
            }

            Specification<EnrollmentWindows> spec = EnrollmentWindowSpecification.withFilters(
                    organizationIds, status, name, from, to, true);
            Sort sort = createSort(sortBy, sortDirection);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            Page<EnrollmentWindows> pageResult = enrollmentWindowsRepository.findAll(spec, pageRequest);

            List<EnrollmentWindowResponseDto> out = new ArrayList<>();
            for (EnrollmentWindows ew : pageResult.getContent()) {
                out.add(EnrollmentWindowMapper.mapToResponseDto(ew));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, pageResult.getTotalElements()));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid value in getAllWithFilters: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse("Invalid value: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow getAllWithFilters: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> getById(UUID id) {
        logger.info("[correlationId:{}] EnrollmentWindow getById called for {}", MDC.get("correlationId"), id);
        BaseResponse<EnrollmentWindowResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            jwtUserExtractor.validateOrganizationAccess(opt.get().getOrganization().getOrganizationId());
            EnrollmentWindowResponseDto dto = EnrollmentWindowMapper.mapToResponseDto(opt.get());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied: {}", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow getById: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> update(UUID id, EnrollmentWindowRequestDto requestDto) {
        logger.info("[correlationId:{}] EnrollmentWindow update called for {}", MDC.get("correlationId"), id);
        BaseResponse<EnrollmentWindowResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            EnrollmentWindows entity = opt.get();
            jwtUserExtractor.validateOrganizationAccess(entity.getOrganization().getOrganizationId());

            if (entity.getStatus() == EnrollementStatus.CANCELLED) {
                return responseObj.render(responseObj.formErrorResponse("Cannot update a cancelled enrollment window"));
            }
            if (requestDto.getEndDate() != null && requestDto.getStartDate() != null
                    && !requestDto.getEndDate().isAfter(requestDto.getStartDate())) {
                return responseObj.render(responseObj.formErrorResponse("End date must be after start date"));
            }

            Organization organization = entity.getOrganization();
            if (requestDto.getOrganizationId() != null && !requestDto.getOrganizationId().equals(entity.getOrganization().getOrganizationId())) {
                Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(requestDto.getOrganizationId());
                if (orgOpt.isEmpty()) {
                    return responseObj.render(responseObj.formErrorResponse("Organization not found"));
                }
                jwtUserExtractor.validateOrganizationAccess(orgOpt.get().getOrganizationId());
                organization = orgOpt.get();
            }

            EnrollmentWindowMapper.updateEntityFromDto(entity, requestDto, organization);
            entity = enrollmentWindowsRepository.save(entity);
            EnrollmentWindowResponseDto dto = EnrollmentWindowMapper.mapToResponseDto(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied: {}", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid value in EnrollmentWindow update: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse("Invalid value: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow update: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.UPDATE_FAILED));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<String>> activate(UUID id) {
        logger.info("[correlationId:{}] EnrollmentWindow activate called for {}", MDC.get("correlationId"), id);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            EnrollmentWindows entity = opt.get();
            jwtUserExtractor.validateOrganizationAccess(entity.getOrganization().getOrganizationId());
            if (entity.getStatus() == EnrollementStatus.CANCELLED) {
                return responseObj.render(responseObj.formErrorResponse("Cannot activate a cancelled enrollment window"));
            }
            if (entity.getStatus() != EnrollementStatus.SCHEDULED) {
                return responseObj.render(responseObj.formErrorResponse("Only scheduled windows can be activated"));
            }
            entity.setStatus(EnrollementStatus.ACTIVE);
            entity.setClosedAt(null);
            enrollmentWindowsRepository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Enrollment window activated successfully"));
        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied: {}", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow activate: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to activate enrollment window"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<String>> close(UUID id) {
        logger.info("[correlationId:{}] EnrollmentWindow close called for {}", MDC.get("correlationId"), id);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            EnrollmentWindows entity = opt.get();
            jwtUserExtractor.validateOrganizationAccess(entity.getOrganization().getOrganizationId());
            if (entity.getStatus() == EnrollementStatus.CANCELLED) {
                return responseObj.render(responseObj.formErrorResponse("Window is already cancelled"));
            }
            entity.setStatus(EnrollementStatus.CLOSED);
            entity.setClosedAt(LocalDateTime.now());
            enrollmentWindowsRepository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Enrollment window closed successfully"));
        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied: {}", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow close: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to close enrollment window"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<String>> delete(UUID id) {
        logger.info("[correlationId:{}] EnrollmentWindow delete (soft) called for {}", MDC.get("correlationId"), id);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            EnrollmentWindows entity = opt.get();
            jwtUserExtractor.validateOrganizationAccess(entity.getOrganization().getOrganizationId());
            if (entity.getStatus() == EnrollementStatus.CANCELLED) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Enrollment window is already deleted"));
            }
            entity.setStatus(EnrollementStatus.CANCELLED);
            enrollmentWindowsRepository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied: {}", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow delete: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.DELETE_FAILED));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<EnrollmentWindowStatsDto>> getStats(UUID id) {
        logger.info("[correlationId:{}] EnrollmentWindow getStats called for {}", MDC.get("correlationId"), id);
        BaseResponse<EnrollmentWindowStatsDto> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            jwtUserExtractor.validateOrganizationAccess(opt.get().getOrganization().getOrganizationId());

            List<com.vimainsurance.vimaadmin.entity.EnrollmentInvitation> invitations =
                    enrollmentInvitationRepository.findAllByEnrollmentWindow_Id(id);
            long sent = invitations.stream().filter(i -> i.getStatus() == EnrollementStatus.SENT).count();
            long completed = invitations.stream().filter(i -> i.getStatus() == EnrollementStatus.COMPLETED).count();
            long expired = invitations.stream().filter(i -> i.getStatus() == EnrollementStatus.EXPIRED).count();

            List<com.vimainsurance.vimaadmin.entity.EnrollmentSubmission> submissions =
                    enrollmentSubmissionRepository.findAllByEnrollmentWindow_Id(id);
            long draft = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.DRAFT).count();
            long submitted = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.SUBMITTED).count();
            long approved = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.APPROVED).count();
            long rejected = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.REJECTED).count();

            EnrollmentWindowStatsDto stats = EnrollmentWindowStatsDto.builder()
                    .invitationCount(invitations.size())
                    .invitationSentCount(sent)
                    .invitationCompletedCount(completed)
                    .invitationExpiredCount(expired)
                    .submissionCount(submissions.size())
                    .submissionDraftCount(draft)
                    .submissionSubmittedCount(submitted)
                    .submissionApprovedCount(approved)
                    .submissionRejectedCount(rejected)
                    .build();
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, stats));
        } catch (OrganizationAccessDeniedException e) {
            logger.warn("[correlationId:{}] Organization access denied: {}", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse(403, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow getStats: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    private Sort createSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "createdAt";
        }
        if (sortDirection == null || sortDirection.trim().isEmpty()) {
            sortDirection = "desc";
        }
        if ("organizationName".equalsIgnoreCase(sortBy)) {
            sortBy = "organization.organizationName";
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }


    /**
     * Validates self-service employee enrollment requests (supports bulk).
     * Uses batch DB lookups for existing employee numbers and emails, then validates each row.
     * Returns a list of validation error messages; empty list means all valid.
     */
    private List<String> validateSelfEmployeeEnrollmentRequest(List<SelfEmployeeEnrollmentRequestDto> requestDtos, UUID organizationId) {
        List<String> errors = new ArrayList<>();
        if (requestDtos == null || requestDtos.isEmpty()) {
            return errors;
        }
        String relationship = NomineeRelationship.SELF.getValue();

        List<String> employeeIds = requestDtos.stream().map(SelfEmployeeEnrollmentRequestDto::getEmployeeId).distinct().toList();
        List<String> emails = requestDtos.stream().map(SelfEmployeeEnrollmentRequestDto::getEmail).distinct().toList();

        Set<String> existingEmployeeIds = dealsRepository
                .findByEmployeeNumberInAndOrganizationIdAndRelationship(employeeIds, organizationId, relationship)
                .stream()
                .map(Deals::getEmployeeNumber)
                .collect(Collectors.toSet());
        Set<String> existingEmails = dealsRepository
                .findByEmailInAndOrganizationIdAndRelationship(emails, organizationId, relationship)
                .stream()
                .map(Deals::getEmail)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        Set<String> seenEmployeeIds = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        for (int i = 0; i < requestDtos.size(); i++) {
            SelfEmployeeEnrollmentRequestDto dto = requestDtos.get(i);
            int row = i + 1;
            String prefix = requestDtos.size() > 1 ? "Row " + row + " (" + dto.getEmployeeId() + "): " : "";

            if (existingEmployeeIds.contains(dto.getEmployeeId()) || !seenEmployeeIds.add(dto.getEmployeeId())) {
                errors.add(prefix + "Employee " + dto.getEmployeeId() + " already exists");
            }
            if (dto.getDateOfBirth() != null && dto.getDateOfBirth().isAfter(today)) {
                errors.add(prefix + "Date of birth cannot be in the future");
            }
            if (existingEmails.contains(dto.getEmail()) || !seenEmails.add(dto.getEmail())) {
                errors.add(prefix + "Email already " + dto.getEmail() + " exists");
            }
        }
        return errors;
    }

    /** Batch size for bulk self-employee save (e.g. 1000 records in batches of 500). */
    private static final int SELF_EMPLOYEE_SAVE_BATCH_SIZE = 500;

    private void createSelfEmployee(List<SelfEmployeeEnrollmentRequestDto> requestDtos, Organization organization, EnrollmentWindows enrollmentWindow) {
        try {
            List<Deals> employees = new ArrayList<>();
            for (SelfEmployeeEnrollmentRequestDto requestDto : requestDtos) {
                Deals employee = new Deals();
                employee.setFullName(requestDto.getName());
                employee.setEmployeeNumber(requestDto.getEmployeeId());
                employee.setDateOfBirth(requestDto.getDateOfBirth());
                employee.setEmail(requestDto.getEmail());
                employee.setPhone("");
                employee.setOrganization(organization);
                employee.setEnrollmentWindow(enrollmentWindow);
                employee.setRelationship(NomineeRelationship.SELF.getValue());
                employee.setStatus(AccountStatus.PENDING_APPROVAL);
                employees.add(employee);
            }
            for (int i = 0; i < employees.size(); i += SELF_EMPLOYEE_SAVE_BATCH_SIZE) {
                int end = Math.min(i + SELF_EMPLOYEE_SAVE_BATCH_SIZE, employees.size());
                List<Deals> batch = employees.subList(i, end);
                dealsRepository.saveAll(batch);
            }
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in createSelfEmployee: {}", MDC.get("correlationId"), e.getMessage(), e);
            throw new RuntimeException("Failed to create self employee");
        }
    }
}
