package com.vimainsurance.vimaadmin.service.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.SubmissionDetailDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EmployeePolicyMap;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.entity.Organization;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEmployeePolicyMapRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IPayrollDeductionScheduleRepository;
import com.vimainsurance.vimaadmin.repository.IPolicyRepository;
import com.vimainsurance.vimaadmin.util.OrganizationAccessHelper;

@ExtendWith(MockitoExtension.class)
class HRApprovalServiceImplUnapproveTest {

    @Mock
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;
    @Mock
    private OrganizationAccessHelper organizationAccessHelper;
    @Mock
    private IEmployeePolicyMapRepository employeePolicyMapRepository;
    @Mock
    private IPayrollDeductionScheduleRepository payrollDeductionScheduleRepository;
    @Mock
    private IEnrollmentInvitationRepository enrollmentInvitationRepository;
    @Mock
    private IDealsRepository dealsRepository;
    @Mock
    private IPolicyRepository policyRepository;

    private HRApprovalServiceImpl service;

    private UUID submissionId;
    private UUID orgId;
    private EnrollmentSubmission submission;

    @BeforeEach
    void setUp() {
        service = new HRApprovalServiceImpl();
        ReflectionTestUtils.setField(service, "enrollmentSubmissionRepository", enrollmentSubmissionRepository);
        ReflectionTestUtils.setField(service, "organizationAccessHelper", organizationAccessHelper);
        ReflectionTestUtils.setField(service, "jwtUserExtractor", null);
        ReflectionTestUtils.setField(service, "employeePolicyMapRepository", employeePolicyMapRepository);
        ReflectionTestUtils.setField(service, "payrollDeductionScheduleRepository", payrollDeductionScheduleRepository);
        ReflectionTestUtils.setField(service, "enrollmentInvitationRepository", enrollmentInvitationRepository);
        ReflectionTestUtils.setField(service, "dealsRepository", dealsRepository);
        ReflectionTestUtils.setField(service, "policyRepository", policyRepository);

        doNothing().when(organizationAccessHelper).validateAndSetContext(any(UUID.class));

        submissionId = UUID.randomUUID();
        orgId = UUID.randomUUID();

        Organization org = new Organization();
        org.setOrganizationId(orgId);
        org.setOrganizationName("Acme");

        Deals employee = new Deals();
        employee.setIndividualId(UUID.randomUUID());
        employee.setOrganization(org);
        employee.setFullName("Test Employee");

        EnrollmentWindows window = new EnrollmentWindows();
        window.setId(UUID.randomUUID());
        window.setOrganization(org);
        window.setName("Window");

        submission = new EnrollmentSubmission();
        submission.setId(submissionId);
        submission.setEmployee(employee);
        submission.setEnrollmentWindow(window);
        submission.setStatus(EnrollementStatus.APPROVED);
        submission.setEndorsement(null);

        when(enrollmentSubmissionRepository.findById(submissionId)).thenAnswer(inv -> Optional.of(submission));
    }

    @Test
    void unapprove_whenNotApproved_returns400() {
        submission.setStatus(EnrollementStatus.SUBMITTED);

        ResponseEntity<ResponseDto<SubmissionDetailDto>> res = service.unapprove(submissionId);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        verify(payrollDeductionScheduleRepository, never()).deleteByEnrollmentSubmissionId(any());
    }

    @Test
    void unapprove_whenLinkedToEndorsement_returns400() {
        submission.setEndorsement(new Endorsement());

        ResponseEntity<ResponseDto<SubmissionDetailDto>> res = service.unapprove(submissionId);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        verify(payrollDeductionScheduleRepository, never()).deleteByEnrollmentSubmissionId(any());
    }

    @Test
    void unapprove_happyPath_submittedClearsReviewDeletesPayrollOpensInvitation() {
        when(policyRepository.findByOrganizationId(orgId)).thenReturn(Collections.emptyList());
        when(dealsRepository.findByEnrollmentSubmission_Id(submissionId)).thenReturn(Collections.emptyList());

        UUID invId = UUID.randomUUID();
        EnrollmentInvitation inv = EnrollmentInvitation.builder()
                .id(invId)
                .status(EnrollementStatus.COMPLETED)
                .completedAt(java.time.LocalDateTime.now())
                .build();
        submission.setInvitation(inv);

        EmployeePolicyMap map = EmployeePolicyMap.builder()
                .id(UUID.randomUUID())
                .status("ACTIVE")
                .build();
        when(employeePolicyMapRepository.findByEnrollmentSubmissionIdAndStatus(submissionId, "ACTIVE"))
                .thenReturn(List.of(map));

        ResponseEntity<ResponseDto<SubmissionDetailDto>> res = service.unapprove(submissionId);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(EnrollementStatus.SUBMITTED, submission.getStatus());
        assertNull(submission.getReviewedBy());
        assertNull(submission.getReviewedAt());

        verify(payrollDeductionScheduleRepository).deleteByEnrollmentSubmissionId(submissionId);
        verify(dealsRepository).save(submission.getEmployee());

        assertEquals("CANCELLED", map.getStatus());
        verify(employeePolicyMapRepository).saveAll(anyList());

        assertEquals(EnrollementStatus.OPENED, inv.getStatus());
        assertNull(inv.getCompletedAt());
        verify(enrollmentInvitationRepository).save(inv);
    }
}
