package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.annotation.RateLimit;
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

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service implementation for enrollment token validation and management.
 * 
 * Handles the complete token validation flow for public enrollment portal:
 * - Cryptographic token validation
 * - Expiry and status checks
 * - Invitation lifecycle management
 * - Draft submission creation
 * - Token-based authentication (no JWT)
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
    @RateLimit(limit = 3, periodMinutes = 1)
    public EnrollmentContext validateToken(String rawToken, HttpServletRequest request) {
        // Log only first 8 characters for security
        logger.info("[correlationId:{}] Validating enrollment token: {}...", 
            MDC.get("correlationId"), 
            rawToken != null && rawToken.length() >= 8 ? rawToken.substring(0, 8) : "invalid"
        );
        
        // Step 1: Hash the raw token
        String tokenHash = tokenSecurityService.hashToken(rawToken);
        
        // Step 2: Find invitation by token hash
        EnrollmentInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> {
                logger.warn("[correlationId:{}] Invalid token - not found in database", 
                    MDC.get("correlationId"));
                return new InvalidTokenException("Invalid or unknown enrollment token");
            });
        
        // Step 3: Check if token has expired
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("[correlationId:{}] Token expired for invitation: {}. Expired at: {}", 
                MDC.get("correlationId"), 
                invitation.getId(),
                invitation.getExpiresAt()
            );
            throw new TokenExpiredException("This enrollment link has expired. Please contact your HR department.");
        }
        
        // Step 4: Check if invitation is already completed
        if (invitation.getStatus() == EnrollementStatus.COMPLETED) {
            logger.warn("[correlationId:{}] Token already used for invitation: {}", 
                MDC.get("correlationId"), 
                invitation.getId()
            );
            throw new TokenAlreadyUsedException("Enrollment has already been completed for this invitation.");
        }
        
        // Step 5: Verify enrollment window is active
        EnrollmentWindows window = invitation.getEnrollmentWindow();
        if (window.getStatus() != EnrollementStatus.ACTIVE) {
            logger.warn("[correlationId:{}] Enrollment window not active. Window: {}, Status: {}", 
                MDC.get("correlationId"), 
                window.getId(),
                window.getStatus()
            );
            throw new EnrollmentWindowClosedException(
                "The enrollment window is not currently active. Status: " + window.getStatus()
            );
        }
        
        // Step 6: Mark invitation as opened (if first access)
        if (invitation.getStatus() == EnrollementStatus.SENT) {
            invitation.setStatus(EnrollementStatus.OPENED);
            invitation.setOpenedAt(LocalDateTime.now());
            invitationRepository.save(invitation);
            logger.info("[correlationId:{}] Invitation marked as OPENED: {}", 
                MDC.get("correlationId"), 
                invitation.getId()
            );
        }
        
        // Step 7: Get or create draft submission
        Deals employee = invitation.getEmployee();
        EnrollmentSubmission submission = submissionRepository
            .findByEmployee_IndividualIdAndEnrollmentWindow_Id(
                employee.getIndividualId(),
                window.getId()
            )
            .orElseGet(() -> {
                logger.info("[correlationId:{}] Creating draft submission for employee: {}", 
                    MDC.get("correlationId"), 
                    employee.getIndividualId()
                );
                return createDraftSubmission(invitation);
            });
        
        // Step 8: Build and return enrollment context
        EnrollmentContext context = EnrollmentContext.builder()
            .rawToken(rawToken)  // Return token for subsequent API calls
            .invitationId(invitation.getId())
            .expiresAt(invitation.getExpiresAt())
            .daysRemaining(ChronoUnit.DAYS.between(LocalDateTime.now(), invitation.getExpiresAt()))
            .employeeId(employee.getIndividualId())
            .employeeName(employee.getFullName())
            .employeeEmail(employee.getEmail())
            .dateOfBirth(employee.getDateOfBirth())
            .grade(employee.getDesignation()) // Using designation as grade
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
            submission.getId()
        );
        
        return context;
    }
    
    /**
     * Creates a new draft submission for an enrollment invitation.
     * 
     * @param invitation The enrollment invitation
     * @return Newly created draft submission
     */
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
}
