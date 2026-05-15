package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.DependentEnrollmentUpdateDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowStatsDto;
import com.vimainsurance.vimaadmin.dto.RateCardCoverageIncompleteDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.SelfEmployeeEnrollmentRequestDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.EndorsementSource;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.enums.NomineeRelationship;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.CoverageType;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.mapper.EnrollmentWindowMapper;
import com.vimainsurance.vimaadmin.notification.FlagshipNotificationService;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEmployeePolicyMapRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IEnrollmentWindowService;
import com.vimainsurance.vimaadmin.service.IHRApprovalService;
import com.vimainsurance.vimaadmin.service.IPremiumCalculationService;
import com.vimainsurance.vimaadmin.specification.EnrollmentWindowSpecification;
import com.fasterxml.jackson.databind.JsonNode;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.EnrollmentWindowEmployeeCsvWriter;
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

    private static final Pattern CHILD_NUMBER_LABEL = Pattern.compile("(?i)^Child\\s*([1-4])\\s*$");

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
    @Autowired
    private IEndorsementRepository endorsementRepository;

    @Autowired(required = false)
    private FlagshipNotificationService flagshipNotificationService;

    @Autowired
    private IPremiumCalculationService premiumCalculationService;

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
            Optional<AdminUser> createdByOpt = jwtUserExtractor.resolveCurrentAdminUser();
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
    public ResponseEntity<ResponseDto<List<String>>> validateEmployees(
            UUID organizationId,
            List<SelfEmployeeEnrollmentRequestDto> selfEmployeeEnrollmentRequestDtos,
            Map<String, List<DependentEnrollmentUpdateDto>> dependentsByEmployeeId) {
        logger.info("[correlationId:{}] EnrollmentWindow validateEmployees called for organization {}", MDC.get("correlationId"), organizationId);
        BaseResponse<List<String>> responseObj = new BaseResponse<>();
        try {
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            validateAndSetOrganizationContext(organizationId);

            if (selfEmployeeEnrollmentRequestDtos == null) {
                selfEmployeeEnrollmentRequestDtos = Collections.emptyList();
            }
            if (dependentsByEmployeeId == null) {
                dependentsByEmployeeId = Collections.emptyMap();
            }

            if (selfEmployeeEnrollmentRequestDtos.isEmpty() && dependentsByEmployeeId.isEmpty()) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Collections.emptyList()));
            }

            List<String> errors = validateSelfEmployeeEnrollmentRequest(selfEmployeeEnrollmentRequestDtos, organizationId);
            if (!errors.isEmpty()) {
                return responseObj.render(new ResponseDto<>(400, "Validation failed", errors));
            }
            List<String> dependentFieldErrors = validateDependentEnrollmentRequests(dependentsByEmployeeId);
            if (!dependentFieldErrors.isEmpty()) {
                return responseObj.render(new ResponseDto<>(400, "Validation failed", dependentFieldErrors));
            }

            List<Policy> activePolicies = policyRepository.findByOrganizationIdAndStatus(organizationId, PolicyStatus.ACTIVE);
            Set<String> overlapBypassEmployeeIds = new HashSet<>(
                    getParentCoverageEmployeeIdsForOverlapBypassFromDependentsMap(
                            organizationId, dependentsByEmployeeId, activePolicies));

            if (!isDependentsOnlyRequestPayload(selfEmployeeEnrollmentRequestDtos)) {
                List<String> renewalErrors = validateExistingEmployeesForRenewal(
                        organizationId,
                        selfEmployeeEnrollmentRequestDtos,
                        overlapBypassEmployeeIds);
                if (!renewalErrors.isEmpty()) {
                    return responseObj.render(new ResponseDto<>(400, "Validation failed", renewalErrors));
                }
            }
            if (!dependentsByEmployeeId.isEmpty()) {
                List<String> dependentRenewalErrors = validateExistingDependentsForRenewal(
                        organizationId,
                        dependentsByEmployeeId,
                        overlapBypassEmployeeIds);
                if (!dependentRenewalErrors.isEmpty()) {
                    return responseObj.render(new ResponseDto<>(400, "Validation failed", dependentRenewalErrors));
                }
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Collections.emptyList()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow validateEmployees: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Validation request failed"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<String>>> validateUploadFile(UUID organizationId, MultipartFile file) {
        logger.info("[correlationId:{}] EnrollmentWindow validateUploadFile called for organization {}", MDC.get("correlationId"), organizationId);
        BaseResponse<List<String>> responseObj = new BaseResponse<>();
        try {
            Optional<Organization> orgOpt = organizationRepository.findByOrganizationId(organizationId);
            if (orgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Organization not found"));
            }
            validateAndSetOrganizationContext(organizationId);

            if (file == null || file.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("File is required"));
            }

            EnrollmentUploadParserUtil.EnrollmentParseResult parseResult = EnrollmentUploadParserUtil.parse(file);
            EnrollmentUploadParserUtil.normalizeChildRelationships(parseResult);
            if (parseResult.hasSelfRows()) {
                EnrollmentUploadParserUtil.validateSelfRows(parseResult);
            }

            List<Policy> activePolicies = policyRepository.findByOrganizationIdAndStatus(
                    organizationId, PolicyStatus.ACTIVE);
            List<String> coverageErrors = GmcCoverageUploadValidationUtil
                    .validateEnrollmentUploadRows(parseResult, activePolicies);
            if (!coverageErrors.isEmpty()) {
                parseResult.getErrors().addAll(coverageErrors);
            }
            if (parseResult.hasFatalErrors()) {
                return responseObj.render(new ResponseDto<>(400, "Validation failed", parseResult.getErrors()));
            }
            boolean allowParentOnlyDependentUpload =
                    isParentOnlyDependentUpload(organizationId, parseResult, activePolicies);
            if (!parseResult.hasSelfRows() && !allowParentOnlyDependentUpload) {
                return responseObj.render(new ResponseDto<>(400, "Validation failed",
                        List.of("No employee SELF rows found in upload file")));
            }

            if (!isDependentsOnlyUpload(parseResult)) {
            List<SelfEmployeeEnrollmentRequestDto> selfRows = parseResult.getSelfRows();
            if (!selfRows.isEmpty()) {
                List<String> errors = validateSelfEmployeeEnrollmentRequest(selfRows, organizationId);
                if (!errors.isEmpty()) {
                    return responseObj.render(new ResponseDto<>(400, "Validation failed", errors));
                }

                Set<String> overlapBypassEmployeeIds = getParentCoverageEmployeeIdsForOverlapBypass(
                        organizationId, parseResult, activePolicies);
                List<String> renewalErrors = validateExistingEmployeesForRenewal(
                        organizationId,
                        selfRows,
                        overlapBypassEmployeeIds);
                if (!renewalErrors.isEmpty()) {
                    return responseObj.render(new ResponseDto<>(400, "Validation failed", renewalErrors));
                }
            } else if (allowParentOnlyDependentUpload) {
                List<String> parentOnlyErrors = validateParentOnlyDependentUploadEmployees(
                        organizationId, parseResult, activePolicies);
                if (!parentOnlyErrors.isEmpty()) {
                    return responseObj.render(new ResponseDto<>(400, "Validation failed", parentOnlyErrors));
                }
            }
            }

            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Collections.emptyList()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow validateUploadFile: {}", MDC.get("correlationId"), e.getMessage(), e);
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
            List<Policy> activePolicies = policyRepository.findByOrganizationIdAndStatus(
                    organization.getOrganizationId(), PolicyStatus.ACTIVE);
            boolean allowParentOnlyDependentUpload = false;
            if (file != null && !file.isEmpty()) {
                parseResult = EnrollmentUploadParserUtil.parse(file);
                EnrollmentUploadParserUtil.normalizeChildRelationships(parseResult);
                if (parseResult.hasSelfRows()) {
                    EnrollmentUploadParserUtil.validateSelfRows(parseResult);
                }
                List<String> coverageErrors = GmcCoverageUploadValidationUtil
                        .validateEnrollmentUploadRows(parseResult, activePolicies);
                if (!coverageErrors.isEmpty()) {
                    parseResult.getErrors().addAll(coverageErrors);
                }
                if (parseResult.hasFatalErrors()) {
                    @SuppressWarnings("unchecked")
                    ResponseDto<EnrollmentWindowResponseDto> errorDto = (ResponseDto<EnrollmentWindowResponseDto>) (ResponseDto<?>) responseObj.formErrorResponse("Validation failed", parseResult.getErrors());
                    return responseObj.render(errorDto);
                }
                allowParentOnlyDependentUpload = isParentOnlyDependentUpload(
                        organization.getOrganizationId(), parseResult, activePolicies);
                if (parseResult.hasSelfRows()) {
                    selfRowsToUse = parseResult.getSelfRows();
                } else if (allowParentOnlyDependentUpload) {
                    selfRowsToUse = new ArrayList<>();
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

            if (selfRowsToUse.isEmpty() && !(allowParentOnlyDependentUpload
                    && parseResult != null
                    && !parseResult.getDependentRowsByEmployeeId().isEmpty())) {
                return responseObj.render(responseObj.formErrorResponse("No employee data provided. Upload a file with employee rows or provide employee list."));
            }

            if (!selfRowsToUse.isEmpty()) {
                List<String> errors = validateSelfEmployeeEnrollmentRequest(selfRowsToUse, organization.getOrganizationId());
                if (!errors.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    ResponseDto<EnrollmentWindowResponseDto> errorDto = (ResponseDto<EnrollmentWindowResponseDto>) (ResponseDto<?>) responseObj.formErrorResponse("Validation failed", errors);
                    return responseObj.render(errorDto);
                }

            if (!isDependentsOnlyUpload(parseResult)) {
                Set<String> overlapBypassEmployeeIds = getParentCoverageEmployeeIdsForOverlapBypass(
                        organization.getOrganizationId(), parseResult, activePolicies);
                List<String> renewalErrors = validateExistingEmployeesForRenewal(
                        organization.getOrganizationId(),
                        selfRowsToUse,
                        overlapBypassEmployeeIds);
                if (!renewalErrors.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    ResponseDto<EnrollmentWindowResponseDto> errorDto = (ResponseDto<EnrollmentWindowResponseDto>) (ResponseDto<?>) responseObj.formErrorResponse("Validation failed", renewalErrors);
                    return responseObj.render(errorDto);
                }
            }

                createSelfEmployee(selfRowsToUse, organization, window);
                logger.info("[correlationId:{}] Self employees created successfully for window {}", MDC.get("correlationId"), windowId);
            } else if (allowParentOnlyDependentUpload && parseResult != null) {
                List<String> parentOnlyErrors = validateParentOnlyDependentUploadEmployees(
                        organization.getOrganizationId(), parseResult, activePolicies);
                if (!parentOnlyErrors.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    ResponseDto<EnrollmentWindowResponseDto> errorDto = (ResponseDto<EnrollmentWindowResponseDto>) (ResponseDto<?>) responseObj.formErrorResponse("Validation failed", parentOnlyErrors);
                    return responseObj.render(errorDto);
                }
            }

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
            if (row.getActualRelationship() != null && !row.getActualRelationship().isBlank()) {
                obj.put("actualRelationship", row.getActualRelationship().trim());
            }
            String rawDob = row.getDateOfBirth();
            String isoDob = (rawDob != null && !rawDob.isBlank())
                ? EnrollmentUploadParserUtil.normalizeDateToIsoString(rawDob) : null;
            obj.put("dateOfBirth", isoDob != null ? isoDob : rawDob);
            obj.put("gender", row.getGender());
            obj.put("email", row.getEmail());
            obj.put("prefilledByHr", true);
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
    public ResponseEntity<?> activate(UUID id) {
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
            UUID orgId = entity.getOrganization() != null ? entity.getOrganization().getOrganizationId() : null;
            if (orgId != null) {
                var gaps = premiumCalculationService.findGapsInRateCardCoverageForActivePolicies(orgId);
                if (gaps != null && !gaps.isEmpty()) {
                    BaseResponse<Object> errObj = new BaseResponse<>();
                    RateCardCoverageIncompleteDto payload = RateCardCoverageIncompleteDto.builder()
                            .missing(gaps)
                            .build();
                    return errObj.render(errObj.formErrorResponseWithKey(422,
                            "Active policies are missing Rate Card coverage. Add or extend rate rows before activating.",
                            payload,
                            Constants.ERROR_KEY_RATE_CARD_COVERAGE_INCOMPLETE));
                }
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
            if (entity.getStatus() == EnrollementStatus.CLOSED) {
                return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Enrollment window is already closed"));
            }

            ResponseEntity<ResponseDto<String>> finalizeResult = hrApprovalService.finalizeEnrollmentWindow(id);
            if (finalizeResult.getBody() != null && finalizeResult.getBody().getErrorCode() != null) {
                return finalizeResult;
            }

            entity.setStatus(EnrollementStatus.CLOSED);
            entity.setClosedAt(LocalDateTime.now());
            enrollmentWindowsRepository.save(entity);
            if (flagshipNotificationService != null && entity.getOrganization() != null) {
                String display = entity.getOrganization().getOrganizationDisplayName() != null
                        && !entity.getOrganization().getOrganizationDisplayName().isBlank()
                                ? entity.getOrganization().getOrganizationDisplayName()
                                : entity.getOrganization().getOrganizationName();
                String closedByName = null;
                if (jwtUserExtractor != null) {
                    String username = jwtUserExtractor.getCurrentUsername();
                    if (username != null && !username.isBlank()) {
                        closedByName = adminUserRepository.findByUsername(username)
                                .map(AdminUser::getFullName)
                                .orElse(username);
                    }
                }
                Endorsement latestWindowEndorsement = endorsementRepository
                        .findFirstByOrganization_OrganizationIdAndEnrollmentWindow_IdAndSourceOrderByCreatedAtDesc(
                                entity.getOrganization().getOrganizationId(),
                                entity.getId(),
                                EndorsementSource.SELF_ENROLLMENT)
                        .orElse(null);
                flagshipNotificationService.scheduleEnrollmentWindowClosed(
                        entity.getOrganization().getOrganizationId(),
                        entity.getId(),
                        display,
                        closedByName,
                        latestWindowEndorsement != null ? latestWindowEndorsement.getEndorsementId() : null,
                        latestWindowEndorsement != null ? latestWindowEndorsement.getTotalEmployees() : null,
                        latestWindowEndorsement != null ? latestWindowEndorsement.getTotalDependents() : null);
            }
            AuditContextSupplier.setNewSnapshotEntity(entity);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Enrollment window closed successfully"));
        } catch (IllegalStateException e) {
            logger.error("[correlationId:{}] Cannot close enrollment window: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentWindow close: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to close enrollment window: " + e.getMessage()));
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
            long submitted = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.SUBMITTED || s.getStatus() == EnrollementStatus.APPROVED || s.getStatus() == EnrollementStatus.COMPLETED || s.getStatus() == EnrollementStatus.ENDORSED || s.getStatus() == EnrollementStatus.REJECTED).count();
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

    private static final DateTimeFormatter EXPORT_DOB = DateTimeFormatter.ofPattern("dd/MM/yy");

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> exportEmployeesCsv(UUID windowId) {
        Optional<EnrollmentWindows> opt = enrollmentWindowsRepository.findById(windowId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        EnrollmentWindows window = opt.get();
        validateAndSetOrganizationContext(window.getOrganization().getOrganizationId());

        List<List<String>> rows = buildEnrollmentWindowExportRows(windowId);
        String filename = sanitizeCsvFilename(window.getName()) + "_employees_export.csv";

        StreamingResponseBody body = outputStream -> {
            try {
                EnrollmentWindowEmployeeCsvWriter.writeBom(outputStream);
                Writer w = EnrollmentWindowEmployeeCsvWriter.newUtf8Writer(outputStream);
                EnrollmentWindowEmployeeCsvWriter.writeRow(w, EnrollmentWindowEmployeeCsvWriter.HEADERS);
                for (List<String> cells : rows) {
                    EnrollmentWindowEmployeeCsvWriter.writeRow(w, cells);
                }
                w.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    private List<List<String>> buildEnrollmentWindowExportRows(UUID windowId) {
        List<Deals> linked = dealsRepository.findByEnrollmentWindow_Id(windowId);
        // One primary row per employee number (avoids duplicate blocks if DB has repeats)
        List<Deals> primaries = linked.stream()
                .filter(this::isEnrollmentPrimaryDealRow)
                .collect(Collectors.toMap(
                        this::primaryExportDedupeKey,
                        d -> d,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new))
                .values()
                .stream()
                .toList();

        List<EnrollmentSubmission> subs = enrollmentSubmissionRepository.findAllByEnrollmentWindow_Id(windowId);
        Map<UUID, EnrollmentSubmission> submissionByEmployeeId = new HashMap<>();
        for (EnrollmentSubmission s : subs) {
            if (s.getEmployee() != null && s.getEmployee().getIndividualId() != null) {
                submissionByEmployeeId.put(s.getEmployee().getIndividualId(), s);
            }
        }

        List<List<String>> out = new ArrayList<>();
        for (Deals primary : primaries) {
            String empNo = primary.getEmployeeNumber() != null ? primary.getEmployeeNumber().trim() : "";
            EnrollmentSubmission sub = submissionByEmployeeId.get(primary.getIndividualId());
            PlanSumsCsv sums = sub != null ? parsePlanSelectionsForCsv(sub.getPlanSelections()) : PlanSumsCsv.empty();

            String pdJson = sub != null ? sub.getPersonalDetails() : null;
            String exportEmail = firstNonEmpty(primary.getEmail(), emailFromPersonalDetailsJson(pdJson));
            String exportMobile = firstNonEmpty(primary.getPhone(), phoneFromPersonalDetailsJson(pdJson));
            String exportDob = primary.getDateOfBirth() != null
                    ? formatDobForCsv(primary.getDateOfBirth())
                    : formatDobJsonValue(dobFromPersonalDetailsJson(pdJson));

            String primaryDisplayName = csvExportDisplayNameFromDeal(primary);
            String submissionStatus = csvExportSubmissionStatusLabel(sub);
            out.add(csvDataRow(empNo, formatRelationshipForCsv(primary), nullToEmpty(primaryDisplayName),
                    nullToEmpty(primary.getGender()), exportDob, sums,
                    nullToEmpty(exportEmail), nullToEmpty(exportMobile),
                    formatDobForCsv(primary.getDateOfJoining()),
                    nullToEmpty(primary.getDepartment()), formatAmount(primary.getCtc()), nullToEmpty(primary.getMaritalStatus()),
                    submissionStatus));

            Set<String> seenDepKeys = new HashSet<>();
            seenDepKeys.add(depDedupeKey(formatRelationshipForCsv(primary), primaryDisplayName));

            List<Deals> depDeals = dealsRepository.findByPrimaryIndividualId(primary.getIndividualId());
            for (Deals dep : depDeals) {
                if (dep.getIndividualId().equals(primary.getIndividualId())) {
                    continue;
                }
                if (dep.getRelationship() != null && "SELF".equalsIgnoreCase(dep.getRelationship().trim())) {
                    continue;
                }
                String rel = formatRelationshipForCsv(dep);
                String depDisplayName = csvExportDisplayNameFromDeal(dep);
                String dk = depDedupeKey(rel, depDisplayName);
                if (seenDepKeys.add(dk)) {
                    out.add(csvDataRow(empNo, rel, nullToEmpty(depDisplayName), nullToEmpty(dep.getGender()),
                            formatDobForCsv(dep.getDateOfBirth()), sums,
                            nullToEmpty(dep.getEmail()), nullToEmpty(dep.getPhone()),
                            formatDobForCsv(dep.getDateOfJoining()),
                            nullToEmpty(dep.getDepartment()), formatAmount(dep.getCtc()), nullToEmpty(dep.getMaritalStatus()),
                            submissionStatus));
                }
            }

            if (sub != null) {
                appendDependentsFromSubmissionJson(out, empNo, sums, sub.getDependents(), seenDepKeys, submissionStatus);
            }
        }
        return dedupeCsvRowsByLogicalPerson(out);
    }

    /** Stable key: prefer employee number, else individual id (one export block per employee). */
    private String primaryExportDedupeKey(Deals p) {
        if (p.getEmployeeNumber() != null && !p.getEmployeeNumber().isBlank()) {
            return p.getEmployeeNumber().trim().toLowerCase(Locale.ROOT);
        }
        return p.getIndividualId().toString();
    }

    /**
     * Drop rows that describe the same person twice (e.g. same dependent from Deals + JSON with
     * different relationship labels like Child vs Son). Keeps the first occurrence.
     * When the exported name is empty, uses the display relationship in the key so distinct
     * dependents (e.g. Son vs Daughter with blank names) are not merged.
     */
    private List<List<String>> dedupeCsvRowsByLogicalPerson(List<List<String>> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        List<List<String>> result = new ArrayList<>(rows.size());
        Set<String> seen = new HashSet<>();
        for (List<String> row : rows) {
            if (row == null || row.size() < 5) {
                result.add(row);
                continue;
            }
            String key = csvExportLogicalPersonKey(row.get(0), row.get(1), row.get(2), row.get(3), row.get(4));
            if (seen.add(key)) {
                result.add(row);
            }
        }
        return result;
    }

    private String csvExportLogicalPersonKey(String employeeId, String relationship, String employeeName,
            String gender, String dateOfBirth) {
        String normName = normalizeCsvExportCell(employeeName);
        String relKey = normName.isEmpty()
                ? normalizeCsvExportCell(relationship)
                : canonicalRelationshipForExportDedupe(relationship);
        return normalizeCsvExportCell(employeeId)
                + '\u001f'
                + relKey
                + '\u001f'
                + normName
                + '\u001f'
                + normalizeCsvExportCell(gender)
                + '\u001f'
                + normalizeCsvExportCell(dateOfBirth);
    }

    private String normalizeCsvExportCell(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Map display relationships that refer to the same dependent class to one bucket for deduplication. */
    private String canonicalRelationshipForExportDedupe(String displayRel) {
        if (displayRel == null || displayRel.isBlank()) {
            return "";
        }
        String t = displayRel.trim().toLowerCase(Locale.ROOT);
        if ("self".equals(t)) {
            return "self";
        }
        if ("spouse".equals(t) || "wife".equals(t) || "husband".equals(t)) {
            return "spouse";
        }
        if (t.contains("child") || "son".equals(t) || "daughter".equals(t) || "kid".equals(t)) {
            return "child_dependent";
        }
        if (t.contains("father") || t.contains("mother") || t.contains("parent")) {
            return "parent_dependent";
        }
        if (t.contains("in-law") || t.contains("in_law") || t.contains("inlaw")) {
            return "inlaw_dependent";
        }
        return t;
    }

    private boolean isEnrollmentPrimaryDealRow(Deals d) {
        if (d == null) {
            return false;
        }
        if (Boolean.FALSE.equals(d.getIsPrimaryMember())) {
            return false;
        }
        String r = d.getRelationship();
        String selfVal = NomineeRelationship.SELF.getValue();
        return r == null || r.isBlank() || "SELF".equalsIgnoreCase(r.trim()) || selfVal.equalsIgnoreCase(r.trim());
    }

    private List<String> csvDataRow(String employeeId, String relationship, String employeeName, String gender,
            String dateOfBirth, PlanSumsCsv sums,
            String email, String mobile, String dateOfJoining, String department, String ctc, String maritalStatus,
            String submissionStatus) {
        return List.of(employeeId, relationship, employeeName, gender, dateOfBirth,
                sums.sumInsured, sums.topupSumInsured, sums.superTopupSumInsured,
                email, mobile, dateOfJoining, department, ctc, maritalStatus, submissionStatus);
    }

    /** Value from {@link EnrollmentSubmission#getStatus()}; {@code NO_SUBMISSION} if no row exists for this employee. */
    private String csvExportSubmissionStatusLabel(EnrollmentSubmission sub) {
        if (sub != null && sub.getStatus() != null) {
            return sub.getStatus().getValue();
        }
        return "NO_SUBMISSION";
    }

    private void appendDependentsFromSubmissionJson(List<List<String>> out, String empNo, PlanSumsCsv sums,
            String dependentsJson, Set<String> seenDepKeys, String submissionStatus) {
        if (dependentsJson == null || dependentsJson.isBlank()) {
            return;
        }
        String trimmed = dependentsJson.trim();
        if ("{}".equals(trimmed) || "[]".equals(trimmed)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(dependentsJson);
            JsonNode arr = root.isArray() ? root : null;
            if (arr == null && root.isObject() && root.has("dependents") && root.get("dependents").isArray()) {
                arr = root.get("dependents");
            }
            if (arr == null || !arr.isArray()) {
                return;
            }
            for (JsonNode n : arr) {
                if (n == null || !n.isObject()) {
                    continue;
                }
                String name = dependentDisplayNameFromSubmissionJson(n);
                String relRaw = firstNonEmpty(jsonText(n, "relationship"), "Dependent");
                String actualRelJson = jsonText(n, "actualRelationship");
                String dob = formatDobJsonValue(firstNonEmpty(jsonText(n, "dateOfBirth"), jsonText(n, "date_of_birth")));
                String gender = firstNonEmpty(jsonText(n, "gender"), "");
                String rel = formatDependentRelationshipFromSubmissionJson(relRaw, actualRelJson, gender);
                String email = firstNonEmpty(jsonText(n, "email"), "");
                String mobile = firstNonEmpty(jsonText(n, "phone"), jsonText(n, "mobile"));
                String dk = depDedupeKey(rel, name);
                if (!seenDepKeys.add(dk)) {
                    continue;
                }
                out.add(csvDataRow(empNo, rel, name, gender, dob, sums, email, mobile, "", "", "", "", submissionStatus));
            }
        } catch (Exception e) {
            logger.warn("[correlationId:{}] export CSV: dependents JSON skipped: {}", MDC.get("correlationId"), e.getMessage());
        }
    }

    private String depDedupeKey(String relationship, String name) {
        String r = relationship != null ? relationship.trim().toLowerCase(Locale.ROOT) : "";
        String n = name != null ? name.trim().toLowerCase(Locale.ROOT) : "";
        return r + "|" + n;
    }

    /**
     * Maps submission dependents JSON relationship + optional actualRelationship + gender to CSV/display labels
     * (Son/Daughter instead of CHILD1 or "Child 1").
     */
    private String formatDependentRelationshipFromSubmissionJson(String relationshipRaw, String actualRelationshipJson,
            String genderRaw) {
        String raw = relationshipRaw != null ? relationshipRaw.trim() : "";
        if (raw.isEmpty()) {
            return "Dependent";
        }
        String compact = raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (!compact.matches("CHILD[1-4]")) {
            Matcher m = CHILD_NUMBER_LABEL.matcher(raw);
            if (m.find()) {
                compact = "CHILD" + m.group(1);
            }
        }
        boolean isChildCode = compact.matches("CHILD[1-4]");
        boolean isSonDaughterWord = "SON".equalsIgnoreCase(raw) || "DAUGHTER".equalsIgnoreCase(raw);
        if (isChildCode || isSonDaughterWord) {
            String ar = actualRelationshipJson != null ? actualRelationshipJson.trim() : "";
            if (!ar.isEmpty()) {
                return titleCaseRelationship(ar);
            }
            String g = genderRaw != null ? genderRaw.trim().toLowerCase(Locale.ROOT) : "";
            if ("male".equals(g)) {
                return "Son";
            }
            if ("female".equals(g)) {
                return "Daughter";
            }
            return "Child";
        }
        if ("SPOUSE".equalsIgnoreCase(raw)) {
            return "Spouse";
        }
        if ("SELF".equalsIgnoreCase(raw)) {
            return "Self";
        }
        if ("FATHER".equalsIgnoreCase(raw) || "MOTHER".equalsIgnoreCase(raw)) {
            return raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1).toLowerCase(Locale.ROOT);
        }
        return titleCaseRelationship(raw.replace('_', ' ').trim());
    }

    private String formatRelationshipForCsv(Deals d) {
        if (d == null) {
            return "";
        }
        String r = d.getRelationship();
        if (r == null || r.isBlank()) {
            return "Self";
        }
        if ("SELF".equalsIgnoreCase(r.trim()) || NomineeRelationship.SELF.getValue().equalsIgnoreCase(r.trim())) {
            return "Self";
        }
        if ("SPOUSE".equalsIgnoreCase(r.trim())) {
            return "Spouse";
        }
        if (r.toUpperCase(Locale.ROOT).startsWith("CHILD")
                || "SON".equalsIgnoreCase(r.trim())
                || "DAUGHTER".equalsIgnoreCase(r.trim())) {
            if (d.getActualRelationship() != null && !d.getActualRelationship().isBlank()) {
                return titleCaseRelationship(d.getActualRelationship().trim());
            }
            String g = d.getGender() != null ? d.getGender().trim().toLowerCase(Locale.ROOT) : "";
            if ("male".equals(g)) {
                return "Son";
            }
            if ("female".equals(g)) {
                return "Daughter";
            }
            return "Child";
        }
        if ("FATHER".equalsIgnoreCase(r.trim()) || "MOTHER".equalsIgnoreCase(r.trim())) {
            return r.substring(0, 1).toUpperCase(Locale.ROOT) + r.substring(1).toLowerCase(Locale.ROOT);
        }
        return titleCaseRelationship(r.replace('_', ' ').trim());
    }

    private String titleCaseRelationship(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String p = parts[i];
            if (p.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private String formatDobForCsv(LocalDate dob) {
        return dob == null ? "" : EXPORT_DOB.format(dob);
    }

    private String formatDobJsonValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        try {
            LocalDate d = LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
            return EXPORT_DOB.format(d);
        } catch (Exception ignored) {
            // keep as provided (e.g. already dd/MM/yy)
        }
        return t;
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }

    /** Prefer full_name; else join first + last (self-service and HR rows may split names). */
    private String csvExportDisplayNameFromDeal(Deals d) {
        if (d == null) {
            return "";
        }
        return firstNonEmpty(
                d.getFullName() != null ? d.getFullName().trim() : "",
                joinGivenAndFamilyName(d.getFirstName(), d.getLastName()));
    }

    private String joinGivenAndFamilyName(String firstName, String lastName) {
        String f = firstName != null ? firstName.trim() : "";
        String l = lastName != null ? lastName.trim() : "";
        if (f.isEmpty() && l.isEmpty()) {
            return "";
        }
        if (l.isEmpty()) {
            return f;
        }
        if (f.isEmpty()) {
            return l;
        }
        return f + " " + l;
    }

    /**
     * Self-service submissions store firstName/lastName; HR bulk upload may set fullName/name.
     */
    private String dependentDisplayNameFromSubmissionJson(JsonNode n) {
        if (n == null || !n.isObject()) {
            return "";
        }
        String direct = firstNonEmpty(jsonText(n, "fullName"), jsonText(n, "name"));
        if (!direct.isBlank()) {
            return direct.trim();
        }
        String first = firstNonEmpty(jsonText(n, "firstName"), jsonText(n, "first_name"));
        String last = firstNonEmpty(jsonText(n, "lastName"), jsonText(n, "last_name"));
        return joinGivenAndFamilyName(first, last);
    }

    private String jsonText(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return "";
        }
        JsonNode v = n.get(field);
        if (v.isTextual()) {
            return v.asText();
        }
        if (v.isNumber()) {
            return v.asText();
        }
        return "";
    }

    /** Enrollment self-service saves verified phone in submission personalDetails; Deals.phone may still be empty. */
    private String phoneFromPersonalDetailsJson(String personalDetailsJson) {
        if (personalDetailsJson == null || personalDetailsJson.isBlank()) {
            return "";
        }
        String trimmed = personalDetailsJson.trim();
        if ("{}".equals(trimmed)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(personalDetailsJson);
            if (root == null || !root.isObject()) {
                return "";
            }
            return firstNonEmpty(jsonText(root, "phone"), jsonText(root, "mobile"));
        } catch (Exception e) {
            logger.debug("[correlationId:{}] export CSV: personalDetails phone parse skipped: {}",
                    MDC.get("correlationId"), e.getMessage());
            return "";
        }
    }

    /** Self-service enrollment saves DOB in submission personalDetails; Deals.dateOfBirth may still be null. */
    private String dobFromPersonalDetailsJson(String personalDetailsJson) {
        if (personalDetailsJson == null || personalDetailsJson.isBlank()) {
            return "";
        }
        String trimmed = personalDetailsJson.trim();
        if ("{}".equals(trimmed)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(personalDetailsJson);
            if (root == null || !root.isObject()) {
                return "";
            }
            return firstNonEmpty(jsonText(root, "dateOfBirth"), jsonText(root, "date_of_birth"));
        } catch (Exception e) {
            logger.debug("[correlationId:{}] export CSV: personalDetails DOB parse skipped: {}",
                    MDC.get("correlationId"), e.getMessage());
            return "";
        }
    }

    private String emailFromPersonalDetailsJson(String personalDetailsJson) {
        if (personalDetailsJson == null || personalDetailsJson.isBlank()) {
            return "";
        }
        String trimmed = personalDetailsJson.trim();
        if ("{}".equals(trimmed)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(personalDetailsJson);
            if (root == null || !root.isObject()) {
                return "";
            }
            return jsonText(root, "email");
        } catch (Exception e) {
            logger.debug("[correlationId:{}] export CSV: personalDetails email parse skipped: {}",
                    MDC.get("correlationId"), e.getMessage());
            return "";
        }
    }

    private String sanitizeCsvFilename(String name) {
        if (name == null || name.isBlank()) {
            return "enrollment_window";
        }
        String s = name.replaceAll("[^a-zA-Z0-9._-]+", "_").replaceAll("^_+|_+$", "");
        return s.isBlank() ? "enrollment_window" : s.substring(0, Math.min(80, s.length()));
    }

    private static final class PlanSumsCsv {
        final String sumInsured;
        final String topupSumInsured;
        final String superTopupSumInsured;

        PlanSumsCsv(String sumInsured, String topupSumInsured, String superTopupSumInsured) {
            this.sumInsured = sumInsured;
            this.topupSumInsured = topupSumInsured;
            this.superTopupSumInsured = superTopupSumInsured;
        }

        static PlanSumsCsv empty() {
            return new PlanSumsCsv("", "", "");
        }
    }

    private PlanSumsCsv parsePlanSelectionsForCsv(String json) {
        if (json == null || json.isBlank()) {
            return PlanSumsCsv.empty();
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray()) {
                return PlanSumsCsv.empty();
            }
            BigDecimal gmc = null;
            BigDecimal top = null;
            BigDecimal sup = null;
            for (JsonNode n : arr) {
                if (n == null || !n.isObject()) {
                    continue;
                }
                if (n.has("opted") && !n.get("opted").asBoolean(true)) {
                    continue;
                }
                String pt = firstNonEmpty(jsonText(n, "planType"), jsonText(n, "productType"));
                if (pt.isBlank()) {
                    pt = jsonText(n, "plan_type");
                }
                if (pt.isBlank()) {
                    continue;
                }
                String upt = pt.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
                BigDecimal cov = readCoverageAmount(n);
                if (cov == null) {
                    continue;
                }
                if (upt.contains("SUPER_TOP") || "SUPER_TOP_UP".equals(upt)) {
                    sup = cov;
                } else if (upt.contains("TOP_UP") || "TOPUP".equals(upt)) {
                    top = cov;
                } else if (upt.contains("GMC") || upt.contains("GHI") || upt.contains("PARENT")) {
                    gmc = cov;
                }
            }
            return new PlanSumsCsv(formatAmount(gmc), formatAmount(top), formatAmount(sup));
        } catch (Exception e) {
            logger.warn("[correlationId:{}] export CSV: planSelections parse failed: {}", MDC.get("correlationId"), e.getMessage());
            return PlanSumsCsv.empty();
        }
    }

    private BigDecimal readCoverageAmount(JsonNode n) {
        BigDecimal v = decimalFromJson(n, "coverageAmount", "coverage_amount", "sumInsured", "sum_insured");
        return v;
    }

    private BigDecimal decimalFromJson(JsonNode n, String... keys) {
        for (String key : keys) {
            if (!n.has(key) || n.get(key).isNull()) {
                continue;
            }
            JsonNode v = n.get(key);
            if (v.isNumber()) {
                return v.decimalValue();
            }
            if (v.isTextual()) {
                try {
                    return new BigDecimal(v.asText().trim().replace(",", ""));
                } catch (Exception ignored) {
                    // next key
                }
            }
        }
        return null;
    }

    private String formatAmount(BigDecimal d) {
        if (d == null) {
            return "";
        }
        return d.stripTrailingZeros().toPlainString();
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


    private Optional<LocalDate> parseDependentDateOfBirth(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        String iso = EnrollmentUploadParserUtil.normalizeDateToIsoString(trimmed);
        String toParse = iso != null && !iso.isBlank() ? iso : trimmed;
        try {
            return Optional.of(LocalDate.parse(toParse));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Field-level checks for dependents sent as JSON (same fields as HR enrollment dependent rows).
     */
    private List<String> validateDependentEnrollmentRequests(Map<String, List<DependentEnrollmentUpdateDto>> dependentsByEmployeeId) {
        List<String> errors = new ArrayList<>();
        if (dependentsByEmployeeId == null || dependentsByEmployeeId.isEmpty()) {
            return errors;
        }
        LocalDate today = LocalDate.now();
        for (Map.Entry<String, List<DependentEnrollmentUpdateDto>> e : dependentsByEmployeeId.entrySet()) {
            String employeeId = e.getKey();
            List<DependentEnrollmentUpdateDto> rows = e.getValue();
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            if (employeeId == null || employeeId.isBlank()) {
                errors.add("dependentsByEmployeeId contains an entry with a missing employee ID key");
                continue;
            }
            for (int i = 0; i < rows.size(); i++) {
                DependentEnrollmentUpdateDto d = rows.get(i);
                int n = i + 1;
                String prefix = "Employee " + employeeId + " dependent " + n + ": ";
                if (d == null) {
                    errors.add(prefix + "Invalid dependent entry");
                    continue;
                }
                if (d.getRelationship() == null || d.getRelationship().isBlank()) {
                    errors.add(prefix + "Relationship is required");
                }
                if (d.getDateOfBirth() == null || d.getDateOfBirth().isBlank()) {
                    errors.add(prefix + "Date of birth is required");
                } else {
                    Optional<LocalDate> dobOpt = parseDependentDateOfBirth(d.getDateOfBirth());
                    if (dobOpt.isEmpty()) {
                        errors.add(prefix + "Invalid date of birth");
                    } else if (dobOpt.get().isAfter(today)) {
                        errors.add(prefix + "Date of birth cannot be in the future");
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Parent-GMC overlap bypass for JSON dependents (mirrors file upload {@code getParentCoverageEmployeeIdsForOverlapBypass}).
     */
    private Set<String> getParentCoverageEmployeeIdsForOverlapBypassFromDependentsMap(
            UUID organizationId,
            Map<String, List<DependentEnrollmentUpdateDto>> dependentsByEmployeeId,
            List<Policy> activePolicies) {
        if (dependentsByEmployeeId == null || dependentsByEmployeeId.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> activeParentPolicyIds = activePolicies.stream()
                .filter(this::isParentCoveragePolicy)
                .map(Policy::getPolicyId)
                .filter(Objects::nonNull)
                .toList();
        if (activeParentPolicyIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> employeeIds = new HashSet<>();
        dependentsByEmployeeId.forEach((employeeId, dependentRows) -> {
            if (employeeId == null || employeeId.isBlank() || dependentRows == null || dependentRows.isEmpty()) {
                return;
            }
            boolean hasParentRow = dependentRows.stream()
                    .filter(Objects::nonNull)
                    .map(DependentEnrollmentUpdateDto::getRelationship)
                    .anyMatch(this::isParentCoverageRelationship);
            if (!hasParentRow) {
                return;
            }
            Optional<Deals> existingSelfDeal = dealsRepository
                    .findByEmployeeNumberAndOrganizationIdAndRelationship(
                            employeeId,
                            organizationId,
                            NomineeRelationship.SELF.getValue());
            if (existingSelfDeal.isEmpty()) {
                employeeIds.add(employeeId);
                return;
            }
            UUID individualId = existingSelfDeal.get().getIndividualId();
            if (individualId == null) {
                employeeIds.add(employeeId);
                return;
            }
            boolean alreadyInActiveParentPolicy = hasExistingParentDependent(individualId) || !employeePolicyMapRepository
                    .findByIndividualIdAndPolicyIdInAndStatus(
                            individualId,
                            activeParentPolicyIds,
                            POLICY_MAP_STATUS_ACTIVE)
                    .isEmpty();
            if (!alreadyInActiveParentPolicy) {
                employeeIds.add(employeeId);
            }
        });
        return employeeIds;
    }

    /**
     * Renewal / dual-policy rule for dependents already on file: same logic as {@link #validateExistingEmployeesForRenewal}
     * but resolves dependents via {@link IDealsRepository#findByEmployeeNumberAndRelationshipAndOrganizationId}.
     */
    private List<String> validateExistingDependentsForRenewal(
            UUID organizationId,
            Map<String, List<DependentEnrollmentUpdateDto>> dependentsByEmployeeId,
            Set<String> overlapBypassEmployeeIds) {
        List<String> errors = new ArrayList<>();
        if (dependentsByEmployeeId == null || dependentsByEmployeeId.isEmpty()) {
            return errors;
        }

        Optional<LocalDate> newPolicyStartOpt = getNewPolicyStartDateForOrganization(organizationId);

        for (Map.Entry<String, List<DependentEnrollmentUpdateDto>> e : dependentsByEmployeeId.entrySet()) {
            String employeeId = e.getKey();
            if (employeeId == null || employeeId.isBlank()) {
                continue;
            }
            List<DependentEnrollmentUpdateDto> rows = e.getValue();
            if (rows == null) {
                continue;
            }
            for (DependentEnrollmentUpdateDto dto : rows) {
                if (dto == null || dto.getRelationship() == null || dto.getRelationship().isBlank()) {
                    continue;
                }
                String relationship = dto.getRelationship().trim();
                if (overlapBypassEmployeeIds != null && overlapBypassEmployeeIds.contains(employeeId)
                        && isParentCoverageRelationship(relationship)) {
                    continue;
                }
                Optional<Deals> existingOpt = dealsRepository.findByEmployeeNumberAndRelationshipAndOrganizationId(
                        employeeId, relationship, organizationId);
                if (existingOpt.isEmpty()) {
                    continue;
                }
                Deals existing = existingOpt.get();
                UUID individualId = existing.getIndividualId();

                if (newPolicyStartOpt.isEmpty()) {
                    errors.add("Dependent " + relationship + " for employee " + employeeId + " already exists.");
                    continue;
                }
                LocalDate newPolicyStart = newPolicyStartOpt.get();
                List<EmployeePolicyMap> mappings = employeePolicyMapRepository.findByIndividualIdAndOrganizationIdAndStatus(
                        individualId, organizationId, POLICY_MAP_STATUS_ACTIVE);
                if (mappings.isEmpty()) {
                    continue;
                }
                LocalDate latestEnd = mappings.stream()
                        .map(EmployeePolicyMap::getEffectiveTo)
                        .filter(Objects::nonNull)
                        .max(LocalDate::compareTo)
                        .orElse(null);
                if (latestEnd == null) {
                    errors.add("Dependent " + relationship + " for employee " + employeeId
                            + " cannot be in two policies; new policy start must be after current policy end.");
                    continue;
                }
                if (!newPolicyStart.isAfter(latestEnd)) {
                    errors.add("Dependent " + relationship + " for employee " + employeeId
                            + " cannot be in two policies; new policy start must be after current policy end.");
                }
            }
        }
        return errors;
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
     * Detect "dependent-only" uploads where SELF rows are present only as anchors for dependent additions.
     * In that case, skip renewal overlap checks intended for new/renewal employee uploads.
     */
    private boolean isDependentsOnlyUpload(EnrollmentUploadParserUtil.EnrollmentParseResult parseResult) {
        if (parseResult == null || parseResult.getDependentRowsByEmployeeId().isEmpty()) {
            return false;
        }

        List<SelfEmployeeEnrollmentRequestDto> selfRows = parseResult.getSelfRows();
        if (selfRows == null || selfRows.isEmpty()) {
            return false;
        }

        Set<String> dependentEmployeeIds = parseResult.getDependentRowsByEmployeeId().keySet();
        return selfRows.stream()
                .map(SelfEmployeeEnrollmentRequestDto::getEmployeeId)
                .filter(id -> id != null && !id.isBlank())
                .allMatch(dependentEmployeeIds::contains);
    }

    /**
     * API payload fallback for dependent additions where the client sends only
     * selfEmployeeEnrollmentRequestDtos (without relationship column).
     * Heuristic: duplicate employee IDs with at least one "anchor" row having email.
     */
    private boolean isDependentsOnlyRequestPayload(List<SelfEmployeeEnrollmentRequestDto> requestDtos) {
        if (requestDtos == null || requestDtos.isEmpty()) {
            return false;
        }

        Map<String, Long> countsByEmployeeId = requestDtos.stream()
                .map(SelfEmployeeEnrollmentRequestDto::getEmployeeId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        boolean hasDuplicateEmployeeIds = countsByEmployeeId.values().stream().anyMatch(count -> count > 1);
        if (!hasDuplicateEmployeeIds) {
            return false;
        }

        // Expect at least one anchor (self) row to carry a non-empty email.
        return requestDtos.stream().anyMatch(dto -> dto.getEmail() != null && !dto.getEmail().isBlank());
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
    private List<String> validateExistingEmployeesForRenewal(
            UUID organizationId,
            List<SelfEmployeeEnrollmentRequestDto> requestDtos,
            Set<String> overlapBypassEmployeeIds) {
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
            if (overlapBypassEmployeeIds != null && overlapBypassEmployeeIds.contains(employeeId)) {
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

    private boolean isParentOnlyDependentUpload(
            UUID organizationId,
            EnrollmentUploadParserUtil.EnrollmentParseResult parseResult,
            List<Policy> activePolicies) {
        if (parseResult == null || parseResult.hasSelfRows()) return false;
        if (parseResult.getDependentRowsByEmployeeId() == null || parseResult.getDependentRowsByEmployeeId().isEmpty()) {
            return false;
        }
        boolean hasActiveParentGmc = activePolicies != null && activePolicies.stream()
                .anyMatch(this::isParentCoveragePolicy);
        if (!hasActiveParentGmc) return false;
        return parseResult.getDependentRowsByEmployeeId().values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(EnrollmentUploadParserUtil.DependentRow::getRelationship)
                .allMatch(this::isParentCoverageRelationship);
    }

    private List<String> validateParentOnlyDependentUploadEmployees(
            UUID organizationId,
            EnrollmentUploadParserUtil.EnrollmentParseResult parseResult,
            List<Policy> activePolicies) {
        List<String> errors = new ArrayList<>();
        if (parseResult == null || parseResult.getDependentRowsByEmployeeId() == null) {
            return errors;
        }
        List<Long> activeParentPolicyIds = activePolicies.stream()
                .filter(this::isParentCoveragePolicy)
                .map(Policy::getPolicyId)
                .filter(Objects::nonNull)
                .toList();
        String selfRelationship = NomineeRelationship.SELF.getValue();
        for (String employeeId : parseResult.getDependentRowsByEmployeeId().keySet()) {
            if (employeeId == null || employeeId.isBlank()) continue;
            Optional<Deals> selfDealOpt = dealsRepository.findByEmployeeNumberAndOrganizationIdAndRelationship(
                    employeeId, organizationId, selfRelationship);
            if (selfDealOpt.isEmpty()) {
                errors.add("Employee " + employeeId + " not found. Cannot add parent dependents without primary employee (Self).");
                continue;
            }
            Deals selfDeal = selfDealOpt.get();
            if (selfDeal.getIndividualId() == null || activeParentPolicyIds.isEmpty()) {
                continue;
            }
            boolean alreadyHasParentCoverage =
                    hasExistingParentDependent(selfDeal.getIndividualId())
                    || !employeePolicyMapRepository.findByIndividualIdAndPolicyIdInAndStatus(
                            selfDeal.getIndividualId(),
                            activeParentPolicyIds,
                            POLICY_MAP_STATUS_ACTIVE).isEmpty();
            if (alreadyHasParentCoverage) {
                errors.add("Employee " + employeeId + " cannot be in two policies; new policy start must be after current policy end.");
            }
        }
        return errors;
    }

    private Set<String> getParentCoverageEmployeeIdsForOverlapBypass(
            UUID organizationId,
            EnrollmentUploadParserUtil.EnrollmentParseResult parseResult,
            List<Policy> activePolicies) {
        if (parseResult == null || parseResult.getDependentRowsByEmployeeId() == null
                || parseResult.getDependentRowsByEmployeeId().isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> activeParentPolicyIds = activePolicies.stream()
                .filter(this::isParentCoveragePolicy)
                .map(Policy::getPolicyId)
                .filter(Objects::nonNull)
                .toList();
        if (activeParentPolicyIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> employeeIds = new HashSet<>();
        parseResult.getDependentRowsByEmployeeId().forEach((employeeId, dependentRows) -> {
            if (employeeId == null || employeeId.isBlank() || dependentRows == null || dependentRows.isEmpty()) {
                return;
            }
            boolean hasParentRow = dependentRows.stream()
                    .map(EnrollmentUploadParserUtil.DependentRow::getRelationship)
                    .anyMatch(this::isParentCoverageRelationship);
            if (!hasParentRow) {
                return;
            }

            Optional<Deals> existingSelfDeal = dealsRepository
                    .findByEmployeeNumberAndOrganizationIdAndRelationship(
                            employeeId,
                            organizationId,
                            NomineeRelationship.SELF.getValue());
            if (existingSelfDeal.isEmpty()) {
                employeeIds.add(employeeId);
                return;
            }
            UUID individualId = existingSelfDeal.get().getIndividualId();
            if (individualId == null) {
                employeeIds.add(employeeId);
                return;
            }

            boolean alreadyInActiveParentPolicy = hasExistingParentDependent(individualId) || !employeePolicyMapRepository
                    .findByIndividualIdAndPolicyIdInAndStatus(
                            individualId,
                            activeParentPolicyIds,
                            POLICY_MAP_STATUS_ACTIVE)
                    .isEmpty();
            if (!alreadyInActiveParentPolicy) {
                employeeIds.add(employeeId);
            }
        });
        return employeeIds;
    }

    private boolean isParentCoverageRelationship(String relationship) {
        if (relationship == null || relationship.isBlank()) {
            return false;
        }
        String normalized = relationship.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return "PARENT".equals(normalized)
                || "FATHER".equals(normalized)
                || "MOTHER".equals(normalized)
                || "PARENT_IN_LAW".equals(normalized)
                || "FATHER_IN_LAW".equals(normalized)
                || "MOTHER_IN_LAW".equals(normalized);
    }

    private boolean isParentCoveragePolicy(Policy policy) {
        if (policy == null) return false;
        return policy.getProductType() == ProductType.PARENT_GMC
                || policy.getCoverageType() == CoverageType.PARENT;
    }

    private boolean hasExistingParentDependent(UUID primaryIndividualId) {
        if (primaryIndividualId == null) return false;
        return dealsRepository.findByPrimaryIndividualId(primaryIndividualId).stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getRelationship() != null)
                .filter(d -> d.getStatus() != AccountStatus.INACTIVE)
                .map(Deals::getRelationship)
                .anyMatch(this::isParentCoverageRelationship);
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
                    existing.setCtc(requestDto.getCtc());
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
                    employee.setCtc(requestDto.getCtc());
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
