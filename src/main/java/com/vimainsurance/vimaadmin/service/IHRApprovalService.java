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
import com.vimainsurance.vimaadmin.dto.ResponseDto;
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
     * Reject a submission: set status=REJECTED, store reason, mark/delete dependents, optionally reopen invitation, send email.
     */
    ResponseEntity<ResponseDto<SubmissionDetailDto>> reject(UUID id, RejectionRequest request);

    /**
     * Bulk approve submissions in a single transaction.
     */
    ResponseEntity<ResponseDto<List<SubmissionListItemDto>>> bulkApprove(BulkApprovalRequest request);
}
