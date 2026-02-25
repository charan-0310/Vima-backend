package com.vimainsurance.vimaadmin.service.claim;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class ClaimValidationServiceTest {

    @Mock
    private IClaimRepository claimRepository;

    @Mock
    private IDealsRepository dealsRepository;

    @Mock
    private IPolicyRepository policyRepository;

    private ClaimValidationService validationService;

    private UUID employeeId;
    private Deals employee;
    private ClaimSubmissionRequest request;

    @BeforeEach
    void setUp() {
        validationService = new ClaimValidationService(claimRepository, dealsRepository, policyRepository);
        employeeId = UUID.randomUUID();
        employee = new Deals();
        employee.setIndividualId(employeeId);
        request = new ClaimSubmissionRequest();
        request.setOrganizationId(UUID.randomUUID());
        request.setPolicyId(1L);
        request.setClaimAmount(BigDecimal.valueOf(1000));
        request.setDateOfAdmission(LocalDate.of(2025, 1, 15));
        request.setDateOfDischarge(LocalDate.of(2025, 1, 20));
        request.setClaimType(ClaimType.CASHLESS);
    }

    @Test
    void validateClaimAmount_positive_doesNotThrow() {
        assertDoesNotThrow(() -> validationService.validateClaimAmount(BigDecimal.TEN));
    }

    @Test
    void validateClaimAmount_zero_throws() {
        assertThrows(BadRequestException.class, () -> validationService.validateClaimAmount(BigDecimal.ZERO));
    }

    @Test
    void validateClaimAmount_null_throws() {
        assertThrows(BadRequestException.class, () -> validationService.validateClaimAmount(null));
    }

    @Test
    void validateDischargeAfterAdmission_valid_doesNotThrow() {
        assertDoesNotThrow(() -> validationService.validateDischargeAfterAdmission(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 15)));
    }

    @Test
    void validateDischargeAfterAdmission_dischargeBeforeAdmission_throws() {
        assertThrows(BadRequestException.class, () -> validationService.validateDischargeAfterAdmission(
                LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 10)));
    }

    @Test
    void validateReimbursementBankDetails_reimbursementWithoutBank_throws() {
        assertThrows(BadRequestException.class, () -> validationService.validateReimbursementBankDetails(
                ClaimType.REIMBURSEMENT, null, "Holder", "IFSC0001"));
    }

    @Test
    void validateReimbursementBankDetails_reimbursementWithBank_doesNotThrow() {
        assertDoesNotThrow(() -> validationService.validateReimbursementBankDetails(
                ClaimType.REIMBURSEMENT, "123456", "Holder", "IFSC0001"));
    }

    @Test
    void validateReimbursementBankDetails_cashless_doesNotThrow() {
        assertDoesNotThrow(() -> validationService.validateReimbursementBankDetails(
                ClaimType.CASHLESS, null, null, null));
    }

    @Test
    void validateFollowUpParentClaim_followUpWithoutParent_throws() {
        assertThrows(BadRequestException.class, () -> validationService.validateFollowUpParentClaim(
                ClaimCategory.FOLLOW_UP, null));
    }

    @Test
    void validateFollowUpParentClaim_followUpWithInvalidParent_throws() {
        when(claimRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> validationService.validateFollowUpParentClaim(
                ClaimCategory.FOLLOW_UP, UUID.randomUUID()));
    }

    @Test
    void validateFollowUpParentClaim_followUpWithValidParent_doesNotThrow() {
        when(claimRepository.findById(any(UUID.class))).thenReturn(Optional.of(new Claim()));
        assertDoesNotThrow(() -> validationService.validateFollowUpParentClaim(
                ClaimCategory.FOLLOW_UP, UUID.randomUUID()));
    }

    @Test
    void validateFollowUpParentClaim_notFollowUp_doesNotThrow() {
        assertDoesNotThrow(() -> validationService.validateFollowUpParentClaim(ClaimCategory.FRESH_CLAIM, null));
    }

    @Test
    void validateEmployeeCanEdit_draftOwnClaim_doesNotThrow() {
        Claim claim = new Claim();
        claim.setInternalStatus(ClaimStatus.DRAFT);
        claim.setEmployee(employee);
        assertDoesNotThrow(() -> validationService.validateEmployeeCanEdit(claim, employeeId));
    }

    @Test
    void validateEmployeeCanEdit_notDraft_throws() {
        Claim claim = new Claim();
        claim.setInternalStatus(ClaimStatus.PENDING_REVIEW);
        claim.setEmployee(employee);
        assertThrows(BadRequestException.class, () -> validationService.validateEmployeeCanEdit(claim, employeeId));
    }

    @Test
    void validateEmployeeCanEdit_otherEmployee_throws() {
        Claim claim = new Claim();
        claim.setInternalStatus(ClaimStatus.DRAFT);
        claim.setEmployee(employee);
        assertThrows(BadRequestException.class, () -> validationService.validateEmployeeCanEdit(claim, UUID.randomUUID()));
    }

    @Test
    void validateEmployeeCanCancel_draftOwn_doesNotThrow() {
        Claim claim = new Claim();
        claim.setInternalStatus(ClaimStatus.DRAFT);
        claim.setEmployee(employee);
        assertDoesNotThrow(() -> validationService.validateEmployeeCanCancel(claim, employeeId));
    }

    @Test
    void validateEmployeeCanCancel_submitted_throws() {
        Claim claim = new Claim();
        claim.setInternalStatus(ClaimStatus.SUBMITTED_TO_INSURER);
        claim.setEmployee(employee);
        assertThrows(BadRequestException.class, () -> validationService.validateEmployeeCanCancel(claim, employeeId));
    }

    @Test
    void validateSubmission_employeeSelf_validRequest_doesNotThrow() {
        request.setMemberType(MemberType.EMPLOYEE);
        when(dealsRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(policyRepository.findById(1L)).thenReturn(Optional.of(createPolicy(1L)));
        assertDoesNotThrow(() -> validationService.validateSubmission(request, employeeId));
    }

    @Test
    void validateSubmission_employeeNotFound_throws() {
        when(dealsRepository.findById(employeeId)).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> validationService.validateSubmission(request, employeeId));
    }

    @Test
    void validateAdmissionWithinPolicyPeriod_beforeStart_throws() {
        Policy policy = createPolicy(1L);
        policy.setStartDate(LocalDate.of(2025, 2, 1));
        policy.setEndDate(LocalDate.of(2025, 12, 31));
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        assertThrows(BadRequestException.class, () -> validationService.validateAdmissionWithinPolicyPeriod(
                1L, LocalDate.of(2025, 1, 15)));
    }

    @Test
    void validateAdmissionWithinPolicyPeriod_afterEnd_throws() {
        Policy policy = createPolicy(1L);
        policy.setStartDate(LocalDate.of(2025, 1, 1));
        policy.setEndDate(LocalDate.of(2025, 6, 30));
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        assertThrows(BadRequestException.class, () -> validationService.validateAdmissionWithinPolicyPeriod(
                1L, LocalDate.of(2025, 7, 1)));
    }

    private Policy createPolicy(Long id) {
        Policy p = new Policy();
        p.setPolicyId(id);
        p.setStartDate(LocalDate.of(2025, 1, 1));
        p.setEndDate(LocalDate.of(2025, 12, 31));
        return p;
    }
}
