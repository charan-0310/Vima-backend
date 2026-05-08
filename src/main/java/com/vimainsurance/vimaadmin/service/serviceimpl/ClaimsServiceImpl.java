package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.adapter.InsurerAdapter;
import com.vimainsurance.vimaadmin.adapter.InsurerAdapterFactory;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDetailsResponse;
import com.vimainsurance.vimaadmin.dto.claim.ClaimListFilters;
import com.vimainsurance.vimaadmin.dto.claim.ClaimSubmissionRequest;
import com.vimainsurance.vimaadmin.dto.claim.InsurerRefRequest;
import com.vimainsurance.vimaadmin.dto.claim.InsurerSubmissionResult;
import com.vimainsurance.vimaadmin.dto.claim.StatusUpdateRequest;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.MemberType;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.mapper.ClaimMapper;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.notification.ClaimsNotificationEmitterService;
import com.vimainsurance.vimaadmin.service.IClaimsService;
import com.vimainsurance.vimaadmin.service.claim.ClaimAuditService;
import com.vimainsurance.vimaadmin.service.claim.ClaimNumberGenerator;
import com.vimainsurance.vimaadmin.service.claim.ClaimStatusTransitionValidator;
import com.vimainsurance.vimaadmin.service.claim.ClaimValidationService;
import com.vimainsurance.vimaadmin.service.claim.notification.ClaimsNotificationService;
import com.vimainsurance.vimaadmin.specification.ClaimSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimsServiceImpl implements IClaimsService {

    private final IClaimRepository claimRepository;
    private final IOrganizationRepository organizationRepository;
    private final IDealsRepository dealsRepository;
    private final IAdminUserRepository adminUserRepository;
    private final IDocumentRepository documentRepository;

    private final ClaimNumberGenerator claimNumberGenerator;
    private final ClaimStatusTransitionValidator statusValidator;
    private final ClaimAuditService auditService;
    private final ClaimValidationService validationService;
    private final InsurerAdapterFactory adapterFactory;
    private final ClaimsNotificationService notificationService;
    private final ClaimsNotificationEmitterService claimsNotificationEmitterService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "SUBMIT")
    public ClaimDetailsResponse submitClaim(ClaimSubmissionRequest request, UUID employeeId) {
        log.info("[claim] submitClaim employeeId={}", employeeId);
        validationService.validateSubmission(request, employeeId);
        Deals employee = dealsRepository.findById(employeeId).orElseThrow(() -> new BadRequestException("Employee not found"));
        Organization org = organizationRepository.findById(request.getOrganizationId()).orElseThrow(() -> new BadRequestException("Organization not found"));

        ensureMemberIdForEmployeeSelf(request, employeeId);
        Claim claim = new Claim();
        claim.setClaimNumber(claimNumberGenerator.generateNext());
        claim.setOrganization(org);
        claim.setPolicyId(request.getPolicyId());
        claim.setEmployee(employee);
        mapRequestToClaim(request, claim);
        claim.setInternalStatus(ClaimStatus.PENDING_REVIEW);
        claim.setDateOfSubmission(LocalDate.now());
        claim.setSubmittedBy(adminUserRepository.findById(employeeId).orElse(null));
        claim.setIsDeleted(false);

        if (request.getParentClaimId() != null) {
            claim.setParentClaim(claimRepository.findById(request.getParentClaimId()).orElse(null));
            claim.setParentInsurerClaimRef(request.getParentInsurerClaimRef());
        }

        claim = claimRepository.save(claim);
        auditService.logAction(claim.getId(), "CLAIM_SUBMITTED", ClaimStatus.DRAFT.getValue(), ClaimStatus.PENDING_REVIEW.getValue(),
                employeeId, "EMPLOYEE", "Claim submitted for review", null, null, null);
        notificationService.notifyStatusChange(claim, ClaimStatus.DRAFT, ClaimStatus.PENDING_REVIEW);
        String actorName = employee != null ? employee.getFullName() : "Employee";
        String actorOrg = employee != null && employee.getOrganization() != null
                ? employee.getOrganization().getOrganizationName()
                : null;
        claimsNotificationEmitterService.scheduleClaimSubmitted(claim, actorName, actorOrg, "EMPLOYEE");
        return toDetailsWithDocuments(claim);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "UPDATE")
    public ClaimDetailsResponse updateDraftClaim(UUID claimId, ClaimSubmissionRequest request, UUID employeeId) {
        log.info("[claim] updateDraftClaim claimId={}, claimNumber={}", claimId, claimRepository.findById(claimId).map(Claim::getClaimNumber).orElse("?"));
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new BadRequestException("Claim not found"));
        validationService.validateEmployeeCanEdit(claim, employeeId);
        validationService.validateClaimAmount(request.getClaimAmount());
        validationService.validateDischargeAfterAdmission(request.getDateOfAdmission(), request.getDateOfDischarge());
        validationService.validateReimbursementBankDetails(request.getClaimType(), request.getBankAccountNumber(),
                request.getAccountHolderName(), request.getIfscCode());

        ensureMemberIdForEmployeeSelf(request, employeeId);
        mapRequestToClaim(request, claim);
        claim = claimRepository.save(claim);
        auditService.logAction(claim.getId(), "CLAIM_UPDATED", ClaimStatus.DRAFT.getValue(), ClaimStatus.DRAFT.getValue(),
                employeeId, "EMPLOYEE", "Draft claim updated", null, null, null);
        return toDetailsWithDocuments(claim);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "UPDATE")
    public void cancelClaim(UUID claimId, UUID employeeId) {
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new BadRequestException("Claim not found"));
        validationService.validateEmployeeCanCancel(claim, employeeId);
        ClaimStatus oldStatus = claim.getInternalStatus();
        claim.setInternalStatus(ClaimStatus.CLOSED);
        claim.setIsDeleted(true);
        claimRepository.save(claim);
        auditService.logAction(claimId, "CLAIM_CANCELLED", oldStatus.getValue(), ClaimStatus.CLOSED.getValue(),
                employeeId, "EMPLOYEE", "Claim cancelled by employee", null, null, null);
        log.info("[claimNumber={}] Claim cancelled", claim.getClaimNumber());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "SUBMIT")
    public ClaimDetailsResponse submitToInsurer(UUID claimId) {
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new BadRequestException("Claim not found"));
        if (claim.getInternalStatus() != ClaimStatus.APPROVED_FOR_SUBMISSION) {
            throw new BadRequestException("Claim must be in APPROVED_FOR_SUBMISSION to submit to insurer");
        }

        InsurerAdapter adapter = adapterFactory.getAdapter(claim);
        if (adapter == null) {
            throw new BadRequestException("No insurer adapter available for this claim");
        }
        InsurerSubmissionResult result = adapter.submitToInsurer(claim);
        if (result == null) {
            throw new BadRequestException("Insurer adapter returned no result");
        }

        ClaimStatus oldStatus = claim.getInternalStatus();
        ClaimStatus newStatus = result.isRequiresManualSubmission() ? ClaimStatus.APPROVED_FOR_SUBMISSION : ClaimStatus.SUBMITTED_TO_INSURER;
        if (result.isSuccess() && result.getInsurerClaimRef() != null) {
            claim.setInsurerClaimRef(result.getInsurerClaimRef());
        }
        if (newStatus != oldStatus) {
            claim.setInternalStatus(newStatus);
        }
        claim = claimRepository.save(claim);
        auditService.logAction(claimId, "SUBMIT_TO_INSURER", oldStatus.getValue(), newStatus.getValue(),
                null, "SYSTEM", result.getMessage() != null ? result.getMessage() : "Submitted to insurer", null, null, null);
        log.info("[claimNumber={}] submitToInsurer -> {}", claim.getClaimNumber(), newStatus);
        if (result.isRequiresManualSubmission()) {
            notificationService.notifyAdminManualSubmission(claim);
        } else if (newStatus == ClaimStatus.SUBMITTED_TO_INSURER) {
            notificationService.notifyStatusChange(claim, oldStatus, newStatus);
        }
        return toDetailsWithDocuments(claim);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "UPDATE")
    public ClaimDetailsResponse updateStatus(UUID claimId, StatusUpdateRequest request, UUID actorId, String actorRole) {
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new BadRequestException("Claim not found"));
        ClaimStatus oldStatus = claim.getInternalStatus();
        statusValidator.validateTransition(oldStatus, request.getNewStatus());
        validationService.validateStatusUpdateRequest(request);
        if (request.getNewStatus() == ClaimStatus.REJECTED || request.getNewStatus() == ClaimStatus.REJECTED_BY_ADMIN) {
            String reason = request.getRemark() != null ? request.getRemark() : request.getNotes();
            if (reason != null && !reason.isBlank()) claim.setRejectionReason(reason);
        }
        if (request.getInsurerClaimRef() != null && !request.getInsurerClaimRef().isBlank()) {
            claim.setInsurerClaimRef(request.getInsurerClaimRef());
        }
        if (request.getInsurerClaimNumber() != null && !request.getInsurerClaimNumber().isBlank()) {
            claim.setInsurerClaimNumber(request.getInsurerClaimNumber());
        }
        claim.setInternalStatus(request.getNewStatus());
        claim = claimRepository.save(claim);

        String auditDetails = request.getNotes() != null && !request.getNotes().isBlank() ? request.getNotes() : request.getRemark();
        auditService.logAction(claimId, "STATUS_UPDATE", oldStatus.getValue(), request.getNewStatus().getValue(),
                actorId, actorRole, auditDetails, null, null, null);
        log.info("[claimNumber={}] status {} -> {}", claim.getClaimNumber(), oldStatus, request.getNewStatus());
        notificationService.notifyStatusChange(claim, oldStatus, request.getNewStatus());
        String actorName = resolveActorName(actorId);
        String actorOrganization = resolveActorOrganizationName(actorId);
        switch (request.getNewStatus()) {
            case APPROVED -> claimsNotificationEmitterService.scheduleClaimApproved(claim, actorName, actorOrganization, actorRole);
            case REJECTED, REJECTED_BY_ADMIN -> claimsNotificationEmitterService.scheduleClaimRejected(claim, actorName, actorOrganization, actorRole);
            default -> {
            }
        }
        // Reload with associations so mapper does not trigger lazy-load (avoids 500 in some environments)
        Claim claimWithAssociations = claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claimId)
                .orElse(claim);
        return toDetailsWithDocuments(claimWithAssociations);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditedOperation(schemaName = "claims", tableName = "claims", entityType = "CLAIM", action = "UPDATE")
    public ClaimDetailsResponse updateInsurerRef(UUID claimId, InsurerRefRequest request) {
        Claim claim = claimRepository.findById(claimId).orElseThrow(() -> new BadRequestException("Claim not found"));
        if (request.getInsurerId() != null) claim.setInsurerId(request.getInsurerId());
        if (request.getInsurerClaimRef() != null) claim.setInsurerClaimRef(request.getInsurerClaimRef());
        if (request.getInsurerClaimNumber() != null) claim.setInsurerClaimNumber(request.getInsurerClaimNumber());
        if (request.getInsurerInwardNumber() != null) claim.setInsurerInwardNumber(request.getInsurerInwardNumber());
        if (request.getInsurerStatus() != null) claim.setInsurerStatus(request.getInsurerStatus());
        if (request.getInsurerCurrentStatus() != null) claim.setInsurerCurrentStatus(request.getInsurerCurrentStatus());
        if (request.getInsurerRemarks() != null) claim.setInsurerRemarks(request.getInsurerRemarks());
        claim = claimRepository.save(claim);
        auditService.logAction(claimId, "INSURER_REF_UPDATED", null, null, null, "ADMIN", "Insurer reference updated", null, null, null);
        return toDetailsWithDocuments(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimDetailsResponse getClaimDetails(UUID claimId) {
        Claim claim = claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        List<com.vimainsurance.vimaadmin.entity.Document> documents = documentRepository.findByEntityTypeAndEntityId(
                DocumentEntityType.CLAIM, claimId.toString());
        return ClaimMapper.toDetailsResponse(claim, documents);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimDetailsResponse> listClaims(ClaimListFilters filters, Pageable pageable) {
        Specification<Claim> spec = ClaimSpecification.withFilters(filters);
        Page<Claim> page = claimRepository.findAllWithOrganizationAndEmployee(spec, pageable);
        return page.map(ClaimMapper::toListResponseLight);
    }

    /** When memberType is EMPLOYEE (self) and memberId is null, set memberId to the submitting employee's ID. */
    private void ensureMemberIdForEmployeeSelf(ClaimSubmissionRequest request, UUID employeeId) {
        if ((request.getMemberType() == null || request.getMemberType() == MemberType.EMPLOYEE)
                && (request.getMemberId() == null || request.getMemberId().isBlank())) {
            request.setMemberId(employeeId.toString());
        }
    }

    private void mapRequestToClaim(ClaimSubmissionRequest request, Claim claim) {
        claim.setMemberId(request.getMemberId());
        claim.setMemberType(request.getMemberType());
        claim.setMemberName(request.getMemberName());
        claim.setMemberDob(request.getMemberDob());
        claim.setMemberUhid(request.getMemberUhid());
        claim.setRelationship(request.getRelationship());
        claim.setClaimType(request.getClaimType());
        claim.setClaimCategory(request.getClaimCategory());
        claim.setProductType(request.getProductType());
        claim.setReasonForAdmission(request.getReasonForAdmission());
        claim.setDiagnosis(request.getDiagnosis());
        claim.setClaimAmount(request.getClaimAmount());
        claim.setInsuranceProviderLogo(request.getInsuranceProviderLogo());
        claim.setPolicyNumber(request.getPolicyNumber());
        claim.setValidUntil(request.getValidUntil());
        claim.setHospitalName(request.getHospitalName());
        claim.setHospitalCity(request.getHospitalCity());
        claim.setHospitalState(request.getHospitalState());
        claim.setHospitalPincode(request.getHospitalPincode());
        claim.setHospitalProviderCode(request.getHospitalProviderCode());
        claim.setIsNetworkHospital(request.getIsNetworkHospital());
        claim.setDateOfAdmission(request.getDateOfAdmission());
        claim.setDateOfDischarge(request.getDateOfDischarge());
        claim.setBankAccountNumber(request.getBankAccountNumber());
        claim.setAccountHolderName(request.getAccountHolderName());
        claim.setIfscCode(request.getIfscCode());
        claim.setBankBranchName(request.getBankBranchName());
        claim.setAbhaId(request.getAbhaId());
        claim.setSubmissionSource(request.getSubmissionSource());
    }

    private ClaimDetailsResponse toDetailsWithDocuments(Claim claim) {
        List<Document> documents = documentRepository.findByEntityTypeAndEntityId(
                DocumentEntityType.CLAIM, claim.getId().toString());
        return ClaimMapper.toDetailsResponse(claim, documents);
    }

    private String resolveActorName(UUID actorId) {
        if (actorId == null) {
            return "System";
        }
        return adminUserRepository.findById(actorId).map(u -> {
            if (u.getFullName() != null && !u.getFullName().isBlank()) {
                return u.getFullName();
            }
            if (u.getUsername() != null && !u.getUsername().isBlank()) {
                return u.getUsername();
            }
            return "System";
        }).orElse("System");
    }

    private String resolveActorOrganizationName(UUID actorId) {
        if (actorId == null) {
            return null;
        }
        return adminUserRepository.findById(actorId)
                .map(u -> u.getOrganization() != null ? u.getOrganization().getOrganizationName() : null)
                .orElse(null);
    }
}
