package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.ApprovalRequest;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.BulkApprovalRequest;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.RejectionRequest;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.SubmissionDetailDto;
import com.vimainsurance.vimaadmin.dto.SubmissionListItemDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.AccountStatus;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.service.IHRApprovalService;
import com.vimainsurance.vimaadmin.specification.EnrollmentSubmissionSpecification;
import com.vimainsurance.vimaadmin.util.TenantContext;
import com.vimainsurance.vimaadmin.util.JwtUserExtractor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HRApprovalServiceImpl implements IHRApprovalService {

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
            List<UUID> organizationIds = resolveOrganizationIds(null);
            if (organizationIds == null || organizationIds.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No organization access"));
            }

            Optional<EnrollmentSubmission> opt = enrollmentSubmissionRepository.findById(id);
            if (opt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Enrollment submission not found"));
            }
            EnrollmentSubmission sub = opt.get();
            UUID orgId = sub.getEmployee() != null && sub.getEmployee().getOrganization() != null
                    ? sub.getEmployee().getOrganization().getOrganizationId()
                    : null;
            if (orgId == null || !organizationIds.contains(orgId)) {
                return responseObj.render(responseObj.formErrorResponse(403, "Access denied to this submission"));
            }

            SubmissionDetailDto dto = toDetailDto(sub);
            return responseObj.render(responseObj.formSuccessResponse("Enrollment detail", dto));
        } catch (Exception e) {
            log.error("[correlationId:{}] getEnrollmentDetail error: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Failed to get enrollment detail"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<SubmissionDetailDto>> approve(UUID id, ApprovalRequest request) {
        BaseResponse<SubmissionDetailDto> responseObj = new BaseResponse<>();
        try {
            List<UUID> organizationIds = resolveOrganizationIds(null);
            if (organizationIds == null || organizationIds.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No organization access"));
            }

            EnrollmentSubmission sub = enrollmentSubmissionRepository.findById(id).orElse(null);
            if (sub == null) {
                return responseObj.render(responseObj.formErrorResponse(404, "Enrollment submission not found"));
            }
            UUID orgId = sub.getEmployee() != null && sub.getEmployee().getOrganization() != null
                    ? sub.getEmployee().getOrganization().getOrganizationId()
                    : null;
            if (orgId == null || !organizationIds.contains(orgId)) {
                return responseObj.render(responseObj.formErrorResponse(403, "Access denied to this submission"));
            }
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

            List<Deals> dependents = dealsRepository.findByEnrollmentSubmission_Id(id);
            LocalDateTime now = LocalDateTime.now();
            for (Deals d : dependents) {
                d.setStatus(AccountStatus.PENDING_APPROVAL);
                d.setUpdatedAt(now);
            }
            if (!dependents.isEmpty()) {
                dealsRepository.saveAll(dependents);
            }

            sendApprovalEmail(sub);

            SubmissionDetailDto dto = toDetailDto(enrollmentSubmissionRepository.findById(id).orElse(sub));
            return responseObj.render(responseObj.formSuccessResponse("Enrollment approved", dto));
        } catch (Exception e) {
            log.error("[correlationId:{}] approve error: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Approval failed"));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<SubmissionDetailDto>> reject(UUID id, RejectionRequest request) {
        BaseResponse<SubmissionDetailDto> responseObj = new BaseResponse<>();
        try {
            List<UUID> organizationIds = resolveOrganizationIds(null);
            if (organizationIds == null || organizationIds.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No organization access"));
            }

            EnrollmentSubmission sub = enrollmentSubmissionRepository.findById(id).orElse(null);
            if (sub == null) {
                return responseObj.render(responseObj.formErrorResponse(404, "Enrollment submission not found"));
            }
            UUID orgId = sub.getEmployee() != null && sub.getEmployee().getOrganization() != null
                    ? sub.getEmployee().getOrganization().getOrganizationId()
                    : null;
            if (orgId == null || !organizationIds.contains(orgId)) {
                return responseObj.render(responseObj.formErrorResponse(403, "Access denied to this submission"));
            }
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

            List<Deals> dependents = dealsRepository.findByEnrollmentSubmission_Id(id);
            LocalDateTime now = LocalDateTime.now();
            for (Deals d : dependents) {
                d.setStatus(AccountStatus.REJECTED);
                d.setUpdatedAt(now);
            }
            if (!dependents.isEmpty()) {
                dealsRepository.saveAll(dependents);
            }

            if (request.isReopenInvitation() && sub.getInvitation() != null) {
                EnrollmentInvitation inv = sub.getInvitation();
                inv.setStatus(EnrollementStatus.SENT);
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
    @Transactional
    public ResponseEntity<ResponseDto<List<SubmissionListItemDto>>> bulkApprove(BulkApprovalRequest request) {
        BaseResponse<List<SubmissionListItemDto>> responseObj = new BaseResponse<>();
        try {
            List<UUID> organizationIds = resolveOrganizationIds(null);
            if (organizationIds == null || organizationIds.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("No organization access"));
            }

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
                if (orgId == null || !organizationIds.contains(orgId)) {
                    throw new IllegalArgumentException("Access denied to submission: " + id);
                }
                if (sub.getStatus() != EnrollementStatus.SUBMITTED) {
                    throw new IllegalArgumentException("Submission " + id + " is not in SUBMITTED status");
                }

                sub.setStatus(EnrollementStatus.APPROVED);
                sub.setReviewedBy(reviewer);
                sub.setReviewedAt(LocalDateTime.now());
                enrollmentSubmissionRepository.save(sub);

                List<Deals> dependents = dealsRepository.findByEnrollmentSubmission_Id(id);
                LocalDateTime now = LocalDateTime.now();
                for (Deals d : dependents) {
                    d.setStatus(AccountStatus.ACTIVE);
                    d.setUpdatedAt(now);
                }
                if (!dependents.isEmpty()) {
                    dealsRepository.saveAll(dependents);
                }

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

    /**
     * Resolve organization IDs for access control. HR_ADMIN sees only their company (from tenant).
     * If companyId is provided, it must be in the tenant's allowed list; otherwise all allowed orgs are used.
     */
    private List<UUID> resolveOrganizationIds(String companyId) {
        Map<String, List<String>> tenantMap = TenantContext.getCurrentTenant();
        List<String> allowed = tenantMap != null ? tenantMap.get("organizationIds") : null;
        if (allowed == null) {
            allowed = new ArrayList<>();
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
        AdminUser reviewedBy = sub.getReviewedBy();
        EnrollmentInvitation inv = sub.getInvitation();

        return SubmissionDetailDto.builder()
                .id(sub.getId())
                .referenceNumber(sub.getReferenceNumber())
                .status(sub.getStatus() != null ? sub.getStatus().getValue() : null)
                .stage(sub.getStage())
                .planSelections(sub.getPlanSelections())
                .nomineeData(sub.getNomineeData())
                .personalDetails(sub.getPersonalDetails())
                .dependents(sub.getDependents())
                .premiumBreakdown(sub.getPremiumBreakdown())
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
                .enrollmentWindowId(window != null ? window.getId() : null)
                .windowName(window != null ? window.getName() : null)
                .organizationId(org != null ? org.getOrganizationId() : null)
                .organizationName(org != null ? org.getOrganizationName() : null)
                .invitationId(inv != null ? inv.getId() : null)
                .invitationStatus(inv != null && inv.getStatus() != null ? inv.getStatus().getValue() : null)
                .build();
    }

    private void sendApprovalEmail(EnrollmentSubmission sub) {
        try {
            Deals emp = sub.getEmployee();
            if (emp == null || emp.getEmail() == null || emp.getEmail().isBlank()) {
                return;
            }
            String subject = "Enrollment Approved – " + (sub.getReferenceNumber() != null ? sub.getReferenceNumber() : sub.getId());
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
        } catch (Exception e) {
            log.warn("[correlationId:{}] Failed to send approval email: {}", MDC.get("correlationId"), e.getMessage());
        }
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
}
