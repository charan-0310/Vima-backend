package com.vimainsurance.vimaadmin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ActivateWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentInvitationResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentProgressResponseDto;
import com.vimainsurance.vimaadmin.dto.ExtendDeadlineResultDto;
import com.vimainsurance.vimaadmin.dto.InvitationLinkResponseDto;
import com.vimainsurance.vimaadmin.dto.ResendInvitationResponseDto;
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
     * Resend activation link for a single invitation (e.g. after email failure).
     */
    ResponseEntity<ResponseDto<ResendInvitationResponseDto>> resendActivationLink(UUID invitationId);

    /**
     * Send reminders for a window. When employeeIds is non-null and non-empty, send only to those employees; otherwise all eligible in the window.
     */
    ResponseEntity<ResponseDto<ReminderResult>> sendReminders(UUID windowId, List<UUID> employeeIds);

    /**
     * Extend invitation expiry deadline by invitation id.
     */
    ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> extendDeadline(UUID invitationId, LocalDateTime newExpiresAt);

    /**
     * Extend invitation expiry deadline by employee and window (for API path /{employeeId}/extend).
     */
    ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> extendDeadlineByEmployeeAndWindow(UUID employeeId, UUID windowId, LocalDateTime newExpiresAt);

    /**
     * Extend invitation expiry deadline for multiple employees in a window.
     */
    ResponseEntity<ResponseDto<ExtendDeadlineResultDto>> extendDeadlineForEmployees(List<UUID> employeeIds, UUID windowId, LocalDateTime newExpiresAt);

    /**
     * List invitations with optional filters (windowId, status) and pagination.
     */
    ResponseEntity<ResponseDto<Page<EnrollmentInvitationResponseDto>>> listInvitations(UUID windowId, String status, Pageable pageable);

    /**
     * Get the enrollment (magic) link for an invitation. Only available when token is deterministic (new invitations).
     */
    ResponseEntity<ResponseDto<InvitationLinkResponseDto>> getInvitationLink(UUID invitationId);

    /**
     * Activate enrollment window (scheduled → active) and send invitations to all employees linked to the window.
     */
    ResponseEntity<ResponseDto<ActivateWindowResponseDto>> activateWindowAndSendInvites(UUID windowId);

    /**
     * Get enrollment progress for a window (no endorsement): counts and employee details.
     */
    ResponseEntity<ResponseDto<EnrollmentProgressResponseDto>> getEnrollmentProgress(UUID windowId);

    /** Result of bulk send: sent count, failed count, and detailed failures (employeeNumber, name, error). */
    record BulkInvitationResult(int sent, int failed, List<com.vimainsurance.vimaadmin.dto.FailedInvitationDto> failedDetails) {}

    /** Result of reminder job: processed, sent, failed. */
    record ReminderResult(int processed, int sent, int failed) {}
}
