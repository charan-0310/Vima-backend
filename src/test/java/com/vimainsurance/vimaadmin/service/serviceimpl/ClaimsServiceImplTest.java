package com.vimainsurance.vimaadmin.service.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.ClaimType;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.exception.InvalidStatusTransitionException;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.repository.IOrganizationRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.service.claim.ClaimAuditService;
import com.vimainsurance.vimaadmin.service.claim.ClaimNumberGenerator;
import com.vimainsurance.vimaadmin.service.claim.ClaimStatusTransitionValidator;
import com.vimainsurance.vimaadmin.service.claim.ClaimValidationService;
import com.vimainsurance.vimaadmin.notification.ClaimsNotificationEmitterService;
import com.vimainsurance.vimaadmin.service.claim.notification.ClaimsNotificationService;

@ExtendWith(MockitoExtension.class)
class ClaimsServiceImplTest {

    @Mock
    private IClaimRepository claimRepository;

    @Mock
    private IOrganizationRepository organizationRepository;

    @Mock
    private IDealsRepository dealsRepository;

    @Mock
    private IAdminUserRepository adminUserRepository;

    @Mock
    private IDocumentRepository documentRepository;

    @Mock
    private ClaimNumberGenerator claimNumberGenerator;

    @Mock
    private ClaimStatusTransitionValidator statusValidator;

    @Mock
    private ClaimAuditService auditService;

    @Mock
    private ClaimValidationService validationService;

    @Mock
    private InsurerAdapterFactory adapterFactory;

    @Mock
    private ClaimsNotificationService notificationService;

    @Mock
    private ClaimsNotificationEmitterService claimsNotificationEmitterService;

    @Mock
    private InsurerAdapter insurerAdapter;

    private ClaimsServiceImpl claimsService;

    private UUID claimId;
    private UUID employeeId;
    private Claim claim;
    private Organization org;
    private Deals employee;
    private ClaimSubmissionRequest submissionRequest;

    @BeforeEach
    void setUp() {
        claimsService = new ClaimsServiceImpl(
                claimRepository, organizationRepository, dealsRepository, adminUserRepository, documentRepository,
                claimNumberGenerator, statusValidator, auditService, validationService, adapterFactory, notificationService,
                claimsNotificationEmitterService);

        claimId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        claim = new Claim();
        claim.setId(claimId);
        claim.setClaimNumber("VIMA-CLM-2025-0001");
        claim.setInternalStatus(ClaimStatus.DRAFT);
        claim.setEmployee(new Deals());
        claim.getEmployee().setIndividualId(employeeId);
        claim.setOrganization(new Organization());
        claim.setPolicyId(1L);

        org = new Organization();
        org.setOrganizationId(UUID.randomUUID());
        employee = new Deals();
        employee.setIndividualId(employeeId);

        submissionRequest = new ClaimSubmissionRequest();
        submissionRequest.setOrganizationId(org.getOrganizationId());
        submissionRequest.setPolicyId(1L);
        submissionRequest.setClaimAmount(BigDecimal.valueOf(5000));
        submissionRequest.setDateOfAdmission(LocalDate.of(2025, 1, 10));
        submissionRequest.setDateOfDischarge(LocalDate.of(2025, 1, 15));
        submissionRequest.setClaimType(ClaimType.CASHLESS);
    }

    @Test
    void submitClaim_validRequest_createsClaimInPendingReview() {
        when(claimNumberGenerator.generateNext()).thenReturn("VIMA-CLM-2025-0001");
        when(dealsRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(organizationRepository.findById(any())).thenReturn(Optional.of(org));
        when(adminUserRepository.findById(employeeId)).thenReturn(Optional.empty());
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> {
            Claim c = i.getArgument(0);
            c.setId(claimId);
            return c;
        });
        when(documentRepository.findByEntityTypeAndEntityId(eq(DocumentEntityType.CLAIM), anyString())).thenReturn(Collections.emptyList());

        ClaimDetailsResponse response = claimsService.submitClaim(submissionRequest, employeeId);

        assertNotNull(response);
        assertEquals(ClaimStatus.PENDING_REVIEW, response.getInternalStatus());
        assertEquals("VIMA-CLM-2025-0001", response.getClaimNumber());
        verify(claimRepository).save(any(Claim.class));
        verify(auditService).logAction(eq(claimId), eq("CLAIM_SUBMITTED"), anyString(), anyString(), eq(employeeId), anyString(), anyString(), eq(null), eq(null), eq(null));
        verify(claimsNotificationEmitterService).scheduleClaimSubmitted(any(Claim.class), any(), any(), eq("EMPLOYEE"));
    }

    @Test
    void submitClaim_validationFails_throws() {
        doThrow(new BadRequestException("BR-CS-002")).when(validationService).validateSubmission(any(), eq(employeeId));

        assertThrows(BadRequestException.class, () -> claimsService.submitClaim(submissionRequest, employeeId));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void updateDraftClaim_valid_updatesAndReturns() {
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.findByEntityTypeAndEntityId(eq(DocumentEntityType.CLAIM), anyString())).thenReturn(Collections.emptyList());

        ClaimDetailsResponse response = claimsService.updateDraftClaim(claimId, submissionRequest, employeeId);

        assertNotNull(response);
        verify(validationService).validateEmployeeCanEdit(claim, employeeId);
        verify(claimRepository).save(claim);
    }

    @Test
    void updateDraftClaim_claimNotFound_throws() {
        when(claimRepository.findById(claimId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> claimsService.updateDraftClaim(claimId, submissionRequest, employeeId));
    }

    @Test
    void cancelClaim_valid_setsClosedAndDeleted() {
        claim.setInternalStatus(ClaimStatus.PENDING_REVIEW);
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        claimsService.cancelClaim(claimId, employeeId);

        verify(claimRepository).save(any(Claim.class));
        verify(auditService).logAction(eq(claimId), eq("CLAIM_CANCELLED"), anyString(), eq(ClaimStatus.CLOSED.getValue()), eq(employeeId), anyString(), anyString(), eq(null), eq(null), eq(null));
    }

    @Test
    void cancelClaim_validationFails_throws() {
        claim.setInternalStatus(ClaimStatus.SUBMITTED_TO_INSURER);
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        doThrow(new BadRequestException("BR-CS-009")).when(validationService).validateEmployeeCanCancel(claim, employeeId);

        assertThrows(BadRequestException.class, () -> claimsService.cancelClaim(claimId, employeeId));
    }

    @Test
    void submitToInsurer_approvedForSubmission_manualAdapter_setsApprovedForSubmission() {
        claim.setInternalStatus(ClaimStatus.APPROVED_FOR_SUBMISSION);
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(adapterFactory.getAdapter(claim)).thenReturn(insurerAdapter);
        when(insurerAdapter.submitToInsurer(claim)).thenReturn(InsurerSubmissionResult.builder()
                .success(true)
                .requiresManualSubmission(true)
                .message("Manual")
                .build());
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.findByEntityTypeAndEntityId(eq(DocumentEntityType.CLAIM), anyString())).thenReturn(Collections.emptyList());

        ClaimDetailsResponse response = claimsService.submitToInsurer(claimId);

        assertNotNull(response);
        assertEquals(ClaimStatus.APPROVED_FOR_SUBMISSION, response.getInternalStatus());
        verify(adapterFactory).getAdapter(claim);
    }

    @Test
    void submitToInsurer_notApprovedForSubmission_throws() {
        claim.setInternalStatus(ClaimStatus.PENDING_REVIEW);
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));

        assertThrows(BadRequestException.class, () -> claimsService.submitToInsurer(claimId));
        verify(adapterFactory, never()).getAdapter(any(Claim.class));
    }

    @Test
    void updateStatus_valid_transitionsAndAudits() {
        claim.setInternalStatus(ClaimStatus.PENDING_REVIEW);
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.findByEntityTypeAndEntityId(eq(DocumentEntityType.CLAIM), anyString())).thenReturn(Collections.emptyList());

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setNewStatus(ClaimStatus.APPROVED_FOR_SUBMISSION);
        ClaimDetailsResponse response = claimsService.updateStatus(claimId, req, employeeId, "ADMIN");

        assertNotNull(response);
        verify(statusValidator).validateTransition(ClaimStatus.PENDING_REVIEW, ClaimStatus.APPROVED_FOR_SUBMISSION);
        verify(auditService).logAction(eq(claimId), eq("STATUS_UPDATE"), eq(ClaimStatus.PENDING_REVIEW.getValue()), eq(ClaimStatus.APPROVED_FOR_SUBMISSION.getValue()), eq(employeeId), eq("ADMIN"), eq(null), eq(null), eq(null), eq(null));
    }

    @Test
    void updateStatus_invalidTransition_throws() {
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setNewStatus(ClaimStatus.SUBMITTED_TO_INSURER);
        doThrow(new InvalidStatusTransitionException(ClaimStatus.DRAFT, ClaimStatus.SUBMITTED_TO_INSURER, java.util.Set.of())).when(statusValidator).validateTransition(ClaimStatus.DRAFT, ClaimStatus.SUBMITTED_TO_INSURER);

        assertThrows(InvalidStatusTransitionException.class, () -> claimsService.updateStatus(claimId, req, employeeId, "ADMIN"));
    }

    @Test
    void updateInsurerRef_updatesFields() {
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.findByEntityTypeAndEntityId(eq(DocumentEntityType.CLAIM), anyString())).thenReturn(Collections.emptyList());

        InsurerRefRequest req = new InsurerRefRequest();
        req.setInsurerClaimRef("INS-REF-001");
        req.setInsurerClaimNumber("ICN-001");
        ClaimDetailsResponse response = claimsService.updateInsurerRef(claimId, req);

        assertNotNull(response);
        verify(claimRepository).save(claim);
    }

    @Test
    void getClaimDetails_found_returnsDetails() {
        when(claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claimId)).thenReturn(Optional.of(claim));
        when(documentRepository.findByEntityTypeAndEntityId(DocumentEntityType.CLAIM, claimId.toString())).thenReturn(Collections.emptyList());

        ClaimDetailsResponse response = claimsService.getClaimDetails(claimId);

        assertNotNull(response);
        assertEquals(claimId, response.getId());
        assertEquals("VIMA-CLM-2025-0001", response.getClaimNumber());
    }

    @Test
    void getClaimDetails_notFound_throws() {
        when(claimRepository.findByIdWithOrganizationAndEmployeeAndSettlement(claimId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> claimsService.getClaimDetails(claimId));
    }

    @Test
    void listClaims_returnsPaginatedResults() {
        List<Claim> claims = List.of(claim);
        Page<Claim> page = new PageImpl<>(claims);
        when(claimRepository.findAllWithOrganizationAndEmployee(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<ClaimDetailsResponse> result = claimsService.listClaims(new ClaimListFilters(), Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(claimId, result.getContent().get(0).getId());
    }
}
