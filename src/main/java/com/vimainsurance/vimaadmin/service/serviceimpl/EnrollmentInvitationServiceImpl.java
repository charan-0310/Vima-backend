package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EnrollmentInvitationResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.mapper.EnrollmentInvitationMapper;
import com.vimainsurance.vimaadmin.service.IEnrollmentInvitation;
import com.vimainsurance.vimaadmin.util.TransactionUtil;

@Service
public class EnrollmentInvitationServiceImpl implements IEnrollmentInvitation {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentInvitationServiceImpl.class);
    private static final int DEFAULT_EXPIRY_DAYS = 7;

    @Autowired
    private IEnrollmentInvitationRepository invitationRepository;
    @Autowired
    private IEnrollmentWindowsRepository enrollmentWindowsRepository;
    @Autowired
    private IDealsRepository dealsRepository;
    @Autowired
    private IEmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> sendInvitation(UUID employeeId, UUID windowId) {
        BaseResponse<EnrollmentInvitationResponseDto> responseObj = new BaseResponse<>();
        try {
            if (invitationRepository.existsByEmployee_IndividualIdAndEnrollmentWindow_Id(employeeId, windowId)) {
                return responseObj.render(responseObj.formErrorResponse("Invitation already exists for this employee and window"));
            }
            Deals employee = dealsRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                return responseObj.render(responseObj.formErrorResponse("Employee not found"));
            }
            if(employee.getEmail() == null || employee.getEmail().isBlank()) {
                return responseObj.render(responseObj.formErrorResponse("Employee email is required"));
            }
            if(employee.getRelationship() != null && !"SELF".equals(employee.getRelationship())) {
                return responseObj.render(responseObj.formErrorResponse("Only Self relationship is allowed"));
            }
            EnrollmentWindows window = enrollmentWindowsRepository.findById(windowId).orElse(null);
            if (window == null) {
                return responseObj.render(responseObj.formErrorResponse("Enrollment window not found"));
            }

            // TODO: Replace with TokenSecurityService.generateToken() then TokenSecurityService.hashToken(token)
            // String rawToken = tokenSecurityService.generateToken();
            // String tokenHash = tokenSecurityService.hashToken(rawToken);
            String placeholderHash = "PLACEHOLDER-" + UUID.randomUUID();
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS);

            EnrollmentInvitation invitation = EnrollmentInvitation.builder()
                .enrollmentWindow(window)
                .employee(employee)
                .tokenHash(placeholderHash)
                .status(EnrollementStatus.PENDING)
                .expiresAt(expiresAt)
                .reminderCount(0)
                .build();
            invitation = invitationRepository.save(invitation);

            // Send email with magic link (link commented out until TokenSecurityService is available)
            // String magicLink = baseUrl + "/enrollment?token=" + rawToken;
            String magicLinkPlaceholder = baseUrl + "/enrollment?token=PLACEHOLDER";
            boolean emailSent = sendEnrollmentInvitationEmail(employee.getEmail(), employee.getFullName(), magicLinkPlaceholder);
            if (emailSent) {
                invitation.setStatus(EnrollementStatus.SENT);
                invitation.setSentAt(LocalDateTime.now());
                invitationRepository.save(invitation);
            } else {
                logger.warn("[correlationId:{}] Invitation created but email failed for employee {}", MDC.get("correlationId"), employeeId);
                TransactionUtil.markRollbackOnly();
                return responseObj.render(responseObj.formErrorResponse("Email sending failed"));
            }

            EnrollmentInvitationResponseDto dto = EnrollmentInvitationMapper.toDto(invitation);
            return responseObj.render(responseObj.formSuccessResponse("Invitation sent", dto));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] sendInvitation failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<IEnrollmentInvitation.BulkInvitationResult>> sendBulkInvitations(List<UUID> employeeIds, UUID windowId) {
        BaseResponse<IEnrollmentInvitation.BulkInvitationResult> responseObj = new BaseResponse<>();
        try {
            if (enrollmentWindowsRepository.findById(windowId).isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Enrollment window not found"));
            }
            List<UUID> failed = new ArrayList<>();
            int sent = 0;
            for (UUID employeeId : employeeIds) {
                try {
                    if (invitationRepository.existsByEmployee_IndividualIdAndEnrollmentWindow_Id(employeeId, windowId)) {
                        failed.add(employeeId);
                        continue;
                    }
                    ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> single = sendInvitation(employeeId, windowId);
                    ResponseDto<EnrollmentInvitationResponseDto> body = single != null ? single.getBody() : null;
                    if (body != null && body.getErrorCode() == null && body.getPayload() != null) {
                        sent++;
                    } else {
                        failed.add(employeeId);
                    }
                } catch (Exception e) {
                    logger.warn("[correlationId:{}] Bulk invite failed for employee {}: {}", MDC.get("correlationId"), employeeId, e.getMessage());
                    failed.add(employeeId);
                }
            }
            IEnrollmentInvitation.BulkInvitationResult result = new IEnrollmentInvitation.BulkInvitationResult(sent, failed.size(), failed);
            return responseObj.render(responseObj.formSuccessResponse("Bulk invitations processed", result));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] sendBulkInvitations failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<IEnrollmentInvitation.ReminderResult>> sendReminders(UUID windowId) {
        BaseResponse<IEnrollmentInvitation.ReminderResult> responseObj = new BaseResponse<>();
        try {
            List<EnrollmentInvitation> all = invitationRepository.findAllByEnrollmentWindow_Id(windowId);
            List<EnrollementStatus> reminderStatuses = List.of(EnrollementStatus.SENT, EnrollementStatus.OPENED, EnrollementStatus.IN_PROGRESS);
            List<EnrollmentInvitation> invitations = all.stream()
                .filter(inv -> reminderStatuses.contains(inv.getStatus()))
                .toList();
            int processed = invitations.size();
            int sent = 0;
            int failed = 0;
            for (EnrollmentInvitation inv : invitations) {
                if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(LocalDateTime.now())) {
                    continue;
                }
                String magicLinkPlaceholder = baseUrl + "/enrollment?token=PLACEHOLDER";
                boolean emailSent = sendEnrollmentReminderEmail(inv.getEmployee().getEmail(), inv.getEmployee().getFullName(), magicLinkPlaceholder);
                if (emailSent) {
                    inv.setReminderCount(inv.getReminderCount() == null ? 1 : inv.getReminderCount() + 1);
                    inv.setLastReminderAt(LocalDateTime.now());
                    invitationRepository.save(inv);
                    sent++;
                } else {
                    failed++;
                }
            }
            IEnrollmentInvitation.ReminderResult result = new IEnrollmentInvitation.ReminderResult(processed, sent, failed);
            return responseObj.render(responseObj.formSuccessResponse("Reminders processed", result));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] sendReminders failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> extendDeadline(UUID invitationId, LocalDateTime newExpiresAt) {
        BaseResponse<EnrollmentInvitationResponseDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentInvitation inv = invitationRepository.findById(invitationId).orElse(null);
            if (inv == null) {
                return responseObj.render(responseObj.formErrorResponse("Invitation not found"));
            }
            inv.setExpiresAt(newExpiresAt);
            inv = invitationRepository.save(inv);
            return responseObj.render(responseObj.formSuccessResponse("Deadline extended", EnrollmentInvitationMapper.toDto(inv)));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] extendDeadline failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> extendDeadlineByEmployeeAndWindow(UUID employeeId, UUID windowId, LocalDateTime newExpiresAt) {
        BaseResponse<EnrollmentInvitationResponseDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentInvitation inv = invitationRepository.findByEmployee_IndividualIdAndEnrollmentWindow_Id(employeeId, windowId).orElse(null);
            if (inv == null) {
                return responseObj.render(responseObj.formErrorResponse("Invitation not found for this employee and window"));
            }
            if (inv.getExpiresAt() != null && inv.getExpiresAt().isAfter(newExpiresAt)) {
                return responseObj.render(responseObj.formErrorResponse("New expiration date is before the current expiration date"));
            }
            inv.setExpiresAt(newExpiresAt);
            inv = invitationRepository.save(inv);
            return responseObj.render(responseObj.formSuccessResponse("Deadline extended", EnrollmentInvitationMapper.toDto(inv)));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] extendDeadlineByEmployeeAndWindow failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Page<EnrollmentInvitationResponseDto>>> listInvitations(UUID windowId, String status, Pageable pageable) {
        BaseResponse<Page<EnrollmentInvitationResponseDto>> responseObj = new BaseResponse<>();
        try {
            Page<EnrollmentInvitation> page;
            if (windowId != null && status != null && !status.isBlank()) {
                EnrollementStatus s = EnrollementStatus.valueOf(status);
                page = invitationRepository.findAllByEnrollmentWindow_IdAndStatus(windowId, s, pageable);
            } else if (windowId != null) {
                page = invitationRepository.findAllByEnrollmentWindow_Id(windowId, pageable);
            } else {
                page = invitationRepository.findAll(pageable);
            }
            Page<EnrollmentInvitationResponseDto> dtos = page.map(EnrollmentInvitationMapper::toDto);
            return responseObj.render(responseObj.formSuccessResponse("OK", dtos));
        } catch (Exception e) {
            logger.error("[correlationId:{}] listInvitations failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    private boolean sendEnrollmentInvitationEmail(String to, String employeeName, String magicLink) {
        try {
            EmailRequest req = EmailRequest.builder()
                .to(to)
                .subject("Enrollment invitation - Vima Insurance")
                .templateName("enrollment-invitation")
                .templateVariables(java.util.Map.of(
                    "employeeName", employeeName != null ? employeeName : "Employee",
                    "magicLink", magicLink,
                    "companyName", "Vima Insurance"
                ))
                .build();
            return emailService.sendTemplateEmail(req).isSuccess();
        } catch (Exception e) {
            logger.warn("Enrollment invitation email failed for {}: {}", to, e.getMessage());
            return false;
        }
    }

    private boolean sendEnrollmentReminderEmail(String to, String employeeName, String magicLink) {
        try {
            EmailRequest req = EmailRequest.builder()
                .to(to)
                .subject("Reminder: Complete your enrollment - Vima Insurance")
                .templateName("enrollment-invitation")
                .templateVariables(java.util.Map.of(
                    "employeeName", employeeName != null ? employeeName : "Employee",
                    "magicLink", magicLink,
                    "companyName", "Vima Insurance",
                    "isReminder", true
                ))
                .build();
            return emailService.sendTemplateEmail(req).isSuccess();
        } catch (Exception e) {
            logger.warn("Enrollment reminder email failed for {}: {}", to, e.getMessage());
            return false;
        }
    }
}
