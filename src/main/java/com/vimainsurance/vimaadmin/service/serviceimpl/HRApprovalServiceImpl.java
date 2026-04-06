package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.ApprovalRequest;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.BulkApprovalRequest;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EnrollmentOrganizationPolicyDto;
import com.vimainsurance.vimaadmin.dto.RejectionRequest;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.SubmissionDetailDto;
import com.vimainsurance.vimaadmin.dto.SubmissionListItemDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.DealEndorsement;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Nominee;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.AccountType;
import com.vimainsurance.vimaadmin.enums.EndorsementSource;
import com.vimainsurance.vimaadmin.enums.EndorsementType;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import com.vimainsurance.vimaadmin.enums.ProductType;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IInsuranceProviderRepository;
import com.vimainsurance.vimaadmin.repository.INomineeRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;
import com.vimainsurance.vimaadmin.service.IHRApprovalService;
import com.vimainsurance.vimaadmin.service.IPayrollSchedulePopulationService;
import com.vimainsurance.vimaadmin.specification.EnrollmentSubmissionSpecification;
import com.vimainsurance.vimaadmin.audit.AuditContextSupplier;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;
import com.vimainsurance.vimaadmin.util.OrganizationAccessHelper;
import com.vimainsurance.vimaadmin.util.TenantContext;
import com.vimainsurance.vimaadmin.util.TransactionUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HRApprovalServiceImpl implements IHRApprovalService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int BATCH_SIZE = 500;

    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;
    @Autowired
    private IDealsRepository dealsRepository;
    @Autowired
    private IEnrollmentInvitationRepository enrollmentInvitationRepository;
    @Autowired
    private IAdminUserRepository adminUserRepository;
    @Autowired
    private IEmailService emailService;
    @Autowired
    private JwtUserExtractor jwtUserExtractor;
    @Autowired(required = false)
    private OrganizationAccessHelper organizationAccessHelper;
    @Autowired
    private INomineeRepository nomineeRepository;
    @Autowired
    private IEndorsementRepository endorsementRepository;
    @Autowired
    private IDealEndorsementRepository dealEndorsementRepository;
    @Autowired
    private IDocumentService documentService;
    @Autowired
    private IPolicyRepository policyRepository;
    @Autowired
    private IInsuranceProviderRepository insuranceProviderRepository;
    @Autowired(required = false)
    private IEmployeePolicyMapService employeePolicyMapService;
    @Autowired(required = false)
    private IPayrollSchedulePopulationService payrollSchedulePopulationService;

    @Override
    public ResponseEntity<ResponseDto<Page<SubmissionListItemDto>>> getEnrollments(
            String companyId,
            String status,
            String search,
            LocalDateTime submittedFrom,
            LocalDateTime submittedTo,
            Pageable pageable) {
        BaseResponse<Page<SubmissionListItemDto>> responseObj = new BaseResponse<>();
        try {
            List<UUID> organizationIds = resolveOrganizationIds(companyId);
            if (organizationIds == null || organizationIds.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No organization access"));
            }

            Sort sort = pageable.getSort().isSorted()
                    ? pageable.getSort()
                    : Sort.by(Sort.Direction.DESC, "submittedAt");
            Pageable effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

            var spec = EnrollmentSubmissionSpecification.withFilters(
                    organizationIds, status, search, submittedFrom, submittedTo);
            Page<EnrollmentSubmission> page = enrollmentSubmissionRepository.findAll(spec, effectivePageable);
            Page<SubmissionListItemDto> dtoPage = page.map(this::toListItemDto);
            ResponseDto<Page<SubmissionListItemDto>> response = responseObj.formSuccessResponse(
                    "Enrollments", dtoPage, dtoPage.getTotalElements());
            return responseObj.render(response);
        } catch (Exception e) {
            log.error("[correlationId:{}] getEnrollments error: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to list enrollments"));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<SubmissionDetailDto>> getEnrollmentDetail(UUID id) {
        BaseResponse<SubmissionDetailDto> responseObj = new BaseResponse<>();
        try {
            Optional<EnrollmentSubmission> opt = enrollmentSubmissionRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Enrollment submission not found"));
            }
            EnrollmentSubmission sub = opt.get();
            UUID orgId = sub.getEmployee() != null && sub.getEmployee().getOrganization() != null
                    ? sub.getEmployee().getOrganization().getOrganizationId()
                    : null;
            if (orgId == null) {
                return responseObj.render(responseObj.formErrorResponse(403, "Submission has no organization"));
            }
            if (organizationAccessHelper != null) { organizationAccessHelper.validateAndSetContext(orgId); } else if (jwtUserExtractor != null) { jwtUserExtractor.validateOrganizationAccess(orgId); AuditContextSupplier.setOrganizationId(orgId); }

            SubmissionDetailDto dto = toDetailDto(sub);
            return responseObj.render(responseObj.formSuccessResponse("Enrollment detail", dto));
        } catch (Exception e) {
            log.error("[correlationId:{}] getEnrollmentDetail error: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get enrollment detail"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_submissions", entityType = "ENROLLMENT_SUBMISSION", action = "APPROVE")
    public ResponseEntity<ResponseDto<SubmissionDetailDto>> approve(UUID id, ApprovalRequest request) {
        BaseResponse<SubmissionDetailDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentSubmission sub = enrollmentSubmissionRepository.findById(id).orElse(null);
            if (sub == null) {
                return responseObj.render(responseObj.formErrorResponse(404, "Enrollment submission not found"));
            }
            UUID orgId = sub.getEmployee() != null && sub.getEmployee().getOrganization() != null
                    ? sub.getEmployee().getOrganization().getOrganizationId()
                    : null;
            if (orgId == null) {
                return responseObj.render(responseObj.formErrorResponse(403, "Submission has no organization"));
            }
            if (organizationAccessHelper != null) { organizationAccessHelper.validateAndSetContext(orgId); } else if (jwtUserExtractor != null) { jwtUserExtractor.validateOrganizationAccess(orgId); AuditContextSupplier.setOrganizationId(orgId); }
            if (sub.getStatus() != EnrollementStatus.SUBMITTED) {
                return responseObj.render(responseObj.formErrorResponse(400,
                        "Only submitted enrollments can be approved; current status: " + sub.getStatus()));
            }

            UUID reviewerId = jwtUserExtractor.getCurrentUserId();
            AdminUser reviewer = reviewerId != null ? adminUserRepository.findById(reviewerId).orElse(null) : null;

            sub.setStatus(EnrollementStatus.APPROVED);
            sub.setReviewedBy(reviewer);
            sub.setReviewedAt(LocalDateTime.now());
            enrollmentSubmissionRepository.save(sub);

            updateDealEnrollmentStatusForSubmission(id, EnrollementStatus.APPROVED);

            if (employeePolicyMapService != null) {
                employeePolicyMapService.createMappingsFromEnrollmentSubmission(id);
            }
            if (payrollSchedulePopulationService != null) {
                payrollSchedulePopulationService.populateFromEnrollmentSubmission(id);
            }

            sendApprovalEmail(sub);

            SubmissionDetailDto dto = toDetailDto(enrollmentSubmissionRepository.findById(id).orElse(sub));

            EnrollmentInvitation inv = sub.getInvitation();
            if (inv != null) {
                inv.setStatus(EnrollementStatus.COMPLETED);
                enrollmentInvitationRepository.save(inv);
            }

            return responseObj.render(responseObj.formSuccessResponse("Enrollment approved", dto));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            log.error("[correlationId:{}] approve error: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Approval failed"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_submissions", entityType = "ENROLLMENT_SUBMISSION", action = "UPDATE")
    public ResponseEntity<ResponseDto<SubmissionDetailDto>> reject(UUID id, RejectionRequest request) {
        BaseResponse<SubmissionDetailDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentSubmission sub = enrollmentSubmissionRepository.findById(id).orElse(null);
            if (sub == null) {
                return responseObj.render(responseObj.formErrorResponse(404, "Enrollment submission not found"));
            }
            UUID orgId = sub.getEmployee() != null && sub.getEmployee().getOrganization() != null
                    ? sub.getEmployee().getOrganization().getOrganizationId()
                    : null;
            if (orgId == null) {
                return responseObj.render(responseObj.formErrorResponse(403, "Submission has no organization"));
            }
            if (organizationAccessHelper != null) { organizationAccessHelper.validateAndSetContext(orgId); } else if (jwtUserExtractor != null) { jwtUserExtractor.validateOrganizationAccess(orgId); AuditContextSupplier.setOrganizationId(orgId); }
            if (sub.getStatus() != EnrollementStatus.SUBMITTED) {
                return responseObj.render(responseObj.formErrorResponse(400,
                        "Only submitted enrollments can be rejected; current status: " + sub.getStatus()));
            }

            UUID reviewerId = jwtUserExtractor.getCurrentUserId();
            AdminUser reviewer = reviewerId != null ? adminUserRepository.findById(reviewerId).orElse(null) : null;

            sub.setStatus(EnrollementStatus.REJECTED);
            sub.setRejectionReason(request.getRejectionReason());
            sub.setReviewedBy(reviewer);
            sub.setReviewedAt(LocalDateTime.now());
            enrollmentSubmissionRepository.save(sub);

            updateDealEnrollmentStatusForSubmission(id, EnrollementStatus.REJECTED);

            EnrollmentInvitation inv = sub.getInvitation();
            if (inv != null) {
                inv.setStatus(request.isReopenInvitation() ? EnrollementStatus.SENT : EnrollementStatus.REJECTED);
                enrollmentInvitationRepository.save(inv);
            }

            sendRejectionEmail(sub, request.getRejectionReason());

            SubmissionDetailDto dto = toDetailDto(enrollmentSubmissionRepository.findById(id).orElse(sub));
            return responseObj.render(responseObj.formSuccessResponse("Enrollment rejected", dto));
        } catch (Exception e) {
            log.error("[correlationId:{}] reject error: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Rejection failed"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_submissions", entityType = "ENROLLMENT_SUBMISSION", action = "BULK_APPROVE")
    public ResponseEntity<ResponseDto<List<SubmissionListItemDto>>> bulkApprove(BulkApprovalRequest request) {
        BaseResponse<List<SubmissionListItemDto>> responseObj = new BaseResponse<>();
        try {
            UUID reviewerId = jwtUserExtractor.getCurrentUserId();
            AdminUser reviewer = reviewerId != null ? adminUserRepository.findById(reviewerId).orElse(null) : null;

            List<SubmissionListItemDto> approved = new ArrayList<>();
            for (UUID id : request.getSubmissionIds()) {
                EnrollmentSubmission sub = enrollmentSubmissionRepository.findById(id).orElse(null);
                if (sub == null) {
                    throw new IllegalArgumentException("Enrollment submission not found: " + id);
                }
                UUID orgId = sub.getEmployee() != null && sub.getEmployee().getOrganization() != null
                        ? sub.getEmployee().getOrganization().getOrganizationId()
                        : null;
                if (orgId == null) {
                    throw new IllegalArgumentException("Submission " + id + " has no organization");
                }
                if (organizationAccessHelper != null) { organizationAccessHelper.validateAndSetContext(orgId); } else if (jwtUserExtractor != null) { jwtUserExtractor.validateOrganizationAccess(orgId); AuditContextSupplier.setOrganizationId(orgId); }
                if (sub.getStatus() != EnrollementStatus.SUBMITTED) {
                    throw new IllegalArgumentException("Submission " + id + " is not in SUBMITTED status");
                }

                sub.setStatus(EnrollementStatus.APPROVED);
                sub.setReviewedBy(reviewer);
                sub.setReviewedAt(LocalDateTime.now());
                enrollmentSubmissionRepository.save(sub);

                updateDealEnrollmentStatusForSubmission(id, EnrollementStatus.APPROVED);

                sendApprovalEmail(sub);
                approved.add(toListItemDto(sub));
            }

            return responseObj.render(responseObj.formSuccessResponse("Bulk approval completed", approved, approved.size()));
        } catch (IllegalArgumentException e) {
            log.warn("[correlationId:{}] bulkApprove validation: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("[correlationId:{}] bulkApprove error: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Bulk approval failed"));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "cpc", tableName = "enrollment_windows", entityType = "ENROLLMENT_WINDOW", action = "UPDATE")
    public ResponseEntity<ResponseDto<String>> finalizeEnrollmentWindow(UUID windowId) {
        BaseResponse<String> responseObj = new BaseResponse<>();

        List<EnrollmentSubmission> submissions = enrollmentSubmissionRepository.findAllByEnrollmentWindow_Id(windowId);
        long pending = submissions.stream()
                .filter(s -> s.getStatus() == EnrollementStatus.SUBMITTED || s.getStatus() == EnrollementStatus.DRAFT)
                .count();
        if (pending > 0) {
            return responseObj.render(responseObj.formErrorResponse(400,
                    "Cannot close window: " + pending + " submission(s) must be approved or rejected first."));
        }

        List<EnrollmentSubmission> approved = submissions.stream()
                .filter(s -> s.getStatus() == EnrollementStatus.APPROVED)
                .toList();
        log.info("[correlationId:{}] finalizeEnrollmentWindow window {}: {} submissions, {} approved",
                MDC.get("correlationId"), windowId, submissions.size(), approved.size());

        UUID splitGroupId = UUID.randomUUID();

        // Phase 1: process each submission (employee data, dependents, nominees) and collect all deals per policy
        Map<Long, List<Deals>> dealsByPolicy = new LinkedHashMap<>();
        Map<Long, Policy> policyCache = new LinkedHashMap<>();
        Organization windowOrg = null;

        for (EnrollmentSubmission sub : approved) {
            Deals employee = sub.getEmployee();
            if (employee == null) {
                throw new IllegalStateException(
                        "Cannot finalize: approved submission " + sub.getId() + " has no linked employee");
            }

            updateEmployeeFromPersonalDetails(employee, sub.getPersonalDetails());
            dealsRepository.save(employee);

            List<Deals> newDependents = createDependentsFromJson(sub, employee, sub.getDependents());
            saveDealsInBatches(newDependents);

            List<Nominee> nominees = createNomineesFromJson(sub, employee, sub.getNomineeData());
            saveNomineesInBatches(nominees);

            Organization org = (employee.getOrganization() != null) ? employee.getOrganization()
                    : sub.getEnrollmentWindow() != null ? sub.getEnrollmentWindow().getOrganization() : null;
            if (org == null) {
                throw new IllegalStateException(
                        "Cannot finalize: submission " + sub.getId() + " has no organization");
            }
            if (windowOrg == null) {
                windowOrg = org;
            }

            Set<Long> policyIds = extractPolicyIdsFromPlanSelections(sub.getPlanSelections(), org);
            if (policyIds.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot finalize: no policies resolved for submission " + sub.getId()
                        + " (org " + org.getOrganizationId() + ")");
            }

            List<Deals> submissionDependents = dealsRepository.findByEnrollmentSubmission_Id(sub.getId());
            List<Deals> submissionDeals = new ArrayList<>();
            submissionDeals.add(employee);
            submissionDeals.addAll(submissionDependents);

            for (Long policyId : policyIds) {
                dealsByPolicy.computeIfAbsent(policyId, k -> new ArrayList<>()).addAll(submissionDeals);
                policyCache.computeIfAbsent(policyId, k -> policyRepository.findById(policyId).orElse(null));
            }
        }

        // Phase 2: create ONE endorsement per policy, aggregate all employees + dependents
        String username = jwtUserExtractor.extractCurrentUsername();
        AdminUser uploadedBy = adminUserRepository.findByUsername(username).orElse(null);
        EnrollmentWindows window = approved.isEmpty() ? null : approved.get(0).getEnrollmentWindow();
        Endorsement primaryEndorsement = null;

        for (Map.Entry<Long, List<Deals>> entry : dealsByPolicy.entrySet()) {
            Long policyId = entry.getKey();
            List<Deals> allDeals = entry.getValue();
            Policy policy = policyCache.get(policyId);
            ProductType pt = policy != null ? policy.getProductType() : null;

            Endorsement endorsement = new Endorsement();
            if (uploadedBy != null) endorsement.setUploadedBy(uploadedBy);
            endorsement.setOrganization(windowOrg);
            endorsement.setEnrollmentWindow(window);
            endorsement.setEndorsementType(EndorsementType.ADDITION);
            endorsement.setSource(EndorsementSource.SELF_ENROLLMENT);
            endorsement.setStatus(AccountStatus.PENDING_APPROVAL);
            endorsement.setPolicy(policy);
            endorsement.setSplitGroupId(splitGroupId);
            endorsement.setCreatedAt(LocalDateTime.now());
            endorsement.setUpdatedAt(LocalDateTime.now());
            endorsement.setSubmissionCount(approved.size());

            int employees = (int) allDeals.stream()
                    .filter(d -> d.getRelationship() == null || "SELF".equalsIgnoreCase(d.getRelationship()) || "EMPLOYEE".equalsIgnoreCase(d.getRelationship()))
                    .count();
            if (pt == ProductType.GPA || pt == ProductType.GTL || pt == ProductType.TOP_UP || pt == ProductType.SUPER_TOP_UP) {
                endorsement.setTotalEmployees(employees);
                endorsement.setTotalDependents(0);
            } else if (pt == ProductType.PARENT_GMC) {
                int parents = (int) allDeals.stream().filter(d -> isParentDealRelationship(d.getRelationship())).count();
                endorsement.setTotalEmployees(employees);
                endorsement.setTotalDependents(parents);
            } else {
                int depCount = (int) allDeals.stream()
                        .filter(d -> d.getRelationship() != null && !"SELF".equalsIgnoreCase(d.getRelationship()) && !"EMPLOYEE".equalsIgnoreCase(d.getRelationship()))
                        .filter(d -> !isParentDealRelationship(d.getRelationship()))
                        .count();
                endorsement.setTotalEmployees(employees);
                endorsement.setTotalDependents(depCount);
            }

            if (primaryEndorsement != null) {
                endorsement.setParentEndorsement(primaryEndorsement);
            }
            Endorsement savedEndorsement = endorsementRepository.save(endorsement);
            if (primaryEndorsement == null) {
                primaryEndorsement = savedEndorsement;
            }

            List<Deals> eligibleDeals = filterDealsForPolicyType(allDeals, pt);
            List<DealEndorsement> deToSave = new ArrayList<>();
            for (Deals d : eligibleDeals) {
                if (d.getIndividualId() != null && !dealEndorsementRepository.existsByDeal_IndividualIdAndEndorsement_EndorsementId(
                        d.getIndividualId(), savedEndorsement.getEndorsementId())) {
                    DealEndorsement de = new DealEndorsement();
                    de.setDeal(d);
                    de.setEndorsement(savedEndorsement);
                    deToSave.add(de);
                }
            }
            if (!deToSave.isEmpty()) {
                dealEndorsementRepository.saveAll(deToSave);
            }
        }

        // Phase 3: link submissions + employees to the primary endorsement and mark COMPLETED
        for (EnrollmentSubmission sub : approved) {
            if (primaryEndorsement != null) {
                sub.setEndorsement(primaryEndorsement);
                Deals emp = sub.getEmployee();
                if (emp != null) {
                    emp.setEndorsementId(primaryEndorsement.getEndorsementId());
                    dealsRepository.save(emp);
                }
                List<Deals> deps = dealsRepository.findByEnrollmentSubmission_Id(sub.getId());
                for (Deals d : deps) {
                    d.setEndorsementId(primaryEndorsement.getEndorsementId());
                }
                if (!deps.isEmpty()) {
                    saveDealsInBatches(deps);
                }
            }
            sub.setStatus(EnrollementStatus.COMPLETED);
            enrollmentSubmissionRepository.save(sub);
        }

        List<EnrollmentInvitation> invitations = enrollmentInvitationRepository.findAllByEnrollmentWindow_Id(windowId);
        int expiredCount = 0;
        for (EnrollmentInvitation inv : invitations) {
            if (inv.getStatus() != EnrollementStatus.COMPLETED) {
                inv.setStatus(EnrollementStatus.EXPIRED);
                enrollmentInvitationRepository.save(inv);
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.info("[correlationId:{}] finalizeEnrollmentWindow window {}: updated {} invitation(s) to EXPIRED",
                    MDC.get("correlationId"), windowId, expiredCount);
        }

        return responseObj.render(responseObj.formSuccessResponse("Window finalized successfully"));
    }

    /**
     * Resolve organization IDs for access control. HR_ADMIN sees only their company (from tenant).
     * If companyId is provided, it must be in the tenant's allowed list; otherwise all allowed orgs are used.
     * Falls back to JWT claims (organization_ids / organizations) when TenantContext has none (e.g. filter order).
     */
    private List<UUID> resolveOrganizationIds(String companyId) {
        Map<String, List<String>> tenantMap = TenantContext.getCurrentTenant();
        List<String> allowed = tenantMap != null ? tenantMap.get("organizationIds") : null;
        if (allowed == null) {
            allowed = new ArrayList<>();
        }
        if (allowed.isEmpty() && jwtUserExtractor != null) {
            List<String> fromJwt = jwtUserExtractor.getCurrentOrganizations();
            if (fromJwt != null && !fromJwt.isEmpty()) {
                allowed = new ArrayList<>(fromJwt);
            } else {
                UUID jwtCompanyId = jwtUserExtractor.getCurrentCompanyId();
                if (jwtCompanyId != null) {
                    allowed = List.of(jwtCompanyId.toString());
                }
            }
        }
        List<String> orgIds = new ArrayList<>();
        if (companyId != null && !companyId.trim().isEmpty()) {
            String trimmed = companyId.trim();
            if (allowed.isEmpty() || allowed.contains(trimmed)) {
                orgIds.add(trimmed);
            } else {
                // HR_ADMIN may only filter by their company; requested companyId not in allowed list
                return new ArrayList<>();
            }
        }
        if (orgIds.isEmpty()) {
            orgIds = new ArrayList<>(allowed);
        }
        List<UUID> result = new ArrayList<>();
        for (String s : orgIds) {
            try {
                result.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // skip invalid
            }
        }
        return result;
    }

    private SubmissionListItemDto toListItemDto(EnrollmentSubmission sub) {
        Deals emp = sub.getEmployee();
        EnrollmentWindows window = sub.getEnrollmentWindow();
        Organization org = (emp != null && emp.getOrganization() != null) ? emp.getOrganization() : null;

        return SubmissionListItemDto.builder()
                .id(sub.getId())
                .referenceNumber(sub.getReferenceNumber())
                .status(sub.getStatus() != null ? sub.getStatus().getValue() : null)
                .employeeName(emp != null ? emp.getFullName() : null)
                .employeeEmail(emp != null ? emp.getEmail() : null)
                .employeeNumber(emp != null ? emp.getEmployeeNumber() : null)
                .employeeId(emp != null ? emp.getIndividualId() : null)
                .windowName(window != null ? window.getName() : null)
                .windowId(window != null ? window.getId() : null)
                .organizationName(org != null ? org.getOrganizationName() : null)
                .organizationId(org != null ? org.getOrganizationId() : null)
                .submittedAt(sub.getSubmittedAt())
                .createdAt(sub.getCreatedAt())
                .build();
    }

    private SubmissionDetailDto toDetailDto(EnrollmentSubmission sub) {
        Deals emp = sub.getEmployee();
        EnrollmentWindows window = sub.getEnrollmentWindow();
        Organization org = (emp != null && emp.getOrganization() != null) ? emp.getOrganization() : null;
        if (org == null && window != null && window.getOrganization() != null) {
            org = window.getOrganization();
        }
        AdminUser reviewedBy = sub.getReviewedBy();
        EnrollmentInvitation inv = sub.getInvitation();

        List<EnrollmentOrganizationPolicyDto> organizationPolicies = new ArrayList<>();
        BigDecimal sumPremium = BigDecimal.ZERO;
        if (org != null) {
            List<Policy> policies = policyRepository.findByOrganizationId(org.getOrganizationId());
            for (Policy p : policies) {
                if (p.getPremiumAmount() != null) {
                    sumPremium = sumPremium.add(p.getPremiumAmount());
                }
                EnrollmentOrganizationPolicyDto pd = new EnrollmentOrganizationPolicyDto();
                pd.setPolicyId(p.getPolicyId());
                pd.setPolicyNumber(p.getPolicyNumber());
                pd.setProductType(p.getProductType() != null ? p.getProductType().name() : null);
                pd.setSumInsured(p.getSumInsured());
                pd.setCoverageAmount(p.getSumInsured());
                pd.setCoverageType(p.getCoverageType() != null ? p.getCoverageType().name() : null);
                pd.setSumInsuredMultiplier(p.getSumInsuredMultiplier());
                pd.setInsurerName(p.getInsuranceProviderId() != null
                        ? insuranceProviderRepository.findById(p.getInsuranceProviderId()).map(provider -> provider.getProviderName()).orElse(null)
                        : null);
                LocalDate effStart = p.getEffectiveFrom() != null ? p.getEffectiveFrom() : p.getStartDate();
                pd.setEffectiveFrom(effStart != null ? effStart.toString() : null);
                pd.setPolicyStatus(p.getStatus() != null ? p.getStatus().name() : null);
                organizationPolicies.add(pd);
            }
        }

        String premiumBreakdown = sub.getPremiumBreakdown();
        if (premiumBreakdown == null || premiumBreakdown.isBlank() || "{}".equals(premiumBreakdown.trim())) {
            premiumBreakdown = String.format("{\"total\":%s,\"employerShare\":%s,\"employeeShare\":0}",
                    sumPremium.stripTrailingZeros().toPlainString(),
                    sumPremium.stripTrailingZeros().toPlainString());
        }

        return SubmissionDetailDto.builder()
                .id(sub.getId())
                .referenceNumber(sub.getReferenceNumber())
                .status(sub.getStatus() != null ? sub.getStatus().getValue() : null)
                .stage(sub.getStage())
                .planSelections(sub.getPlanSelections())
                .nomineeData(sub.getNomineeData())
                .personalDetails(sub.getPersonalDetails())
                .dependents(sub.getDependents())
                .premiumBreakdown(premiumBreakdown)
                .submittedAt(sub.getSubmittedAt())
                .reviewedAt(sub.getReviewedAt())
                .reviewedByName(reviewedBy != null ? reviewedBy.getFullName() : null)
                .reviewedById(reviewedBy != null ? reviewedBy.getId() : null)
                .rejectionReason(sub.getRejectionReason())
                .declarationAccepted(sub.getDeclarationAccepted())
                .declarationTimestamp(sub.getDeclarationTimestamp())
                .version(sub.getVersion())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .employeeId(emp != null ? emp.getIndividualId() : null)
                .employeeName(emp != null ? emp.getFullName() : null)
                .employeeEmail(emp != null ? emp.getEmail() : null)
                .employeePhone(emp != null ? emp.getPhone() : null)
                .employeeNumber(emp != null ? emp.getEmployeeNumber() : null)
                .employeeDateOfJoining(emp != null && emp.getDateOfJoining() != null
                        ? emp.getDateOfJoining().toString() : null)
                .employeeDepartment(emp != null ? emp.getDepartment() : null)
                .employeeMaritalStatus(emp != null ? emp.getMaritalStatus() : null)
                .employeeDesignation(emp != null ? emp.getDesignation() : null)
                .enrollmentWindowId(window != null ? window.getId() : null)
                .windowName(window != null ? window.getName() : null)
                .organizationId(org != null ? org.getOrganizationId() : null)
                .organizationName(org != null ? org.getOrganizationName() : null)
                .invitationId(inv != null ? inv.getId() : null)
                .invitationStatus(inv != null && inv.getStatus() != null ? inv.getStatus().getValue() : null)
                .organizationPolicies(organizationPolicies)
                .build();
    }

    /**
     * Sets enrollmentStatus on the submission's employee and all deals linked to this submission (dependents).
     * Does not change account status (e.g. remains PENDING_APPROVAL).
     */
    private void updateDealEnrollmentStatusForSubmission(UUID submissionId, EnrollementStatus enrollmentStatus) {
        EnrollmentSubmission sub = enrollmentSubmissionRepository.findById(submissionId).orElse(null);
        if (sub == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Deals employee = sub.getEmployee();
        if (employee != null) {
            employee.setEnrollmentStatus(enrollmentStatus);
            employee.setUpdatedAt(now);
            dealsRepository.save(employee);
        }
        List<Deals> dependents = dealsRepository.findByEnrollmentSubmission_Id(submissionId);
        for (Deals d : dependents) {
            d.setEnrollmentStatus(enrollmentStatus);
            d.setUpdatedAt(now);
        }
        if (!dependents.isEmpty()) {
            saveDealsInBatches(dependents);
        }
    }

    private void sendApprovalEmail(EnrollmentSubmission sub) {
        try {
            Deals emp = sub.getEmployee();
            if (emp == null || emp.getEmail() == null || emp.getEmail().isBlank()) {
                return;
            }
            String subject = "Enrollment Approved – " + (sub.getReferenceNumber() != null ? sub.getReferenceNumber() : sub.getId());
            boolean useCostSharingNotice = sub.getTotalEmployeeAnnualPremium() != null
                    && sub.getTotalEmployeeAnnualPremium().compareTo(BigDecimal.ZERO) > 0;
            if (useCostSharingNotice) {
                String companyName = emp.getOrganization() != null ? emp.getOrganization().getOrganizationName() : "Company";
                java.util.Map<String, Object> vars = new java.util.HashMap<>();
                vars.put("employeeName", emp.getFullName() != null ? emp.getFullName() : "Employee");
                vars.put("companyName", companyName);
                vars.put("totalPremium", sub.getTotalEmployeeAnnualPremium().add(sub.getTotalEmployerAnnualPremium() != null ? sub.getTotalEmployerAnnualPremium() : BigDecimal.ZERO));
                vars.put("employerShare", sub.getTotalEmployerAnnualPremium() != null ? sub.getTotalEmployerAnnualPremium() : BigDecimal.ZERO);
                vars.put("employeeShare", sub.getTotalEmployeeAnnualPremium());
                vars.put("deductionAmount", sub.getDeductionAmountPerPeriod() != null ? sub.getDeductionAmountPerPeriod() : BigDecimal.ZERO);
                vars.put("deductionFrequency", sub.getDeductionFrequency() != null ? sub.getDeductionFrequency() : "MONTHLY");
                EmailRequest req = EmailRequest.builder()
                        .to(emp.getEmail())
                        .subject(subject)
                        .templateName("cost-sharing-notice")
                        .templateVariables(vars)
                        .build();
                emailService.sendTemplateEmail(req);
            } else {
                String body = "Your enrollment submission has been approved.\n\nReference: "
                        + (sub.getReferenceNumber() != null ? sub.getReferenceNumber() : sub.getId())
                        + "\n\nThank you.";
                EmailRequest req = EmailRequest.builder()
                        .to(emp.getEmail())
                        .subject(subject)
                        .body(body)
                        .isHtml(false)
                        .build();
                emailService.sendSimpleEmail(req);
            }
        } catch (Exception e) {
            log.warn("[correlationId:{}] Failed to send approval email: {}", MDC.get("correlationId"), e.getMessage());
        }
    }


    /**
     * Filters deals eligible for a given policy type:
     * - GPA/GTL/TOP_UP/SUPER_TOP_UP: employees only (SELF/EMPLOYEE)
     * - PARENT_GMC: employees + parent relationships
     * - GMC/GHI and others: employees + all non-parent dependents
     */
    private List<Deals> filterDealsForPolicyType(List<Deals> allDeals, ProductType pt) {
        if (pt == ProductType.GPA || pt == ProductType.GTL || pt == ProductType.TOP_UP || pt == ProductType.SUPER_TOP_UP) {
            return allDeals.stream()
                    .filter(d -> d.getRelationship() == null
                            || "SELF".equalsIgnoreCase(d.getRelationship())
                            || "EMPLOYEE".equalsIgnoreCase(d.getRelationship()))
                    .toList();
        }
        if (pt == ProductType.PARENT_GMC) {
            return allDeals.stream()
                    .filter(d -> d.getRelationship() == null
                            || "SELF".equalsIgnoreCase(d.getRelationship())
                            || "EMPLOYEE".equalsIgnoreCase(d.getRelationship())
                            || isParentDealRelationship(d.getRelationship()))
                    .toList();
        }
        // GMC/GHI/default: employees + all non-parent dependents
        return allDeals.stream()
                .filter(d -> !isParentDealRelationship(d.getRelationship()))
                .toList();
    }

    private Set<Long> extractPolicyIdsFromPlanSelections(String planSelectionsJson, Organization org) {
        Set<Long> policyIds = new HashSet<>();
        if (planSelectionsJson == null || planSelectionsJson.isBlank()) {
            return policyIds;
        }
        Set<String> unresolvedPlanTypes = new HashSet<>();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(planSelectionsJson);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (node.has("policyId")) {
                        JsonNode pidNode = node.get("policyId");
                        Long parsed = parsePolicyIdNode(pidNode);
                        if (parsed != null) {
                            policyIds.add(parsed);
                            continue;
                        }
                    }
                    String planType = text(node.get("planType"));
                    if (planType != null && !planType.isBlank()) {
                        unresolvedPlanTypes.add(planType.trim().toUpperCase());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[correlationId:{}] Failed to parse plan selections for policy extraction: {}", MDC.get("correlationId"), e.getMessage());
        }

        if (org == null || org.getOrganizationId() == null) {
            return policyIds;
        }

        try {
            List<Policy> organizationPolicies = policyRepository.findByOrganizationId(org.getOrganizationId());
            if (!unresolvedPlanTypes.isEmpty()) {
                for (String planType : unresolvedPlanTypes) {
                    Policy matched = organizationPolicies.stream()
                            .filter(p -> p.getPolicyId() != null && matchesPlanType(planType, p.getProductType()))
                            .findFirst()
                            .orElse(null);
                    if (matched != null && matched.getPolicyId() != null) {
                        policyIds.add(matched.getPolicyId());
                    }
                }
            }
            // Last resort: planSelections missing policyId/planType but window has approved submissions — use all org policies
            if (policyIds.isEmpty()) {
                for (Policy p : organizationPolicies) {
                    if (p.getPolicyId() != null) {
                        policyIds.add(p.getPolicyId());
                    }
                }
                if (!policyIds.isEmpty()) {
                    log.info("[correlationId:{}] Resolved policy IDs from organization {} policies (fallback): {}",
                            MDC.get("correlationId"), org.getOrganizationId(), policyIds);
                }
            }
        } catch (Exception e) {
            log.warn("[correlationId:{}] Failed fallback policy resolution from planType for org {}: {}",
                    MDC.get("correlationId"),
                    org.getOrganizationId(),
                    e.getMessage());
        }
        return policyIds;
    }

    private Long parsePolicyIdNode(JsonNode pidNode) {
        if (pidNode == null || pidNode.isNull()) {
            return null;
        }
        try {
            if (pidNode.canConvertToLong()) {
                return pidNode.asLong();
            }
            if (pidNode.isTextual()) {
                String t = pidNode.asText();
                if (t != null && !t.isBlank()) {
                    return Long.parseLong(t.trim());
                }
            }
        } catch (Exception e) {
            log.warn("[correlationId:{}] Could not parse policyId from planSelections: {}", MDC.get("correlationId"), e.getMessage());
        }
        return null;
    }

    private boolean matchesPlanType(String planType, ProductType productType) {
        if (planType == null || productType == null) {
            return false;
        }
        String normalizedPlanType = planType.trim().toUpperCase();
        String normalizedProductType = productType.name().toUpperCase();
        if ("GMC".equals(normalizedPlanType) || "GHI".equals(normalizedPlanType)) {
            return "GMC".equals(normalizedProductType) || "GHI".equals(normalizedProductType);
        }
        if ("PARENT_GMC".equals(normalizedPlanType)) {
            return "PARENT_GMC".equals(normalizedProductType);
        }
        return normalizedPlanType.equals(normalizedProductType);
    }

    private void sendRejectionEmail(EnrollmentSubmission sub, String reason) {
        try {
            Deals emp = sub.getEmployee();
            if (emp == null || emp.getEmail() == null || emp.getEmail().isBlank()) {
                return;
            }
            String subject = "Enrollment Not Approved – " + (sub.getReferenceNumber() != null ? sub.getReferenceNumber() : sub.getId());
            String body = "Your enrollment submission could not be approved.\n\nReference: "
                    + (sub.getReferenceNumber() != null ? sub.getReferenceNumber() : sub.getId())
                    + "\nReason: " + (reason != null ? reason : "Not specified")
                    + "\n\nYou may contact HR if you need to resubmit.";
            EmailRequest req = EmailRequest.builder()
                    .to(emp.getEmail())
                    .subject(subject)
                    .body(body)
                    .isHtml(false)
                    .build();
            emailService.sendSimpleEmail(req);
        } catch (Exception e) {
            log.warn("[correlationId:{}] Failed to send rejection email: {}", MDC.get("correlationId"), e.getMessage());
        }
    }

    /**
     * Updates employee (Deals) from personalDetails JSON. Only non-null fields in JSON are applied.
     */
    private void updateEmployeeFromPersonalDetails(Deals employee, String personalDetailsJson) {
        if (employee == null || personalDetailsJson == null || personalDetailsJson.isBlank()) {
            return;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(personalDetailsJson);
            if (!root.isObject()) {
                return;
            }
            if (root.has("firstName")) {
                employee.setFirstName(text(root.get("firstName")));
            }
            if (root.has("lastName")) {
                employee.setLastName(text(root.get("lastName")));
            }
            if (root.has("fullName")) {
                employee.setFullName(text(root.get("fullName")));
            } else if (root.has("firstName") || root.has("lastName")) {
                String first = employee.getFirstName() != null ? employee.getFirstName() : "";
                String last = employee.getLastName() != null ? employee.getLastName() : "";
                employee.setFullName((first + " " + last).trim());
            }
            if (root.has("email")) {
                employee.setEmail(text(root.get("email")));
            }
            if (root.has("phone") || root.has("mobile")) {
                employee.setPhone(text(root.has("phone") ? root.get("phone") : root.get("mobile")));
            }
            if (root.has("dateOfBirth") || root.has("dob")) {
                employee.setDateOfBirth(parseDate(text(root.has("dateOfBirth") ? root.get("dateOfBirth") : root.get("dob"))));
            }
            if (root.has("gender")) {
                employee.setGender(text(root.get("gender")));
            }
            if (root.has("address")) {
                employee.setAddress(text(root.get("address")));
            }
            if (root.has("city")) {
                employee.setCity(text(root.get("city")));
            }
            if (root.has("state")) {
                employee.setState(text(root.get("state")));
            }
            if (root.has("pincode")) {
                employee.setPincode(text(root.get("pincode")));
            }
            if (root.has("maritalStatus")) {
                employee.setMaritalStatus(text(root.get("maritalStatus")));
            }
            if (root.has("employeeNumber")) {
                employee.setEmployeeNumber(text(root.get("employeeNumber")));
            }
            employee.setUpdatedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("[correlationId:{}] Failed to parse personalDetails JSON: {}", MDC.get("correlationId"), e.getMessage());
        }
    }

    /**
     * When dependent DOJ is omitted, use active GMC or GHI policy effective_from (or start_date if effective_from is null).
     * If several such policies exist, the earliest candidate date is used.
     */
    private LocalDate resolveDefaultDependentDateOfJoining(Organization org) {
        if (org == null || org.getOrganizationId() == null) {
            return null;
        }
        LocalDate best = null;
        for (ProductType pt : new ProductType[] { ProductType.GMC, ProductType.GHI }) {
            List<Policy> policies = policyRepository.findByOrganizationIdAndProductTypeAndStatus(
                    org.getOrganizationId(), pt, PolicyStatus.ACTIVE);
            if (policies == null || policies.isEmpty()) {
                continue;
            }
            for (Policy p : policies) {
                LocalDate candidate = p.getEffectiveFrom() != null ? p.getEffectiveFrom() : p.getStartDate();
                if (candidate == null) {
                    continue;
                }
                if (best == null || candidate.isBefore(best)) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    /**
     * Creates Deals (dependents) from dependents JSON array. Each object may have name/firstName/lastName, relationship, dateOfBirth, gender, etc.
     */
    private List<Deals> createDependentsFromJson(EnrollmentSubmission sub, Deals primaryEmployee, String dependentsJson) {
        List<Deals> list = new ArrayList<>();
        if (dependentsJson == null || dependentsJson.isBlank()) {
            return list;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(dependentsJson);
            if (!root.isArray()) {
                return list;
            }
            Organization org = primaryEmployee.getOrganization();
            LocalDate defaultDependentDoj = resolveDefaultDependentDateOfJoining(org);
            for (JsonNode node : root) {
                if (!node.isObject()) {
                    continue;
                }
                Deals d = new Deals();
                d.setOrganization(org);
                d.setPrimaryIndividual(primaryEmployee);
                d.setEnrollmentSubmission(sub);
                d.setIsPrimaryMember(false);
                d.setAccountType(AccountType.CORPORATE_DEPENDENT);
                d.setStatus(AccountStatus.PENDING_APPROVAL);
                d.setEmployeeNumber(primaryEmployee.getEmployeeNumber());

                String firstName = text(node.get("firstName"));
                String lastName = text(node.get("lastName"));
                String fullName = text(node.get("fullName"));
                if (fullName == null && (firstName != null || lastName != null)) {
                    fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                    fullName = fullName.trim();
                }
                if (fullName == null) {
                    fullName = text(node.get("name"));
                }
                if (firstName == null && fullName != null) {
                    String[] parts = fullName.split("\\s+", 2);
                    firstName = parts[0];
                    lastName = parts.length > 1 ? parts[1] : null;
                }
                d.setFirstName(firstName);
                d.setLastName(lastName);
                d.setFullName(fullName);
                d.setRelationship(text(node.get("relationship")));
                d.setDateOfBirth(parseDate(text(node.get("dateOfBirth"))));
                if (d.getDateOfBirth() == null) {
                    d.setDateOfBirth(parseDate(text(node.get("dob"))));
                }
                d.setGender(text(node.get("gender")));
                d.setEmail(text(node.get("email")));
                d.setPhone(text(node.get("phone")));
                if (d.getPhone() == null) {
                    d.setPhone(text(node.get("mobile")));
                }
                LocalDate dependentDoj = null;
                if (node.has("dateOfJoining")) {
                    dependentDoj = parseDate(text(node.get("dateOfJoining")));
                }
                if (dependentDoj == null && node.has("date_of_joining")) {
                    dependentDoj = parseDate(text(node.get("date_of_joining")));
                }
                if (dependentDoj == null && node.has("doj")) {
                    dependentDoj = parseDate(text(node.get("doj")));
                }
                if (dependentDoj != null) {
                    d.setDateOfJoining(dependentDoj);
                } else if (defaultDependentDoj != null) {
                    d.setDateOfJoining(defaultDependentDoj);
                }
                d.setCreatedAt(LocalDateTime.now());
                d.setUpdatedAt(LocalDateTime.now());
                list.add(d);
            }
        } catch (Exception e) {
            log.warn("[correlationId:{}] Failed to parse dependents JSON: {}", MDC.get("correlationId"), e.getMessage());
        }
        return list;
    }

    /**
     * Creates Nominee entities from nomineeData JSON.
     * Supports:
     * - New structure: { "gpaNominees": [...], "gtlNominees": [...], "customNominees": [...], "useSameAsGtl": boolean }
     *   where each nominee has id, name (or firstName/lastName), percentage, relationship, etc.
     * - Legacy: single object or array of objects with firstName, lastName, fullName, dateOfBirth, gender, relationship.
     */
    private List<Nominee> createNomineesFromJson(EnrollmentSubmission sub, Deals customer, String nomineeDataJson) {
        List<Nominee> list = new ArrayList<>();
        if (nomineeDataJson == null || nomineeDataJson.isBlank()) {
            return list;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(nomineeDataJson);

            // New structure: gpaNominees, gtlNominees, customNominees (dedupe by id)
            if (root.isObject() && (root.has("gpaNominees") || root.has("gtlNominees") || root.has("customNominees"))) {
                Map<String, JsonNode> byId = new LinkedHashMap<>();
                if (root.has("customNominees") && root.get("customNominees").isArray()) {
                    for (JsonNode node : root.get("customNominees")) {
                        if (node.isObject()) {
                            String id = text(node.get("id"));
                            if (id != null && !id.isBlank()) {
                                byId.put(id, node);
                            }
                        }
                    }
                }
                if (root.has("gpaNominees") && root.get("gpaNominees").isArray()) {
                    for (JsonNode node : root.get("gpaNominees")) {
                        if (node.isObject()) {
                            String id = text(node.get("id"));
                            if (id != null && !id.isBlank()) {
                                byId.putIfAbsent(id, node);
                            }
                        }
                    }
                }
                boolean useSameAsGtl = root.has("useSameAsGtl") && root.get("useSameAsGtl").asBoolean(false);
                if (root.has("gtlNominees") && root.get("gtlNominees").isArray()) {
                    for (JsonNode node : root.get("gtlNominees")) {
                        if (node.isObject()) {
                            String id = text(node.get("id"));
                            if (id != null && !id.isBlank()) {
                                if (useSameAsGtl && byId.containsKey(id)) {
                                    continue;
                                }
                                byId.putIfAbsent(id, node);
                            }
                        }
                    }
                }
                for (JsonNode node : byId.values()) {
                    Nominee nominee = mapNomineeNodeToEntity(sub, customer, node, true);
                    if (nominee != null) {
                        list.add(nominee);
                    }
                }
                return list;
            }

            // Legacy: array or single object
            List<JsonNode> nodes = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode n : root) {
                    nodes.add(n);
                }
            } else if (root.isObject()) {
                nodes.add(root);
            }
            for (JsonNode node : nodes) {
                if (!node.isObject()) {
                    continue;
                }
                Nominee nominee = mapNomineeNodeToEntity(sub, customer, node, false);
                if (nominee != null) {
                    list.add(nominee);
                }
            }
        } catch (Exception e) {
            log.warn("[correlationId:{}] Failed to parse nomineeData JSON: {}", MDC.get("correlationId"), e.getMessage());
        }
        return list;
    }

    /**
     * Maps a single JSON node (from gpaNominees/gtlNominees/customNominees or legacy) to a Nominee entity.
     * @param allowMissingDobGender when true (new structure), use defaults for missing dob/gender; when false (legacy), skip if missing.
     */
    private Nominee mapNomineeNodeToEntity(EnrollmentSubmission sub, Deals customer, JsonNode node, boolean allowMissingDobGender) {
        String firstName = text(node.get("firstName"));
        String lastName = text(node.get("lastName"));
        String fullName = text(node.get("fullName"));
        String policyId = text(node.get("policyId"));
        if (fullName == null && (firstName != null || lastName != null)) {
            fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
            fullName = fullName.trim();
        }
        if (fullName == null) {
            fullName = text(node.get("name"));
        }
        if (firstName == null && fullName != null) {
            String[] parts = fullName.split("\\s+", 2);
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = parts[1];
            }
        }
        LocalDate dob = parseDate(text(node.get("dateOfBirth")));
        if (dob == null) {
            dob = parseDate(text(node.get("dob")));
        }
        String gender = text(node.get("gender"));
        String relationship = text(node.get("relationship"));
        if ((firstName == null || firstName.isBlank()) && (fullName == null || fullName.isBlank())) {
            return null;
        }
        if (relationship == null || relationship.isBlank()) {
            return null;
        }
        if (!allowMissingDobGender && (dob == null || gender == null)) {
            return null;
        }
        if (dob == null) {
            dob = LocalDate.of(1900, 1, 1);
        }
        if (gender == null || gender.isBlank()) {
            gender = "Other";
        }
        Nominee n = new Nominee();
        n.setCustomer(customer);
        n.setFirstName(firstName != null ? firstName : fullName);
        n.setLastName(lastName);
        n.setFullName(fullName);
        n.setDateOfBirth(dob);
        n.setGender(gender);
        n.setRelationship(relationship);
        if (policyId != null && !policyId.isBlank()) {
            n.setPolicy(policyRepository.findById(Long.parseLong(policyId)).orElse(null));
        }
        else{
            n.setSubmission(sub);
        }
        if (node.has("percentage")) {
            try {
                n.setNomineePercentage(BigDecimal.valueOf(node.get("percentage").doubleValue()));
            } catch (Exception ignored) {
                n.setNomineePercentage(BigDecimal.valueOf(100.00));
            }
        } else if (node.has("nomineePercentage")) {
            try {
                n.setNomineePercentage(BigDecimal.valueOf(node.get("nomineePercentage").doubleValue()));
            } catch (Exception ignored) {
                n.setNomineePercentage(BigDecimal.valueOf(100.00));
            }
        } else {
            n.setNomineePercentage(BigDecimal.valueOf(100.00));
        }
        n.setIsActive(true);
        return n;
    }

    /** Same rules as EmployeeService: parent/in-law rows for PARENT_GMC vs floater dependents. */
    private boolean isParentDealRelationship(String relationship) {
        if (relationship == null) {
            return false;
        }
        String r = relationship.trim();
        if ("FATHER".equalsIgnoreCase(r) || "MOTHER".equalsIgnoreCase(r)) {
            return true;
        }
        String compact = r.replaceAll("[\\s_-]+", "").toUpperCase();
        return "FATHERINLAW".equals(compact) || "MOTHERINLAW".equals(compact);
    }

    private void saveDealsInBatches(List<Deals> deals) {
        if (deals == null || deals.isEmpty()) {
            return;
        }
        for (int i = 0; i < deals.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, deals.size());
            List<Deals> batch = deals.subList(i, end);
            dealsRepository.saveAll(batch);
            log.debug("[correlationId:{}] Saved deals batch {}-{} of {}", MDC.get("correlationId"), i + 1, end, deals.size());
        }
    }

    private void saveNomineesInBatches(List<Nominee> nominees) {
        if (nominees == null || nominees.isEmpty()) {
            return;
        }
        for (int i = 0; i < nominees.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, nominees.size());
            List<Nominee> batch = nominees.subList(i, end);
            nomineeRepository.saveAll(batch);
            log.debug("[correlationId:{}] Saved nominees batch {}-{} of {}", MDC.get("correlationId"), i + 1, end, nominees.size());
        }
    }

    private static String text(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        String s = n.asText();
        return (s != null && !s.isBlank()) ? s.trim() : null;
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}
