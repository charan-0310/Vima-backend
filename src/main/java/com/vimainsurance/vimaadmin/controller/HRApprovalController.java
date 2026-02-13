package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ApprovalRequest;
import com.vimainsurance.vimaadmin.dto.BulkApprovalRequest;
import com.vimainsurance.vimaadmin.dto.RejectionRequest;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.SubmissionDetailDto;
import com.vimainsurance.vimaadmin.dto.SubmissionListItemDto;
import com.vimainsurance.vimaadmin.service.IHRApprovalService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;

/**
 * HR portal controller for reviewing and approving enrollment submissions.
 * GET /api/v1/hr/enrollments - list with filters and pagination
 * GET /api/v1/hr/enrollments/{id} - detail
 * POST /api/v1/hr/enrollments/{id}/approve - approve
 * POST /api/v1/hr/enrollments/{id}/reject - reject
 * POST /api/v1/hr/enrollments/bulk-approve - bulk approve
 */
@RestController
@RequestMapping("/api/v1/hr/enrollments")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
public class HRApprovalController {

    private static final Logger logger = LoggerFactory.getLogger(HRApprovalController.class);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private IHRApprovalService hrApprovalService;

    /**
     * List enrollment submissions with filters and pagination.
     * HR_ADMIN sees only their company (organization IDs from tenant context).
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Page<SubmissionListItemDto>>> getEnrollments(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        logger.info("[correlationId:{}] GET /api/v1/hr/enrollments", MDC.get("correlationId"));

        LocalDateTime from = parseDateTime(submittedFrom, true);
        LocalDateTime to = parseDateTime(submittedTo, false);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String orderBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "submittedAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, orderBy));

        return hrApprovalService.getEnrollments(companyId, status, search, from, to, pageable);
    }

    /**
     * Get enrollment submission detail by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<SubmissionDetailDto>> getEnrollmentDetail(@PathVariable UUID id) {
        logger.info("[correlationId:{}] GET /api/v1/hr/enrollments/{}", MDC.get("correlationId"), id);
        return hrApprovalService.getEnrollmentDetail(id);
    }

    /**
     * Approve an enrollment submission.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ResponseDto<SubmissionDetailDto>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalRequest request) {
        logger.info("[correlationId:{}] POST /api/v1/hr/enrollments/{}/approve", MDC.get("correlationId"), id);
        return hrApprovalService.approve(id, request != null ? request : new ApprovalRequest());
    }

    /**
     * Reject an enrollment submission.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ResponseDto<SubmissionDetailDto>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectionRequest request) {
        logger.info("[correlationId:{}] POST /api/v1/hr/enrollments/{}/reject", MDC.get("correlationId"), id);
        return hrApprovalService.reject(id, request);
    }

    /**
     * Bulk approve multiple enrollment submissions (single transaction).
     */
    @PostMapping("/bulk-approve")
    public ResponseEntity<ResponseDto<List<SubmissionListItemDto>>> bulkApprove(
            @Valid @RequestBody BulkApprovalRequest request) {
        logger.info("[correlationId:{}] POST /api/v1/hr/enrollments/bulk-approve", MDC.get("correlationId"));
        return hrApprovalService.bulkApprove(request);
    }

    private static LocalDateTime parseDateTime(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                LocalDate date = LocalDate.parse(value, ISO_DATE);
                return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
