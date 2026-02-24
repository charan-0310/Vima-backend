package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.config.AsyncConfig;
import com.vimainsurance.vimaadmin.dto.ActivateWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EmailRequest;
import com.vimainsurance.vimaadmin.dto.EmployeeProgressDetailDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentInvitationResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentProgressResponseDto;
import com.vimainsurance.vimaadmin.dto.ExtendDeadlineResultDto;
import com.vimainsurance.vimaadmin.dto.FailedInvitationDto;
import com.vimainsurance.vimaadmin.dto.InvitationLinkResponseDto;
import com.vimainsurance.vimaadmin.dto.ResendInvitationResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.mapper.EnrollmentInvitationMapper;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.service.IEmailService;
import com.vimainsurance.vimaadmin.service.IEnrollmentInvitation;
import com.vimainsurance.vimaadmin.service.TokenSecurityService;
import com.vimainsurance.vimaadmin.util.TransactionUtil;

@Service
public class EnrollmentInvitationServiceImpl implements IEnrollmentInvitation {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentInvitationServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private IEnrollmentInvitationRepository invitationRepository;
    @Autowired
    private IEnrollmentWindowsRepository enrollmentWindowsRepository;
    @Autowired
    private IDealsRepository dealsRepository;
    @Autowired
    private IEnrollmentSubmissionRepository submissionRepository;
    @Autowired
    private IEmailService emailService;
    @Autowired
    private TokenSecurityService tokenSecurityService;

    @Autowired
    @Qualifier(AsyncConfig.ENROLLMENT_BULK_EXECUTOR)
    private Executor enrollmentBulkExecutor;

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

            // Token valid until window end; deterministic so reminders can resend the same link
            UUID invId = UUID.randomUUID();
            String rawToken = tokenSecurityService.generateTokenForInvitation(invId);
            String tokenHash = tokenSecurityService.hashToken(rawToken);
            LocalDateTime expiresAt = window.getEndDate().atTime(23, 59, 59);

            EnrollmentInvitation invitation = EnrollmentInvitation.builder()
                .id(invId)
                .enrollmentWindow(window)
                .employee(employee)
                .tokenHash(tokenHash)
                .status(EnrollementStatus.PENDING)
                .expiresAt(expiresAt)
                .reminderCount(0)
                .tokenDeterministic(true)
                .build();
            invitation = invitationRepository.save(invitation);

            String magicLink = baseUrl + "/enrollment/" + rawToken;
            boolean emailSent = sendEnrollmentInvitationEmail(employee.getEmail(), employee.getFullName(), magicLink);
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
            dto.setMagicLink(magicLink);
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
            List<CompletableFuture<FailedInvitationDto>> futures = employeeIds.stream()
                .map(employeeId -> CompletableFuture.supplyAsync(() -> {
                    Optional<Deals> dealOpt = dealsRepository.findById(employeeId);
                    String employeeName = dealOpt.map(Deals::getFullName).filter(n -> n != null && !n.isBlank()).orElse("Unknown");
                    String employeeNumber = dealOpt.map(Deals::getEmployeeNumber).filter(n -> n != null && !n.isBlank()).orElse(null);
                    try {
                        if (invitationRepository.existsByEmployee_IndividualIdAndEnrollmentWindow_Id(employeeId, windowId)) {
                            return FailedInvitationDto.builder()
                                .employeeNumber(employeeNumber)
                                .employeeName(employeeName)
                                .error("Invitation already exists for this employee and window")
                                .build();
                        }
                        ResponseEntity<ResponseDto<EnrollmentInvitationResponseDto>> single = sendInvitation(employeeId, windowId);
                        ResponseDto<EnrollmentInvitationResponseDto> body = single != null ? single.getBody() : null;
                        if (body != null && body.getErrorCode() == null && body.getPayload() != null) {
                            return null;
                        }
                        String error = (body != null && body.getMessage() != null) ? body.getMessage() : "Invitation send failed";
                        return FailedInvitationDto.builder()
                            .employeeNumber(employeeNumber)
                            .employeeName(employeeName)
                            .error(error)
                            .build();
                    } catch (Exception e) {
                        logger.warn("[correlationId:{}] Bulk invite failed for employee {}: {}", MDC.get("correlationId"), employeeId, e.getMessage());
                        return FailedInvitationDto.builder()
                            .employeeNumber(employeeNumber)
                            .employeeName(employeeName)
                            .error(e.getMessage() != null ? e.getMessage() : "Unexpected error")
                            .build();
                    }
                }, enrollmentBulkExecutor))
                .toList();
            List<FailedInvitationDto> failedDetails = new ArrayList<>();
            for (CompletableFuture<FailedInvitationDto> f : futures) {
                try {
                    FailedInvitationDto detail = f.get();
                    if (detail != null) {
                        failedDetails.add(detail);
                    }
                } catch (Exception e) {
                    logger.warn("[correlationId:{}] Bulk invite future failed: {}", MDC.get("correlationId"), e.getMessage());
                }
            }
            int sent = employeeIds.size() - failedDetails.size();
            IEnrollmentInvitation.BulkInvitationResult result = new IEnrollmentInvitation.BulkInvitationResult(sent, failedDetails.size(), failedDetails);
            return responseObj.render(responseObj.formSuccessResponse("Bulk invitations processed", result));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] sendBulkInvitations failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<IEnrollmentInvitation.ReminderResult>> sendReminders(UUID windowId, List<UUID> employeeIds) {
        BaseResponse<IEnrollmentInvitation.ReminderResult> responseObj = new BaseResponse<>();
        try {
            List<EnrollmentInvitation> all = invitationRepository.findAllByEnrollmentWindow_Id(windowId);
            List<EnrollementStatus> reminderStatuses = List.of(EnrollementStatus.SENT, EnrollementStatus.OPENED, EnrollementStatus.IN_PROGRESS, EnrollementStatus.REJECTED);
            List<EnrollmentInvitation> invitations = all.stream()
                .filter(inv -> reminderStatuses.contains(inv.getStatus()))
                .filter(inv -> employeeIds == null || employeeIds.isEmpty() || employeeIds.contains(inv.getEmployee().getIndividualId()))
                .toList();
            int processed = invitations.size();
            int sent = 0;
            int failed = 0;
            for (EnrollmentInvitation inv : invitations) {
                if (inv.getExpiresAt() != null && inv.getExpiresAt().toLocalDate().isBefore(LocalDate.now())) {
                    continue;
                }
                // Deterministic token: resend same link. Legacy: generate new token and update hash.
                String rawToken = Boolean.TRUE.equals(inv.getTokenDeterministic())
                    ? tokenSecurityService.generateTokenForInvitation(inv.getId())
                    : tokenSecurityService.generateToken();
                if (!Boolean.TRUE.equals(inv.getTokenDeterministic())) {
                    inv.setTokenHash(tokenSecurityService.hashToken(rawToken));
                    invitationRepository.save(inv);
                }
                String magicLink = baseUrl + "/enrollment/" + rawToken;
                boolean emailSent = sendEnrollmentReminderEmail(inv.getEmployee().getEmail(), inv.getEmployee().getFullName(), magicLink);
                if (emailSent) {
                    inv.setStatus(inv.getStatus() == EnrollementStatus.REJECTED ? EnrollementStatus.SENT : inv.getStatus());
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
    public void runScheduledReminders() {
        List<EnrollementStatus> reminderStatuses = List.of(EnrollementStatus.SENT, EnrollementStatus.OPENED, EnrollementStatus.IN_PROGRESS);
        List<EnrollmentInvitation> eligible = invitationRepository.findEligibleForReminder(reminderStatuses, LocalDateTime.now());
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        int sent = 0;
        int skipped = 0;
        int failed = 0;
        for (EnrollmentInvitation inv : eligible) {
            try {
                EnrollmentWindows window = inv.getEnrollmentWindow();
                String configJson = window != null ? window.getConfig() : null;
                boolean reminderEnabled = true;
                int reminderFrequencyDays = 3;
                if (configJson != null && !configJson.isBlank()) {
                    try {
                        JsonNode config = OBJECT_MAPPER.readTree(configJson);
                        if (config.has("reminderEnabled") && config.get("reminderEnabled").isBoolean()) {
                            reminderEnabled = config.get("reminderEnabled").asBoolean();
                        }
                        if (config.has("reminderFrequencyDays") && config.get("reminderFrequencyDays").isNumber()) {
                            reminderFrequencyDays = config.get("reminderFrequencyDays").asInt();
                        }
                    } catch (Exception e) {
                        logger.debug("[correlationId:{}] Failed to parse window config, using defaults: {}", MDC.get("correlationId"), e.getMessage());
                    }
                }
                if (!reminderEnabled) {
                    skipped++;
                    continue;
                }
                LocalDateTime lastReminderAt = inv.getLastReminderAt();
                LocalDate expiresDate = inv.getExpiresAt() != null ? inv.getExpiresAt().toLocalDate() : null;
                boolean dueByFrequency = lastReminderAt == null
                        || !lastReminderAt.toLocalDate().plusDays(reminderFrequencyDays).isAfter(today);
                boolean finalReminderDue = expiresDate != null && expiresDate.equals(tomorrow);
                if (!dueByFrequency && !finalReminderDue) {
                    skipped++;
                    continue;
                }
                String rawToken = Boolean.TRUE.equals(inv.getTokenDeterministic())
                        ? tokenSecurityService.generateTokenForInvitation(inv.getId())
                        : tokenSecurityService.generateToken();
                if (!Boolean.TRUE.equals(inv.getTokenDeterministic())) {
                    inv.setTokenHash(tokenSecurityService.hashToken(rawToken));
                }
                String magicLink = baseUrl + "/enrollment/" + rawToken;
                boolean emailSent = sendEnrollmentReminderEmail(
                        inv.getEmployee().getEmail(),
                        inv.getEmployee().getFullName(),
                        magicLink);
                if (emailSent) {
                    inv.setReminderCount(inv.getReminderCount() == null ? 1 : inv.getReminderCount() + 1);
                    inv.setLastReminderAt(LocalDateTime.now());
                    invitationRepository.save(inv);
                    sent++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                logger.warn("[correlationId:{}] Scheduled reminder failed for invitation {}: {}", MDC.get("correlationId"), inv.getId(), e.getMessage());
                failed++;
            }
        }
        logger.info("[correlationId:{}] Scheduled reminders: eligible={}, sent={}, skipped={}, failed={}", MDC.get("correlationId"), eligible.size(), sent, skipped, failed);
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
    @Transactional
    public ResponseEntity<ResponseDto<ExtendDeadlineResultDto>> extendDeadlineForEmployees(List<UUID> employeeIds, UUID windowId, LocalDateTime newExpiresAt) {
        BaseResponse<ExtendDeadlineResultDto> responseObj = new BaseResponse<>();
        try {
            if (employeeIds == null || employeeIds.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("At least one employee ID is required"));
            }
            List<UUID> failed = new ArrayList<>();
            int extended = 0;
            for (UUID employeeId : employeeIds) {
                try {
                    EnrollmentInvitation inv = invitationRepository.findByEmployee_IndividualIdAndEnrollmentWindow_Id(employeeId, windowId).orElse(null);
                    if (inv == null) {
                        failed.add(employeeId);
                        continue;
                    }
                    if (inv.getExpiresAt() != null && inv.getExpiresAt().isAfter(newExpiresAt)) {
                        failed.add(employeeId);
                        continue;
                    }
                    inv.setExpiresAt(newExpiresAt);
                    invitationRepository.save(inv);
                    extended++;
                } catch (Exception e) {
                    logger.warn("[correlationId:{}] Extend deadline failed for employee {}: {}", MDC.get("correlationId"), employeeId, e.getMessage());
                    failed.add(employeeId);
                }
            }
            ExtendDeadlineResultDto dto = ExtendDeadlineResultDto.builder()
                .extended(extended)
                .failed(failed.size())
                .build();
            return responseObj.render(responseObj.formSuccessResponse("Deadline extended for " + extended + " employee(s)", dto));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] extendDeadlineForEmployees failed", MDC.get("correlationId"), e);
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

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<InvitationLinkResponseDto>> getInvitationLink(UUID invitationId) {
        BaseResponse<InvitationLinkResponseDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentInvitation inv = invitationRepository.findById(invitationId).orElse(null);
            if (inv == null) {
                return responseObj.render(responseObj.formErrorResponse("Invitation not found"));
            }
            if (!Boolean.TRUE.equals(inv.getTokenDeterministic())) {
                return responseObj.render(responseObj.formErrorResponse(
                    "Link not available for this invitation; use Resend to generate a new link."));
            }
            String rawToken = tokenSecurityService.generateTokenForInvitation(inv.getId());
            // Ensure stored hash matches the deterministic token (fixes inconsistency from creation vs copy-link)
            if (!tokenSecurityService.verifyToken(rawToken, inv.getTokenHash())) {
                logger.warn("[correlationId:{}] Invitation {} token hash mismatch; repairing to deterministic token hash",
                    MDC.get("correlationId"), invitationId);
                inv.setTokenHash(tokenSecurityService.hashToken(rawToken));
                invitationRepository.save(inv);
            }
            String magicLink = baseUrl + "/enrollment/" + rawToken;
            InvitationLinkResponseDto dto = InvitationLinkResponseDto.builder().magicLink(magicLink).build();
            return responseObj.render(responseObj.formSuccessResponse("OK", dto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getInvitationLink failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<ResendInvitationResponseDto>> resendActivationLink(UUID invitationId) {
        BaseResponse<ResendInvitationResponseDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentInvitation inv = invitationRepository.findById(invitationId).orElse(null);
            if (inv == null) {
                return responseObj.render(responseObj.formErrorResponse("Invitation not found"));
            }
            Deals employee = inv.getEmployee();
            if (employee.getEmail() == null || employee.getEmail().isBlank()) {
                return responseObj.render(responseObj.formErrorResponse("Employee email is required"));
            }
            // Deterministic: resend same link. Legacy: generate new token and update hash.
            String rawToken = Boolean.TRUE.equals(inv.getTokenDeterministic())
                ? tokenSecurityService.generateTokenForInvitation(inv.getId())
                : tokenSecurityService.generateToken();
            if (!Boolean.TRUE.equals(inv.getTokenDeterministic())) {
                inv.setTokenHash(tokenSecurityService.hashToken(rawToken));
                invitationRepository.save(inv);
            }
            String magicLink = baseUrl + "/enrollment/" + rawToken;
            boolean emailSent = sendEnrollmentInvitationEmail(employee.getEmail(), employee.getFullName(), magicLink);
            if (emailSent) {
                inv.setStatus(EnrollementStatus.SENT);
                inv.setSentAt(LocalDateTime.now());
                invitationRepository.save(inv);
            } else {
                logger.warn("[correlationId:{}] Resend email failed for invitation {}", MDC.get("correlationId"), invitationId);
                return responseObj.render(responseObj.formErrorResponse("Email sending failed"));
            }
            ResendInvitationResponseDto dto = ResendInvitationResponseDto.builder()
                .sent(true)
                .email(employee.getEmail())
                .magicLink(magicLink)
                .build();
            return responseObj.render(responseObj.formSuccessResponse("Activation link resent", dto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] resendActivationLink failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<ActivateWindowResponseDto>> activateWindowAndSendInvites(UUID windowId) {
        BaseResponse<ActivateWindowResponseDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentWindows window = enrollmentWindowsRepository.findById(windowId).orElse(null);
            if (window == null) {
                return responseObj.render(responseObj.formErrorResponse("Enrollment window not found"));
            }
            if (window.getStatus() != EnrollementStatus.SCHEDULED) {
                return responseObj.render(responseObj.formErrorResponse("Window is not in SCHEDULED status"));
            }
            window.setStatus(EnrollementStatus.ACTIVE);
            enrollmentWindowsRepository.save(window);

            List<Deals> employees = dealsRepository.findByEnrollmentWindow_Id(windowId);
            if (employees.isEmpty()) {
                ActivateWindowResponseDto dto = ActivateWindowResponseDto.builder()
                    .windowStatus(EnrollementStatus.ACTIVE.name())
                    .sent(0)
                    .failed(0)
                    .failedDetails(List.of())
                    .build();
                return responseObj.render(responseObj.formSuccessResponse("Window activated", dto));
            }
            List<UUID> employeeIds = employees.stream().map(Deals::getIndividualId).toList();
            ResponseEntity<ResponseDto<IEnrollmentInvitation.BulkInvitationResult>> bulkResp = sendBulkInvitations(employeeIds, windowId);
            ResponseDto<IEnrollmentInvitation.BulkInvitationResult> body = bulkResp != null ? bulkResp.getBody() : null;
            if (body == null || body.getErrorCode() != null || body.getPayload() == null) {
                return responseObj.render(responseObj.formErrorResponse(body != null && body.getMessage() != null ? body.getMessage() : "Bulk send failed"));
            }
            IEnrollmentInvitation.BulkInvitationResult result = body.getPayload();
            ActivateWindowResponseDto dto = ActivateWindowResponseDto.builder()
                .windowStatus(EnrollementStatus.ACTIVE.name())
                .sent(result.sent())
                .failed(result.failed())
                .failedDetails(result.failedDetails())
                .build();
            return responseObj.render(responseObj.formSuccessResponse("Window activated", dto));
        } catch (Exception e) {
            TransactionUtil.markRollbackOnly();
            logger.error("[correlationId:{}] activateWindowAndSendInvites failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<EnrollmentProgressResponseDto>> getEnrollmentProgress(UUID windowId) {
        BaseResponse<EnrollmentProgressResponseDto> responseObj = new BaseResponse<>();
        try {
            EnrollmentWindows window = enrollmentWindowsRepository.findById(windowId).orElse(null);
            if (window == null) {
                return responseObj.render(responseObj.formErrorResponse("Enrollment window not found"));
            }

            // Query ALL employees linked to this window (from customers table)
            List<Deals> allEmployees = dealsRepository.findByEnrollmentWindow_Id(windowId);

            // Query invitations and submissions
            List<EnrollmentInvitation> invitations = invitationRepository.findAllByEnrollmentWindow_Id(windowId);
            List<EnrollmentSubmission> submissions = submissionRepository.findAllByEnrollmentWindow_Id(windowId);

            // Index invitations and submissions by employee ID for quick lookup
            java.util.Map<UUID, EnrollmentInvitation> invitationByEmployee = new java.util.HashMap<>();
            for (EnrollmentInvitation inv : invitations) {
                invitationByEmployee.put(inv.getEmployee().getIndividualId(), inv);
            }
            java.util.Map<UUID, EnrollmentSubmission> submissionByEmployee = new java.util.HashMap<>();
            for (EnrollmentSubmission s : submissions) {
                submissionByEmployee.put(s.getEmployee().getIndividualId(), s);
            }

            // Use the larger of employees vs invitations as the total
            int totalEmployees = Math.max(allEmployees.size(), invitations.size());

            // Count as invited if they have an invitation (including PENDING: invitation exists, link was created even if status wasn't updated to SENT)
            long invitedCount = invitations.size();
            long openedCount = invitations.stream().filter(inv -> inv.getStatus() == EnrollementStatus.OPENED || inv.getStatus() == EnrollementStatus.IN_PROGRESS || inv.getStatus() == EnrollementStatus.COMPLETED).count();
            // Include COMPLETED/ENDORSED so finalized windows show correct counts (finalize sets submission to COMPLETED)
            long submittedCount = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.SUBMITTED || s.getStatus() == EnrollementStatus.APPROVED || s.getStatus() == EnrollementStatus.COMPLETED || s.getStatus() == EnrollementStatus.ENDORSED).count();
            long approvedCount = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.APPROVED || s.getStatus() == EnrollementStatus.ENDORSED || s.getStatus() == EnrollementStatus.COMPLETED).count();
            long reviewedCount = submissions.stream().filter(s -> s.getStatus() == EnrollementStatus.APPROVED || s.getStatus() == EnrollementStatus.REJECTED || s.getStatus() == EnrollementStatus.ENDORSED || s.getStatus() == EnrollementStatus.COMPLETED).count();

            double completionRate = totalEmployees > 0 ? (submittedCount * 100.0 / totalEmployees) : 0.0;

            // Build employee details from ALL employees (customers table),
            // enriched with invitation and submission data where available
            List<EmployeeProgressDetailDto> employeeDetails = new ArrayList<>();
            java.util.Set<UUID> processedEmployeeIds = new java.util.HashSet<>();

            for (Deals emp : allEmployees) {
                UUID empId = emp.getIndividualId();
                processedEmployeeIds.add(empId);

                EnrollmentInvitation inv = invitationByEmployee.get(empId);
                EnrollmentSubmission sub = submissionByEmployee.get(empId);

                String enrollmentStatus;
                if (sub != null) {
                    enrollmentStatus = sub.getStatus().name();
                } else if (inv != null) {
                    // PENDING = invite created but not sent → show as pending_invite so UI shows "Send Invitations"
                    EnrollementStatus invStatus = inv.getStatus();
                    enrollmentStatus = (invStatus != null && invStatus == EnrollementStatus.PENDING)
                        ? "pending"
                        : (invStatus != null ? invStatus.name().toLowerCase() : "sent");
                } else {
                    enrollmentStatus = "not_invited";
                }

                EmployeeProgressDetailDto detail = EmployeeProgressDetailDto.builder()
                    .employeeId(empId)
                    .invitationId(inv != null ? inv.getId() : null)
                    .submissionId(sub != null ? sub.getId() : null)
                    .name(emp.getFullName())
                    .email(emp.getEmail())
                    .employeeNumber(emp.getEmployeeNumber())
                    .enrollmentStatus(enrollmentStatus)
                    .submittedAt(sub != null ? sub.getSubmittedAt() : null)
                    .approvedAt(sub != null && (sub.getStatus() == EnrollementStatus.APPROVED || sub.getStatus() == EnrollementStatus.ENDORSED) ? sub.getReviewedAt() : null)
                    .build();
                employeeDetails.add(detail);
            }

            // Also include any invitation-only records (edge case: invitation exists but employee not linked to window)
            for (EnrollmentInvitation inv : invitations) {
                UUID empId = inv.getEmployee().getIndividualId();
                if (!processedEmployeeIds.contains(empId)) {
                    Deals emp = inv.getEmployee();
                    EnrollmentSubmission sub = submissionByEmployee.get(empId);
                    EnrollementStatus invStatus = inv.getStatus();
                    String enrollmentStatus = sub != null ? sub.getStatus().name()
                        : (invStatus != null && invStatus == EnrollementStatus.PENDING ? "pending" : (invStatus != null ? invStatus.name().toLowerCase() : "sent"));
                    EmployeeProgressDetailDto detail = EmployeeProgressDetailDto.builder()
                        .employeeId(empId)
                        .invitationId(inv.getId())
                        .submissionId(sub != null ? sub.getId() : null)
                        .name(emp.getFullName())
                        .email(emp.getEmail())
                        .employeeNumber(emp.getEmployeeNumber())
                        .enrollmentStatus(enrollmentStatus)
                        .submittedAt(sub != null ? sub.getSubmittedAt() : null)
                        .approvedAt(sub != null && (sub.getStatus() == EnrollementStatus.APPROVED || sub.getStatus() == EnrollementStatus.ENDORSED) ? sub.getReviewedAt() : null)
                        .build();
                    employeeDetails.add(detail);
                }
            }

            EnrollmentProgressResponseDto dto = EnrollmentProgressResponseDto.builder()
                .windowId(windowId)
                .windowStatus(window.getStatus().name())
                .totalEmployees(totalEmployees)
                .invitedCount((int) invitedCount)
                .openedCount((int) openedCount)
                .submittedCount((int) submittedCount)
                .approvedCount((int) approvedCount)
                .reviewedCount((int) reviewedCount)
                .completionRate(Math.round(completionRate * 100.0) / 100.0)
                .employeeDetails(employeeDetails)
                .build();
            return responseObj.render(responseObj.formSuccessResponse("OK", dto));
        } catch (Exception e) {
            logger.error("[correlationId:{}] getEnrollmentProgress failed", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    private boolean sendEnrollmentInvitationEmail(String to, String employeeName, String magicLink) {
        try {
            EmailRequest req = EmailRequest.builder()
                .to("rajaram.ganesan@kumaran.com")
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
                .to("rajaram.ganesan@kumaran.com")
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
