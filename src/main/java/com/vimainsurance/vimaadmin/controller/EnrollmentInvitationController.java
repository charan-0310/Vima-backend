package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ExtendDeadlineRequestDto;
import com.vimainsurance.vimaadmin.dto.SendBulkInvitationsRequestDto;
import com.vimainsurance.vimaadmin.dto.SendInvitationRequestDto;
import com.vimainsurance.vimaadmin.dto.SendRemindersRequestDto;
import com.vimainsurance.vimaadmin.service.IEnrollmentInvitation;

/**
 * Admin APIs for enrollment invitations: send, bulk send, activate window, resend, reminders, extend deadline, list, progress.
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/admin/enrollments")
public class EnrollmentInvitationController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentInvitationController.class);

    @Autowired
    private IEnrollmentInvitation enrollmentInvitationService;

    /** POST /api/v1/admin/enrollments/windows/{windowId}/activate - Activate window and send invites */
    @PostMapping("/windows/{windowId}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> activateWindowAndSendInvites(@PathVariable UUID windowId) {
        logger.info("[correlationId:{}] POST /api/v1/admin/enrollments/windows/{}/activate", MDC.get("correlationId"), windowId);
        return enrollmentInvitationService.activateWindowAndSendInvites(windowId);
    }

    /** POST /api/v1/admin/enrollments/{invitationId}/resend - Resend single activation link */
    @PostMapping("/{invitationId}/resend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> resendActivationLink(@PathVariable UUID invitationId) {
        logger.info("[correlationId:{}] POST /api/v1/admin/enrollments/{}/resend", MDC.get("correlationId"), invitationId);
        return enrollmentInvitationService.resendActivationLink(invitationId);
    }

    /** POST /api/v1/admin/enrollments/send-invitation - Single or multiple (use employeeIds for bulk) */
    @PostMapping("/send-invitation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> sendInvitation(@RequestBody @Valid SendInvitationRequestDto request) {
        logger.info("[correlationId:{}] POST /api/v1/admin/enrollments/send-invitation", MDC.get("correlationId"));
        return enrollmentInvitationService.sendBulkInvitations(request.getEmployeeIds(), request.getWindowId());
        
    }

    /** POST /api/v1/admin/enrollments/send-bulk - Bulk invites */
    @PostMapping("/send-bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> sendBulkInvitations(@RequestBody @Valid SendBulkInvitationsRequestDto request) {
        logger.info("[correlationId:{}] POST /api/v1/admin/enrollments/send-bulk", MDC.get("correlationId"));
        return enrollmentInvitationService.sendBulkInvitations(request.getEmployeeIds(), request.getWindowId());
    }

    /** POST /api/v1/admin/enrollments/send-reminders - Manual reminder trigger (optional employeeIds to limit to specific employees) */
    @PostMapping("/send-reminders")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> sendReminders(@RequestBody @Valid SendRemindersRequestDto request) {
        logger.info("[correlationId:{}] POST /api/v1/admin/enrollments/send-reminders", MDC.get("correlationId"));
        return enrollmentInvitationService.sendReminders(request.getWindowId(), request.getEmployeeIds());
    }

    /** POST /api/v1/admin/enrollments/extend - Extend deadline for one or more employees (body: windowId, newExpiresAt, employeeIds) */
    @PostMapping("/extend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> extendDeadlineBulk(@RequestBody @Valid ExtendDeadlineRequestDto request) {
        logger.info("[correlationId:{}] POST /api/v1/admin/enrollments/extend", MDC.get("correlationId"));
        List<UUID> ids = request.getEmployeeIds();
        if (ids == null || ids.isEmpty()) {
            return enrollmentInvitationService.extendDeadlineForEmployees(
                    java.util.Collections.emptyList(), request.getWindowId(), request.getNewExpiresAt());
        }
        return enrollmentInvitationService.extendDeadlineForEmployees(
                ids, request.getWindowId(), request.getNewExpiresAt());
    }

    /** POST /api/v1/admin/enrollments/{employeeId}/extend - Extend deadline for single employee (body: windowId, newExpiresAt) */
    @PostMapping("/{employeeId}/extend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> extendDeadlineSingle(
            @PathVariable UUID employeeId,
            @RequestBody @Valid ExtendDeadlineRequestDto request) {
        logger.info("[correlationId:{}] POST /api/v1/admin/enrollments/{}/extend", MDC.get("correlationId"), employeeId);
        return enrollmentInvitationService.extendDeadlineByEmployeeAndWindow(
                employeeId, request.getWindowId(), request.getNewExpiresAt());
    }

    /** GET /api/v1/admin/enrollments/invitations - List with optional windowId, status and pagination (page, size, sort) */
    @GetMapping("/invitations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> listInvitations(
            @RequestParam(required = false) UUID windowId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        logger.info("[correlationId:{}] GET /api/v1/admin/enrollments/invitations", MDC.get("correlationId"));
        return enrollmentInvitationService.listInvitations(windowId, status, pageable);
    }

    /** GET /api/v1/admin/enrollments/windows/{windowId}/progress - Track completion (no endorsement) */
    @GetMapping("/windows/{windowId}/progress")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
    public ResponseEntity<?> getEnrollmentProgress(@PathVariable UUID windowId) {
        logger.info("[correlationId:{}] GET /api/v1/admin/enrollments/windows/{}/progress", MDC.get("correlationId"), windowId);
        return enrollmentInvitationService.getEnrollmentProgress(windowId);
    }
}
