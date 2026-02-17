package com.vimainsurance.vimaadmin.service.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.claim.ClaimSubmissionRequest;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.Policy;
import com.vimainsurance.vimaadmin.enums.ClaimCategory;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;
import com.vimainsurance.vimaadmin.enums.MemberType;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business rules for claims (BR-CS-001 through BR-CS-009).
 */
@Service
@RequiredArgsConstructor
public class ClaimValidationService {

    private final IClaimRepository claimRepository;
    private final IDealsRepository dealsRepository;
    private final IPolicyRepository policyRepository;

    /** BR-CS-001: Employee can only submit for self or registered dependents */
    public void validateEmployeeCanSubmitForMember(UUID employeeId, ClaimSubmissionRequest request) {
        Deals employee = dealsRepository.findById(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));
        if (request.getMemberType() == null || request.getMemberType() == MemberType.EMPLOYEE) {
            if (!Objects.equals(employeeId, employee.getIndividualId())) {
                throw new BadRequestException("BR-CS-001: Employee can only submit for self or registered dependents");
            }
            return;
        }
        if (request.getMemberType() == MemberType.DEPENDENT) {
            if (request.getMemberId() == null) {
                throw new BadRequestException("BR-CS-001: Dependent memberId required");
            }
            try {
                UUID memberUuid = UUID.fromString(request.getMemberId());
                Deals dependent = dealsRepository.findById(memberUuid).orElse(null);
                if (dependent == null || dependent.getPrimaryIndividual() == null
                        || !Objects.equals(dependent.getPrimaryIndividual().getIndividualId(), employeeId)) {
                    throw new BadRequestException("BR-CS-001: Employee can only submit for self or registered dependents");
                }
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("BR-CS-001: Invalid memberId");
            }
        }
    }

    /** BR-CS-002: Claim amount > 0 */
    public void validateClaimAmount(BigDecimal claimAmount) {
        if (claimAmount == null || claimAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("BR-CS-002: Claim amount must be greater than 0");
        }
    }

    /** BR-CS-003: Admission date within active policy period */
    public void validateAdmissionWithinPolicyPeriod(Long policyId, LocalDate dateOfAdmission) {
        if (policyId == null || dateOfAdmission == null) return;
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new BadRequestException("Policy not found"));
        if (policy.getStartDate() != null && dateOfAdmission.isBefore(policy.getStartDate())) {
            throw new BadRequestException("BR-CS-003: Date of admission must be within active policy period");
        }
        if (policy.getEndDate() != null && dateOfAdmission.isAfter(policy.getEndDate())) {
            throw new BadRequestException("BR-CS-003: Date of admission must be within active policy period");
        }
    }

    /** BR-CS-004: Discharge date >= admission date */
    public void validateDischargeAfterAdmission(LocalDate dateOfAdmission, LocalDate dateOfDischarge) {
        if (dateOfAdmission != null && dateOfDischarge != null && dateOfDischarge.isBefore(dateOfAdmission)) {
            throw new BadRequestException("BR-CS-004: Discharge date must be on or after admission date");
        }
    }

    /** BR-CS-005: Reimbursement requires bank details */
    public void validateReimbursementBankDetails(ClaimType claimType, String bankAccountNumber, String accountHolderName, String ifscCode) {
        if (claimType != ClaimType.REIMBURSEMENT) return;
        if (bankAccountNumber == null || bankAccountNumber.isBlank()
                || accountHolderName == null || accountHolderName.isBlank()
                || ifscCode == null || ifscCode.isBlank()) {
            throw new BadRequestException("BR-CS-005: Reimbursement claims require bank account number, account holder name, and IFSC code");
        }
    }

    /** BR-CS-007: Follow-up claims must reference valid parent_claim_id */
    public void validateFollowUpParentClaim(ClaimCategory claimCategory, UUID parentClaimId) {
        if (claimCategory != ClaimCategory.FOLLOW_UP) return;
        if (parentClaimId == null) {
            throw new BadRequestException("BR-CS-007: Follow-up claims must reference a valid parent_claim_id");
        }
        if (claimRepository.findById(parentClaimId).isEmpty()) {
            throw new BadRequestException("BR-CS-007: Parent claim not found");
        }
    }

    /** BR-CS-008: Employee can only edit DRAFT claims */
    public void validateEmployeeCanEdit(Claim claim, UUID employeeId) {
        if (claim.getInternalStatus() != ClaimStatus.DRAFT) {
            throw new BadRequestException("BR-CS-008: Employee can only edit claims in DRAFT status");
        }
        if (!Objects.equals(claim.getEmployee().getIndividualId(), employeeId)) {
            throw new BadRequestException("BR-CS-008: Employee can only edit their own claims");
        }
    }

    /** BR-CS-010: REJECTED / REJECTED_BY_ADMIN require rejection reason (remark or notes). */
    public void validateStatusUpdateRequest(com.vimainsurance.vimaadmin.dto.claim.StatusUpdateRequest request) {
        if (request.getNewStatus() == ClaimStatus.REJECTED || request.getNewStatus() == ClaimStatus.REJECTED_BY_ADMIN) {
            boolean hasReason = (request.getRemark() != null && !request.getRemark().isBlank())
                    || (request.getNotes() != null && !request.getNotes().isBlank());
            if (!hasReason) {
                throw new BadRequestException("Rejection reason (remark or notes) is required when status is REJECTED or REJECTED_BY_ADMIN");
            }
        }
    }

    /** BR-CS-009: Employee can only cancel DRAFT/PENDING_REVIEW */
    public void validateEmployeeCanCancel(Claim claim, UUID employeeId) {
        if (claim.getInternalStatus() != ClaimStatus.DRAFT && claim.getInternalStatus() != ClaimStatus.PENDING_REVIEW) {
            throw new BadRequestException("BR-CS-009: Employee can only cancel claims in DRAFT or PENDING_REVIEW status");
        }
        if (!Objects.equals(claim.getEmployee().getIndividualId(), employeeId)) {
            throw new BadRequestException("BR-CS-009: Employee can only cancel their own claims");
        }
    }

    /** Run all submission validations */
    public void validateSubmission(ClaimSubmissionRequest request, UUID employeeId) {
        validateEmployeeCanSubmitForMember(employeeId, request);
        validateClaimAmount(request.getClaimAmount());
        validateAdmissionWithinPolicyPeriod(request.getPolicyId(), request.getDateOfAdmission());
        validateDischargeAfterAdmission(request.getDateOfAdmission(), request.getDateOfDischarge());
        validateReimbursementBankDetails(request.getClaimType(), request.getBankAccountNumber(),
                request.getAccountHolderName(), request.getIfscCode());
        validateFollowUpParentClaim(request.getClaimCategory(), request.getParentClaimId());
    }
}
