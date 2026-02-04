package com.vimainsurance.vimaadmin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.EnrollmentInvitationResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IEnrollmentInvitation {

    /**
     * Send a single enrollment invitation to an employee for a window.
     */
    ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> sendInvitation(UUID employeeId, UUID windowId);

    /**
     * Send bulk enrollment invitations (async). Returns immediately with accepted count; failures logged and marked.
     */
    ResponseEntity<ResponseDto<BulkInvitationResult>> sendBulkInvitations(List<UUID> employeeIds, UUID windowId);

    /**
     * Send reminders for a window (e.g. used by scheduled job or manual trigger).
     */
    ResponseEntity<ResponseDto<ReminderResult>> sendReminders(UUID windowId);

    /**
     * Extend invitation expiry deadline by invitation id.
     */
    ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> extendDeadline(UUID invitationId, LocalDateTime newExpiresAt);

    /**
     * Extend invitation expiry deadline by employee and window (for API path /{employeeId}/extend).
     */
    ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> extendDeadlineByEmployeeAndWindow(UUID employeeId, UUID windowId, LocalDateTime newExpiresAt);

    /**
     * List invitations with optional filters (windowId, status) and pagination.
     */
    ResponseEntity<ResponseDto<Page<EnrollmentInvitationResponseDto>>> listInvitations(UUID windowId, String status, Pageable pageable);

    /** Result of bulk send: sent count, failed count, failed employee ids. */
    record BulkInvitationResult(int sent, int failed, List<UUID> failedEmployeeIds) {}

    /** Result of reminder job: processed, sent, failed. */
    record ReminderResult(int processed, int sent, int failed) {}
}
