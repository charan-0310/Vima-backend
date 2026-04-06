package com.vimainsurance.vimaadmin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ApprovalRequest;
import com.vimainsurance.vimaadmin.dto.BulkApprovalRequest;
import com.vimainsurance.vimaadmin.dto.RejectionRequest;
import com.vimainsurance.vimaadmin.dto.ResendRequest;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.UpdateEnrollmentEmployeeRequest;
import com.vimainsurance.vimaadmin.dto.SubmissionDetailDto;
import com.vimainsurance.vimaadmin.dto.SubmissionListItemDto;

public interface IHRApprovalService {

    /**
     * List enrollment submissions with filters and pagination. HR_ADMIN sees only their company (organization).
     */
    ResponseEntity<ResponseDto<Page<SubmissionListItemDto>>> getEnrollments(
            String companyId,
            String status,
            String search,
            LocalDateTime submittedFrom,
            LocalDateTime submittedTo,
            Pageable pageable);

    /**
     * Get enrollment submission detail by ID. Access restricted to user's organizations.
     */
    ResponseEntity<ResponseDto<SubmissionDetailDto>> getEnrollmentDetail(UUID id);

    /**
     * Approve a submission: set status=APPROVED, update dependents to ACTIVE, set reviewed_by/at, send email.
     */
    ResponseEntity<ResponseDto<SubmissionDetailDto>> approve(UUID id, ApprovalRequest request);

    /**
     * Revoke HR approval: set status back to SUBMITTED, clear review fields, cancel policy maps and payroll rows
     * created from this submission, reopen invitation. Not allowed if submission is endorsed or linked to an endorsement.
     */
    ResponseEntity<ResponseDto<SubmissionDetailDto>> unapprove(UUID id);

    /**
     * Reject a submission: set status=REJECTED, store reason, mark/delete dependents, optionally reopen invitation, send email.
     */
    ResponseEntity<ResponseDto<SubmissionDetailDto>> reject(UUID id, RejectionRequest request);

    /**
     * Resend a submitted enrollment: reset submission to DRAFT, set invitation to SENT, send reminder email with HR notes.
     */
    ResponseEntity<ResponseDto<SubmissionDetailDto>> resend(UUID id, ResendRequest request);

    /**
     * Update primary employee demographics for an enrollment submission (Deals + personal_details JSON).
     */
    ResponseEntity<ResponseDto<SubmissionDetailDto>> updateEnrollmentEmployee(UUID submissionId, UpdateEnrollmentEmployeeRequest request);

    /**
     * Bulk approve submissions in a single transaction.
     */
    ResponseEntity<ResponseDto<List<SubmissionListItemDto>>> bulkApprove(BulkApprovalRequest request);

    /**
     * Finalize enrollment window on close: ensure all submissions are approved or rejected,
     * then for each approved submission update employee from personalDetails, create dependents and nominees from JSON,
     * and create endorsement + deal endorsements for the window. Call this before closing the window.
     *
     * @return success or 400 if any submission is still SUBMITTED/DRAFT
     */
    ResponseEntity<ResponseDto<String>> finalizeEnrollmentWindow(UUID windowId);
}
