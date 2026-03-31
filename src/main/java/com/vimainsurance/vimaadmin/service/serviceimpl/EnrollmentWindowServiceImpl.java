package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vimainsurance.vimaadmin.repository.WindowProgressProjection;

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

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowStatsDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.SelfEmployeeEnrollmentRequestDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.enums.NomineeRelationship;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.mapper.EnrollmentWindowMapper;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEmployeePolicyMapRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IEnrollmentWindowService;
import com.vimainsurance.vimaadmin.service.IHRApprovalService;
import com.vimainsurance.vimaadmin.specification.EnrollmentWindowSpecification;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.EnrollmentUploadParserUtil;
import com.vimainsurance.vimaadmin.util.GmcCoverageUploadValidationUtil;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.OrganizationAccessHelper;
import com.vimainsurance.vimaadmin.util.TenantContext;
import com.vimainsurance.vimaadmin.util.TransactionUtil;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.AccountType;

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
    private IHRApprovalService hrApprovalService;

    @Autowired
    private JwtUserExtractor jwtUserExtractor;

    @Autowired(required = false)
    private OrganizationAccessHelper organizationAccessHelper;

    @Autowired
    private IDocumentService documentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IEmployeePolicyMapRepository employeePolicyMapRepository;

    @Autowired
    private IPolicyRepository policyRepository;

    private static final String POLICY_MAP_STATUS_ACTIVE = "ACTIVE";

    private void validateAndSetOrganizationContext(UUID organizationId) {
        if (organizationId == null) return;
        if (organizationAccessHelper != null) {
            organizationAccessHelper.validateAndSetContext(organizationId);
        } else if (jwtUserExtractor != null) {
            jwtUserExtractor.validateOrganizationAccess(organizationId);
            AuditContextSupplier.setOrganizationId(organizationId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_windows", entityType = "ENROLLMENT_WINDOW", action = "CREATE")
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> create(EnrollmentWindowRequestDto requestDto) {
        logger.info("[correlationId:{}] EnrollmentWindow create called", MDC.get("correlationId"));
        BaseResponse<EnrollmentWindowResponseDto> responseObj = new BaseResponse<>();
        try {
            if (requestDto.getEndDate() != null && requestDto.getStartDate() != null
                    && !requestDto.getEndDate().isAfter(requestDto.getStartDate())) {
                return responseObj.render(responseObj.formErrorResponse("End date must be after start date"));
            }
            if (requestDto.getOrganizationId() != null) {
                if (organizationAccessHelper != null) {
                    organizationAccessHelper.validateAndSetContext(requestDto.getOrganizationId());
                } else if (jwtUserExtractor != null) {
                    validateAndSetOrganizationContext(requestDto.getOrganizationId());
                    AuditContextSupplier.setOrganizationId(requestDto.getOrganizationId());
                }
            }
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(requestDto.getOrganizationId());
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
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
    public ResponseEntity<ResponseDto<List<String>>> validateEmployees(UUID organizationId, List<SelfEmployeeEnrollmentRequestDto> selfEmployeeEnrollmentRequestDtos) {
        logger.info("[correlationId:{}] EnrollmentWindow validateEmployees called for organization {}", MDC.get("correlationId"), organizationId);
        BaseResponse<List<String>> responseObj = new BaseResponse<>();
        try {
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            validateAndSetOrganizationContext(organizationId);

            if (selfEmployeeEnrollmentRequestDtos == null || selfEmployeeEnrollmentRequestDtos.isEmpty()) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Collections.emptyList()));
            }

            List<String> errors = validateSelfEmployeeEnrollmentRequest(selfEmployeeEnrollmentRequestDtos, organizationId);
            if (!errors.isEmpty()) {
                return responseObj.render(new ResponseDto<>(400, "Validation failed", errors));
            }
            List<String> renewalErrors = validateExistingEmployeesForRenewal(organizationId, selfEmployeeEnrollmentRequestDtos);
            if (!renewalErrors.isEmpty()) {
                return responseObj.render(new ResponseDto<>(400, "Validation failed", renewalErrors));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Collections.emptyList()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow validateEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Validation request failed"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "customers", entityType = "ENROLLMENT_EMPLOYEE", action = "BULK_CREATE")
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
            validateAndSetOrganizationContext(organization.getOrganizationId());

            List<SelfEmployeeEnrollmentRequestDto> selfRowsToUse;
            EnrollmentUploadParserUtil.EnrollmentParseResult parseResult = null;
            if (file != null && !file.isEmpty()) {
                parseResult = EnrollmentUploadParserUtil.parse(file);
                if (parseResult.hasSelfRows()) {
                    EnrollmentUploadParserUtil.normalizeChildRelationships(parseResult);
                    EnrollmentUploadParserUtil.validateSelfRows(parseResult);
                    List<Policy> activePolicies = policyRepository.findByOrganizationIdAndStatus(
                            organization.getOrganizationId(), PolicyStatus.ACTIVE);
                    List<String> coverageErrors = GmcCoverageUploadValidationUtil
                            .validateEnrollmentUploadRows(parseResult, activePolicies);
                    if (!coverageErrors.isEmpty()) {
                        parseResult.getErrors().addAll(coverageErrors);
                    }
                    if (!parseResult.hasFatalErrors()) {
                        selfRowsToUse = parseResult.getSelfRows();
                    } else {
                        @SuppressWarnings("unchecked")
                        ResponseDto<EnrollmentWindowResponseDto> errorDto = (ResponseDto<EnrollmentWindowResponseDto>) (ResponseDto<?>) responseObj.formErrorResponse("Validation failed", parseResult.getErrors());
                        return responseObj.render(errorDto);
                    }
                } else {
                    selfRowsToUse = (selfEmployeeEnrollmentRequestDtos != null && !selfEmployeeEnrollmentRequestDtos.isEmpty())
                            ? selfEmployeeEnrollmentRequestDtos
                            : new ArrayList<>();
                }
            } else {
                selfRowsToUse = (selfEmployeeEnrollmentRequestDtos != null && !selfEmployeeEnrollmentRequestDtos.isEmpty())
                        ? selfEmployeeEnrollmentRequestDtos
                        : new ArrayList<>();
            }

            if (selfRowsToUse.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No employee data provided. Upload a file with employee rows or provide employee list."));
            }

            List<String> errors = validateSelfEmployeeEnrollmentRequest(selfRowsToUse, organization.getOrganizationId());
            if (!errors.isEmpty()) {
                @SuppressWarnings("unchecked")
                ResponseDto<EnrollmentWindowResponseDto> errorDto = (ResponseDto<EnrollmentWindowResponseDto>) (ResponseDto<?>) responseObj.formErrorResponse("Validation failed", errors);
                return responseObj.render(errorDto);
            }

            List<String> renewalErrors = validateExistingEmployeesForRenewal(organization.getOrganizationId(), selfRowsToUse);
            if (!renewalErrors.isEmpty()) {
                @SuppressWarnings("unchecked")
                ResponseDto<EnrollmentWindowResponseDto> errorDto = (ResponseDto<EnrollmentWindowResponseDto>) (ResponseDto<?>) responseObj.formErrorResponse("Validation failed", renewalErrors);
                return responseObj.render(errorDto);
            }

            createSelfEmployee(selfRowsToUse, organization, window);
            logger.info("[correlationId:{}] Self employees created successfully for window {}", MDC.get("correlationId"), windowId);

            if (parseResult != null && !parseResult.getDependentRowsByEmployeeId().isEmpty()) {
                persistDependentsFromParseResult(parseResult, organization, window);
            }

            ResponseEntity<ResponseDto<String>> documentResponse = documentService.uploadDocument(file, DocumentType.SELF_ENROLLMENT.getValue(), DocumentCategory.ENDORSEMENT_DOCUMENTS.getValue(), DocumentEntityType.ORGANIZATION.getValue(), windowId.toString(), "");
            ResponseDto<String> docBody = documentResponse.getBody();
            if (docBody != null && docBody.getErrorCode() != null) {
                TransactionUtil.markRollbackOnly();
                return responseObj.render(responseObj.formErrorResponse(docBody.getMessage()));
            }

            EnrollmentWindowResponseDto dto = EnrollmentWindowMapper.mapToResponseDto(window);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] Exception in EnrollmentWindow uploadEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_CREATED));
        }
    }

    /**
     * For each employee that has dependent rows in the parse result, find or create draft EnrollmentSubmission
     * and set dependents JSON for self-enrollment prefill.
     */
    private void persistDependentsFromParseResult(EnrollmentUploadParserUtil.EnrollmentParseResult parseResult, Organization organization, EnrollmentWindows window) {
        String relationship = NomineeRelationship.SELF.getValue();
        for (Map.Entry<String, List<EnrollmentUploadParserUtil.DependentRow>> entry : parseResult.getDependentRowsByEmployeeId().entrySet()) {
            String employeeNumber = entry.getKey();
            List<EnrollmentUploadParserUtil.DependentRow> dependentRows = entry.getValue();
            if (dependentRows == null || dependentRows.isEmpty()) continue;
            Optional<Deals> dealOpt = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(
                    employeeNumber, organization.getOrganizationId(), relationship);
            if (dealOpt.isEmpty()) continue;
            Deals employeeDeal = dealOpt.get();
            String dependentsJson = buildDependentsJson(dependentRows);
            EnrollmentSubmission submission = enrollmentSubmissionRepository
                    .findByEmployee_IndividualIdAndEnrollmentWindow_Id(employeeDeal.getIndividualId(), window.getId())
                    .orElseGet(() -> {
                        EnrollmentSubmission draft = new EnrollmentSubmission();
                        draft.setEmployee(employeeDeal);
                        draft.setEnrollmentWindow(window);
                        draft.setStatus(EnrollementStatus.DRAFT);
                        draft.setPlanSelections("[]");
                        draft.setNomineeData("{}");
                        draft.setPersonalDetails("{}");
                        draft.setDependents("[]");
                        draft.setPremiumBreakdown("{}");
                        draft.setDeclarationAccepted(false);
                        return enrollmentSubmissionRepository.saveAndFlush(draft);
                    });
            submission.setDependents(dependentsJson);
            enrollmentSubmissionRepository.save(submission);
            logger.info("[correlationId:{}] Updated draft submission with {} dependents for employee {}", MDC.get("correlationId"), dependentRows.size(), employeeNumber);
        }
    }

    private String buildDependentsJson(List<EnrollmentUploadParserUtil.DependentRow> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (EnrollmentUploadParserUtil.DependentRow row : rows) {
            Map<String, Object> obj = new LinkedHashMap<>();
            String name = row.getName() != null ? row.getName().trim() : "";
            String[] parts = name.split("\\s+", 2);
            obj.put("firstName", parts.length > 0 ? parts[0] : "");
            obj.put("lastName", parts.length > 1 ? parts[1] : null);
            obj.put("fullName", name);
            obj.put("name", name);
            obj.put("relationship", row.getRelationship());
            String rawDob = row.getDateOfBirth();
            String isoDob = (rawDob != null && !rawDob.isBlank())
                ? EnrollmentUploadParserUtil.normalizeDateToIsoString(rawDob) : null;
            obj.put("dateOfBirth", isoDob != null ? isoDob : rawDob);
            obj.put("gender", row.getGender());
            obj.put("email", row.getEmail());
            list.add(obj);
        }
        try {
            return new ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            logger.warn("[correlationId:{}] Failed to serialize dependents JSON: {}", MDC.get("correlationId"), e.getMessage());
            return "[]";
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

            List<UUID> organizationIds = new ArrayList<>();
            if (organizationId != null) {
                // Explicit query param: filter only by this org (and enforce access)
                validateAndSetOrganizationContext(organizationId);
                organizationIds.add(organizationId);
            } else {
                // No org param: use tenant context (orgs the user has access to)
                Map<String, List<String>> tenantMap = TenantContext.getCurrentTenant();
                List<String> orgIds = (tenantMap != null) ? tenantMap.get("organizationIds") : null;
                if (orgIds == null) {
                    orgIds = new ArrayList<>();
                }
                for (String orgIdStr : orgIds) {
                    try {
                        organizationIds.add(UUID.fromString(orgIdStr));
                    } catch (IllegalArgumentException iae) {
                        logger.warn("[correlationId:{}] Skipping invalid organizationId: {}", MDC.get("correlationId"), orgIdStr);
                    }
                }
            }

            Specification<EnrollmentWindows> spec = EnrollmentWindowSpecification.withFilters(
                    organizationIds, status, name, from, to, true);
            Sort sort = createSort(sortBy, sortDirection);
            PageRequest pageRequest = PageRequest.of(page, size, sort);
            Page<EnrollmentWindows> pageResult = enrollmentWindowsRepository.findAll(spec, pageRequest);
            List<EnrollmentWindows> content = pageResult.getContent();

            List<EnrollmentWindowResponseDto> out = new ArrayList<>();
            if (!content.isEmpty()) {
                List<UUID> windowIds = content.stream().map(EnrollmentWindows::getId).toList();
                List<WindowProgressProjection> progressList = enrollmentWindowsRepository.findProgressByWindowIds(windowIds);
                Map<UUID, WindowProgressProjection> progressByWindow = progressList.stream()
                        .collect(Collectors.toMap(WindowProgressProjection::getWindowId, p -> p, (a, b) -> a));

                for (EnrollmentWindows ew : content) {
                    EnrollmentWindowResponseDto dto = EnrollmentWindowMapper.mapToResponseDto(ew);
                    WindowProgressProjection progress = progressByWindow.get(ew.getId());
                    if (progress != null) {
                        int totalEmployees = (int) Math.max(progress.getEmployeeCount(), progress.getInvitationCount());
                        long submittedCount = progress.getSubmittedCount();
                        double completionRate = totalEmployees > 0 ? (submittedCount * 100.0 / totalEmployees) : 0.0;
                        dto.setTotalEmployees(totalEmployees);
                        dto.setSubmittedCount((int) submittedCount);
                        dto.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);
                    }
                    out.add(dto);
                }
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
            validateAndSetOrganizationContext(opt.get().getOrganization().getOrganizationId());
            EnrollmentWindowResponseDto dto = EnrollmentWindowMapper.mapToResponseDto(opt.get());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow getById: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_windows", entityType = "ENROLLMENT_WINDOW", action = "UPDATE")
    public ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> update(UUID id, EnrollmentWindowRequestDto requestDto) {
        logger.info("[correlationId:{}] EnrollmentWindow update called for {}", MDC.get("correlationId"), id);
        BaseResponse<EnrollmentWindowResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            EnrollmentWindows entity = opt.get();
            validateAndSetOrganizationContext(entity.getOrganization().getOrganizationId());
            try {
                AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(entity));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Could not serialize enrollment window for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
            }
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
                validateAndSetOrganizationContext(orgOpt.get().getOrganizationId());
                organization = orgOpt.get();
            }

            EnrollmentWindowMapper.updateEntityFromDto(entity, requestDto, organization);
            entity = enrollmentWindowsRepository.save(entity);
            AuditContextSupplier.setNewSnapshotEntity(entity);
            EnrollmentWindowResponseDto dto = EnrollmentWindowMapper.mapToResponseDto(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, dto));
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
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_windows", entityType = "ENROLLMENT_WINDOW", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> activate(UUID id) {
        logger.info("[correlationId:{}] EnrollmentWindow activate called for {}", MDC.get("correlationId"), id);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            EnrollmentWindows entity = opt.get();
            validateAndSetOrganizationContext(entity.getOrganization().getOrganizationId());
            try {
                AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(entity));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Could not serialize enrollment window for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
            }
            if (entity.getStatus() == EnrollementStatus.CANCELLED) {
                return responseObj.render(responseObj.formErrorResponse("Cannot activate a cancelled enrollment window"));
            }
            if (entity.getStatus() != EnrollementStatus.SCHEDULED) {
                return responseObj.render(responseObj.formErrorResponse("Only scheduled windows can be activated"));
            }
            entity.setStatus(EnrollementStatus.ACTIVE);
            entity.setClosedAt(null);
            enrollmentWindowsRepository.save(entity);
            AuditContextSupplier.setNewSnapshotEntity(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Enrollment window activated successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow activate: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to activate enrollment window"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_windows", entityType = "ENROLLMENT_WINDOW", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> close(UUID id) {
        logger.info("[correlationId:{}] EnrollmentWindow close called for {}", MDC.get("correlationId"), id);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            EnrollmentWindows entity = opt.get();
            validateAndSetOrganizationContext(entity.getOrganization().getOrganizationId());
            try {
                AuditContextSupplier.setOldSnapshotJson(objectMapper.writeValueAsString(entity));
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Could not serialize enrollment window for audit old snapshot: {}", MDC.get("correlationId"), e.getMessage());
            }
            if (entity.getStatus() == EnrollementStatus.CANCELLED) {
                return responseObj.render(responseObj.formErrorResponse("Window is already cancelled"));
            }

            ResponseEntity<ResponseDto<String>> finalizeResult = hrApprovalService.finalizeEnrollmentWindow(id);
            if (finalizeResult.getBody() != null && finalizeResult.getBody().getErrorCode() != null) {
                return finalizeResult;
            }

            entity.setStatus(EnrollementStatus.CLOSED);
            entity.setClosedAt(LocalDateTime.now());
            enrollmentWindowsRepository.save(entity);
            AuditContextSupplier.setNewSnapshotEntity(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Enrollment window closed successfully"));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow close: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to close enrollment window"));
        }
    }

    @Override
    @Transactional
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_windows", entityType = "ENROLLMENT_WINDOW", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> delete(UUID id) {
        logger.info("[correlationId:{}] EnrollmentWindow delete (soft) called for {}", MDC.get("correlationId"), id);
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
            }
            EnrollmentWindows entity = opt.get();
            validateAndSetOrganizationContext(entity.getOrganization().getOrganizationId());
            if (entity.getStatus() == EnrollementStatus.CANCELLED) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Enrollment window is already deleted"));
            }
            entity.setStatus(EnrollementStatus.CANCELLED);
            enrollmentWindowsRepository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.DELETE_MESSAGE));
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
            validateAndSetOrganizationContext(opt.get().getOrganization().getOrganizationId());

            List<com.vimainsurance.vimaadmin.entity.EnrollmentInvitation> invitations =
                    enrollmentInvitationRepository.findAllByEnrollmentWindow_Id(id);
            long sent = invitations.stream().filter(i -> i.getStatus() == EnrollementStatus.SENT).count();
            long completed = invitations.stream().filter(i -> i.getStatus() == EnrollementStatus.COMPLETED).count();
            long expired = invitations.stream().filter(i -> i.getStatus() == EnrollementStatus.EXPIRED).count();

            List<com.vimainsurance.vimaadmin.entity.EnrollmentSubmission> submissions =
                    enrollmentSubmissionRepository.findAllByEnrollmentWindow_Id(id);
            long draft = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.DRAFT).count();
            long submitted = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.SUBMITTED || s.getStatus() == EnrollementStatus.APPROVED || s.getStatus() == EnrollementStatus.COMPLETED || s.getStatus() == EnrollementStatus.ENDORSED).count();
            long approved = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.APPROVED || s.getStatus() == EnrollementStatus.ENDORSED || s.getStatus() == EnrollementStatus.COMPLETED).count();
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
     * Does not reject based on "already exists in customers"; that is handled in upload flow
     * with policy-map date rule (renewal: new policy start must be after current policy end).
     */
    private List<String> validateSelfEmployeeEnrollmentRequest(List<SelfEmployeeEnrollmentRequestDto> requestDtos, UUID organizationId) {
        List<String> errors = new ArrayList<>();
        if (requestDtos == null || requestDtos.isEmpty()) {
            return errors;
        }
        LocalDate today = LocalDate.now();
        for (int i = 0; i < requestDtos.size(); i++) {
            SelfEmployeeEnrollmentRequestDto dto = requestDtos.get(i);
            int row = i + 1;
            String prefix = requestDtos.size() > 1 ? "Row " + row + " (" + dto.getEmployeeId() + "): " : "";

            if (dto.getDateOfBirth() != null && dto.getDateOfBirth().isAfter(today)) {
                errors.add(prefix + "Date of birth cannot be in the future");
            }
        }
        return errors;
    }

    /**
     * Returns the earliest start date among the organization's applicable (ACTIVE, appliesToEmployees) policies.
     * Used as "new policy start date" for renewal rule: allow upload only if new policy start is after existing policy end.
     * Empty if the company has no such policy ("no new policy" → existing employees get "Employee already exists.").
     */
    private Optional<LocalDate> getNewPolicyStartDateForOrganization(UUID organizationId) {
        List<Policy> policies = policyRepository.findByOrganizationIdAndStatus(organizationId, PolicyStatus.ACTIVE);
        List<LocalDate> starts = policies.stream()
                .filter(p -> Boolean.TRUE.equals(p.getAppliesToEmployees()))
                .map(Policy::getStartDate)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (starts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(starts.stream().min(LocalDate::compareTo).orElseThrow());
    }

    /**
     * For employees who already exist in customers: if there is no new policy for the company, reject with "Employee already exists.";
     * if there is a new policy, allow only when new policy start date is after the employee's current policy end date (renewal).
     */
    private List<String> validateExistingEmployeesForRenewal(UUID organizationId, List<SelfEmployeeEnrollmentRequestDto> requestDtos) {
        List<String> errors = new ArrayList<>();
        if (requestDtos == null || requestDtos.isEmpty()) {
            return errors;
        }
        List<String> distinctEmployeeIds = requestDtos.stream()
                .map(SelfEmployeeEnrollmentRequestDto::getEmployeeId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (distinctEmployeeIds.isEmpty()) {
            return errors;
        }

        Optional<LocalDate> newPolicyStartOpt = getNewPolicyStartDateForOrganization(organizationId);
        String relationship = NomineeRelationship.SELF.getValue();

        for (String employeeId : distinctEmployeeIds) {
            Optional<Deals> existingOpt = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(employeeId, organizationId, relationship);
            if (existingOpt.isEmpty()) {
                continue;
            }
            Deals existing = existingOpt.get();
            UUID individualId = existing.getIndividualId();

            if (newPolicyStartOpt.isEmpty()) {
                errors.add("Employee " + employeeId + " already exists.");
                continue;
            }
            LocalDate newPolicyStart = newPolicyStartOpt.get();
            List<EmployeePolicyMap> mappings = employeePolicyMapRepository.findByIndividualIdAndOrganizationIdAndStatus(individualId, organizationId, POLICY_MAP_STATUS_ACTIVE);
            if (mappings.isEmpty()) {
                continue;
            }
            LocalDate latestEnd = mappings.stream()
                    .map(EmployeePolicyMap::getEffectiveTo)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            if (latestEnd == null) {
                errors.add("Employee " + employeeId + " cannot be in two policies; new policy start must be after current policy end.");
                continue;
            }
            if (!newPolicyStart.isAfter(latestEnd)) {
                errors.add("Employee " + employeeId + " cannot be in two policies; new policy start must be after current policy end.");
            }
        }
        return errors;
    }

    /** Batch size for bulk self-employee save (e.g. 1000 records in batches of 500). */
    private static final int SELF_EMPLOYEE_SAVE_BATCH_SIZE = 500;

    /**
     * Create or update customers (Deals) for each SELF row: if employee already exists (same employee number + org), update and link to window; otherwise insert.
     */
    private void createSelfEmployee(List<SelfEmployeeEnrollmentRequestDto> requestDtos, Organization organization, EnrollmentWindows enrollmentWindow) {
        try {
            String relationship = NomineeRelationship.SELF.getValue();
            List<Deals> toSave = new ArrayList<>();
            for (SelfEmployeeEnrollmentRequestDto requestDto : requestDtos) {
                Optional<Deals> existingOpt = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(
                        requestDto.getEmployeeId(), organization.getOrganizationId(), relationship);
                if (existingOpt.isPresent()) {
                    Deals existing = existingOpt.get();
                    if (requestDto.getName() != null && !requestDto.getName().isBlank()) {
                        existing.setFullName(requestDto.getName());
                    }
                    if (requestDto.getEmail() != null && !requestDto.getEmail().isBlank()) {
                        existing.setEmail(requestDto.getEmail());
                    }
                    if (requestDto.getDateOfBirth() != null) {
                        existing.setDateOfBirth(requestDto.getDateOfBirth());
                    }
                    if (requestDto.getPhone() != null) {
                        existing.setPhone(requestDto.getPhone());
                    }
                    if (requestDto.getGender() != null) {
                        existing.setGender(requestDto.getGender());
                    }
                    if (requestDto.getDateOfJoining() != null) {
                        existing.setDateOfJoining(requestDto.getDateOfJoining());
                    }
                    if (requestDto.getDesignation() != null) {
                        existing.setDesignation(requestDto.getDesignation());
                    }
                    if (requestDto.getDepartment() != null) {
                        existing.setDepartment(requestDto.getDepartment());
                    }
                    existing.setEnrollmentWindow(enrollmentWindow);
                    existing.setUpdatedAt(LocalDateTime.now());
                    toSave.add(existing);
                } else {
                    Deals employee = new Deals();
                    employee.setFullName(requestDto.getName());
                    employee.setEmployeeNumber(requestDto.getEmployeeId());
                    if (requestDto.getDateOfBirth() != null) {
                        employee.setDateOfBirth(requestDto.getDateOfBirth());
                    }
                    employee.setEmail(requestDto.getEmail());
                    employee.setPhone(requestDto.getPhone() != null && !requestDto.getPhone().isBlank() ? requestDto.getPhone() : "");
                    if (requestDto.getGender() != null) {
                        employee.setGender(requestDto.getGender());
                    }
                    if (requestDto.getDateOfJoining() != null) {
                        employee.setDateOfJoining(requestDto.getDateOfJoining());
                    }
                    if (requestDto.getDesignation() != null) {
                        employee.setDesignation(requestDto.getDesignation());
                    }
                    if (requestDto.getDepartment() != null) {
                        employee.setDepartment(requestDto.getDepartment());
                    }
                    employee.setOrganization(organization);
                    employee.setEnrollmentWindow(enrollmentWindow);
                    employee.setRelationship(relationship);
                    employee.setStatus(AccountStatus.PENDING_APPROVAL);
                    employee.setAccountType(AccountType.CORPORATE_EMPLOYEE);
                    toSave.add(employee);
                }
            }
            for (int i = 0; i < toSave.size(); i += SELF_EMPLOYEE_SAVE_BATCH_SIZE) {
                int end = Math.min(i + SELF_EMPLOYEE_SAVE_BATCH_SIZE, toSave.size());
                List<Deals> batch = toSave.subList(i, end);
                dealsRepository.saveAll(batch);
            }
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in createSelfEmployee: {}", MDC.get("correlationId"), e.getMessage(), e);
            throw new RuntimeException("Failed to create self employee");
        }
    }
}
