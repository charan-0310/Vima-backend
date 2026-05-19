package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.EnrollmentContext;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.exception.EnrollmentWindowClosedException;
import com.vimainsurance.vimaadmin.exception.InvalidTokenException;
import com.vimainsurance.vimaadmin.exception.TokenAlreadyUsedException;
import com.vimainsurance.vimaadmin.exception.TokenExpiredException;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.service.IEnrollmentTokenService;
import com.vimainsurance.vimaadmin.service.TokenSecurityService;
import com.vimainsurance.vimaadmin.util.IpAddressExtractor;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service implementation for enrollment token validation and management.
 */
@Service
public class EnrollmentTokenServiceImpl implements IEnrollmentTokenService {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentTokenServiceImpl.class);

    @Autowired
    private TokenSecurityService tokenSecurityService;

    @Autowired
    private IEnrollmentInvitationRepository invitationRepository;

    @Autowired
    private IEnrollmentSubmissionRepository submissionRepository;

    @Override
    @Transactional
    public EnrollmentContext validateToken(String rawToken, HttpServletRequest request) {
        if (rawToken == null || rawToken.isBlank()) {
            logValidationFailure(request, rawToken, "blank token");
            throw new InvalidTokenException("Enrollment token is required");
        }

        String trimmedToken = rawToken.trim();

        logger.info("[correlationId:{}] Validating enrollment token: {}...",
                MDC.get("correlationId"),
                trimmedToken.length() >= 8 ? trimmedToken.substring(0, 8) : "short");

        String tokenHash = tokenSecurityService.hashToken(trimmedToken);
        EnrollmentInvitation invitation = invitationRepository.findByTokenHashWithWindowAndEmployee(tokenHash)
                .orElseThrow(() -> {
                    logValidationFailure(request, trimmedToken, "token not found");
                    return new InvalidTokenException("Invalid or unknown enrollment token");
                });

        if (invitation.getStatus() == EnrollementStatus.EXPIRED) {
            logValidationFailure(request, trimmedToken, "invitation expired");
            throw new TokenExpiredException("Enrollment invitation expired");
        }

        if (invitation.getStatus() == EnrollementStatus.REJECTED) {
            logValidationFailure(request, trimmedToken, "invitation rejected");
            throw new InvalidTokenException("Enrollment invitation rejected");
        }

        if (invitation.getStatus() == EnrollementStatus.COMPLETED) {
            logValidationFailure(request, trimmedToken, "invitation completed");
            throw new TokenAlreadyUsedException("Enrollment has already been completed for this invitation.");
        }

        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            logValidationFailure(request, trimmedToken, "token expiresAt passed");
            throw new TokenExpiredException("This enrollment link has expired. Please contact your HR department.");
        }

        EnrollmentWindows window = invitation.getEnrollmentWindow();
        if (window == null) {
            logValidationFailure(request, trimmedToken, "enrollment window missing");
            throw new EnrollmentWindowClosedException("Enrollment window not found");
        }

        LocalDate today = LocalDate.now();
        if (window.getStartDate() != null && today.isBefore(window.getStartDate())
                || window.getEndDate() != null && today.isAfter(window.getEndDate())) {
            logValidationFailure(request, trimmedToken, "calendar window closed");
            throw new EnrollmentWindowClosedException(
                    String.format("Enrollment window expired. Window is open from %s to %s; today is %s.",
                            window.getStartDate(), window.getEndDate(), today));
        }

        if (invitation.getStatus() == EnrollementStatus.SENT || invitation.getStatus() == EnrollementStatus.PENDING) {
            invitation.setStatus(EnrollementStatus.OPENED);
            invitation.setOpenedAt(LocalDateTime.now());
            invitationRepository.save(invitation);
            logger.info("[correlationId:{}] Invitation marked as OPENED: {}",
                    MDC.get("correlationId"), invitation.getId());
        }

        Deals employee = invitation.getEmployee();
        EnrollmentSubmission submission = submissionRepository
                .findByEmployee_IndividualIdAndEnrollmentWindow_Id(
                        employee.getIndividualId(),
                        window.getId())
                .orElseGet(() -> {
                    logger.info("[correlationId:{}] Creating draft submission for employee: {}",
                            MDC.get("correlationId"), employee.getIndividualId());
                    return createDraftSubmission(invitation);
                });

        EnrollmentContext context = EnrollmentContext.builder()
                .rawToken(trimmedToken)
                .invitationId(invitation.getId())
                .expiresAt(invitation.getExpiresAt())
                .daysRemaining(invitation.getExpiresAt() != null
                        ? ChronoUnit.DAYS.between(LocalDateTime.now(), invitation.getExpiresAt())
                        : null)
                .employeeId(employee.getIndividualId())
                .employeeName(employee.getFullName())
                .employeeEmail(employee.getEmail())
                .dateOfBirth(employee.getDateOfBirth())
                .grade(employee.getDesignation())
                .windowId(window.getId())
                .windowName(window.getName())
                .windowStartDate(window.getStartDate())
                .windowEndDate(window.getEndDate())
                .windowStatus(window.getStatus())
                .windowConfig(window.getConfig())
                .submissionId(submission.getId())
                .submissionStatus(submission.getStatus())
                .planSelections(submission.getPlanSelections())
                .nomineeData(submission.getNomineeData())
                .build();

        logger.info("[correlationId:{}] Token validation successful. Employee: {}, Invitation: {}, Submission: {}",
                MDC.get("correlationId"),
                employee.getIndividualId(),
                invitation.getId(),
                submission.getId());

        return context;
    }

    private EnrollmentSubmission createDraftSubmission(EnrollmentInvitation invitation) {
        EnrollmentSubmission submission = new EnrollmentSubmission();
        submission.setEmployee(invitation.getEmployee());
        submission.setEnrollmentWindow(invitation.getEnrollmentWindow());
        submission.setInvitation(invitation);
        submission.setStatus(EnrollementStatus.DRAFT);
        submission.setPlanSelections("[]");
        submission.setNomineeData("{}");
        submission.setPremiumBreakdown("{}");
        submission.setDeclarationAccepted(false);
        return submissionRepository.save(submission);
    }

    private void logValidationFailure(HttpServletRequest request, String rawToken, String reason) {
        String ip = IpAddressExtractor.extractIpAddress(request);
        String prefix = rawToken != null && rawToken.length() >= 8
                ? rawToken.substring(0, 8)
                : (rawToken != null && !rawToken.isBlank() ? rawToken : "invalid");
        logger.warn("[correlationId:{}] Enrollment token validation failed: reason={}, clientIp={}, tokenPrefix={}",
                MDC.get("correlationId"), reason, ip, prefix);
    }
}
