package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.enums.EnrollementStatus;
import com.vimainsurance.vimaadmin.mapper.EnrollmentSubmissionMapper;
import com.vimainsurance.vimaadmin.mapper.EnrollmentWindowMapper;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.service.IEnrollmentService;
import com.vimainsurance.vimaadmin.service.TokenSecurityService;

@Service
public class EnrollmentServiceImpl implements IEnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentServiceImpl.class);
    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired enrollment link";
    private static final String ENROLLMENT_WINDOW_EXPIRED_MESSAGE = "Enrollment window expired";

    @Autowired
    private TokenSecurityService tokenSecurityService;
    @Autowired
    private IEnrollmentInvitationRepository enrollmentInvitationRepository;
    @Autowired
    private IEnrollmentWindowsRepository enrollmentWindowsRepository;
    @Autowired
    private IDealsRepository dealsRepository;
    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<EnrollmentContextDto>> validateTokenAndGetContext(String token) {
        logger.info("[correlationId:{}] Enrollment validateTokenAndGetContext called", MDC.get("correlationId"));
        BaseResponse<EnrollmentContextDto> responseObj = new BaseResponse<>();
        try {
            if (token == null || token.isBlank()) {
                return responseObj.render(responseObj.formErrorResponse(400, "Token is required"));
            }

            String tokenHash = tokenSecurityService.hashToken(token.trim());
            Optional<EnrollmentInvitation> invOpt = enrollmentInvitationRepository.findByTokenHashWithWindowAndEmployee(tokenHash);
            if (invOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, INVALID_TOKEN_MESSAGE));
            }

            EnrollmentInvitation invitation = invOpt.get();
            UUID enrollmentWindowId = invitation.getEnrollmentWindow().getId();
            UUID employeeId = invitation.getEmployee().getIndividualId();

            // 1. Fetch enrollment window from enrollment_windows table via IEnrollmentWindowsRepository
            Optional<EnrollmentWindows> windowOpt = enrollmentWindowsRepository.findById(enrollmentWindowId);
            if (windowOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Enrollment window not found"));
            }
            EnrollmentWindows enrollmentWindow = windowOpt.get();

            // 2. Validate enrollment window dates: current date must be between start_date and end_date
            LocalDate today = LocalDate.now();
            if (today.isBefore(enrollmentWindow.getStartDate()) || today.isAfter(enrollmentWindow.getEndDate())) {
                logger.warn("[correlationId:{}] Enrollment window expired: id={}, start={}, end={}, today={}",
                        MDC.get("correlationId"), enrollmentWindowId, enrollmentWindow.getStartDate(), enrollmentWindow.getEndDate(), today);
                return responseObj.render(responseObj.formErrorResponse(400, ENROLLMENT_WINDOW_EXPIRED_MESSAGE));
            }

            // Update enrollment window status to OPENED when date validation is successful
            enrollmentWindow.setStatus(EnrollementStatus.OPENED);
            enrollmentWindowsRepository.save(enrollmentWindow);

            // 3. Retrieve and return the matched enrollment_windows record (via IEnrollmentWindowsRepository)
            EnrollmentWindowResponseDto enrollmentWindowDto = EnrollmentWindowMapper.mapToResponseDto(enrollmentWindow);

            // 4. Fetch and return the corresponding employee/deals record from IDealsRepository
            Optional<Deals> dealsOpt = dealsRepository.findById(employeeId);
            if (dealsOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, "Employee not found"));
            }
            Deals employeeDeal = dealsOpt.get();
            DealsResponseDto employeeDto = mapDealToResponseDto(employeeDeal);

            EnrollmentContextDto dto = new EnrollmentContextDto(
                    enrollmentWindowId,
                    employeeId,
                    enrollmentWindowDto,
                    employeeDto);
            return responseObj.render(responseObj.formSuccessResponse("Token valid", dto));
        } catch (IllegalArgumentException e) {
            logger.warn("[correlationId:{}] Invalid token: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(400, INVALID_TOKEN_MESSAGE));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in validateTokenAndGetContext: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, INVALID_TOKEN_MESSAGE));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getSubmissionsByToken(String token) {
        logger.info("[correlationId:{}] Enrollment getSubmissionsByToken called", MDC.get("correlationId"));
        BaseResponse<List<EnrollmentSubmissionResponseDto>> responseObj = new BaseResponse<>();
        try {
            if (token == null || token.isBlank()) {
                return responseObj.render(responseObj.formErrorResponse(400, "Token is required"));
            }

            // Validate the token and extract the employee ID
            String tokenHash = tokenSecurityService.hashToken(token.trim());
            Optional<EnrollmentInvitation> invOpt = enrollmentInvitationRepository.findByTokenHashWithWindowAndEmployee(tokenHash);
            if (invOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(404, INVALID_TOKEN_MESSAGE));
            }

            EnrollmentInvitation invitation = invOpt.get();
            UUID employeeId = invitation.getEmployee().getIndividualId();

            // Fetch submissions for this employee
            List<EnrollmentSubmission> submissions = enrollmentSubmissionRepository.findAllByEmployee_IndividualId(employeeId);
            List<EnrollmentSubmissionResponseDto> out = new ArrayList<>();
            for (EnrollmentSubmission submission : submissions) {
                out.add(EnrollmentSubmissionMapper.mapToResponseDto(submission));
            }
            return responseObj.render(responseObj.formSuccessResponse("Success", out, out.size()));
        } catch (IllegalArgumentException e) {
            logger.warn("[correlationId:{}] Invalid token in getSubmissionsByToken: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse(400, INVALID_TOKEN_MESSAGE));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in getSubmissionsByToken: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(500, "Failed to retrieve submissions"));
        }
    }

    private DealsResponseDto mapDealToResponseDto(Deals deal) {
        DealsResponseDto dto = new DealsResponseDto();
        dto.setIndividualId(deal.getIndividualId());
        dto.setFirstName(deal.getFirstName());
        dto.setLastName(deal.getLastName());
        dto.setFullName(deal.getFullName());
        dto.setEmail(deal.getEmail());
        dto.setPhone(deal.getPhone());
        dto.setDateOfBirth(deal.getDateOfBirth());
        dto.setGender(deal.getGender());
        dto.setPanNumber(deal.getPanNumber());
        dto.setAadhaarNumber(deal.getAadhaarNumber());
        dto.setAddress(deal.getAddress());
        dto.setCity(deal.getCity());
        dto.setState(deal.getState());
        dto.setPincode(deal.getPincode());
        dto.setEmployeeNumber(deal.getEmployeeNumber());
        dto.setRelationship(deal.getRelationship());
        dto.setDesignation(deal.getDesignation());
        dto.setDateOfJoining(deal.getDateOfJoining());
        dto.setIsPrimaryMember(deal.getIsPrimaryMember());
        dto.setUsername(deal.getUsername());
        dto.setPreferredLanguage(deal.getPreferredLanguage());
        dto.setLeadId(deal.getLeadId());
        dto.setCustId(deal.getCustId());
        dto.setMaritalStatus(deal.getMaritalStatus());
        dto.setSumInsured(deal.getSumInsured());
        dto.setCreatedAt(deal.getCreatedAt());
        dto.setUpdatedAt(deal.getUpdatedAt());
        dto.setHealthId(deal.getHealthId());
        dto.setPolicies(null);
        if (deal.getAccountType() != null) {
            dto.setAccountType(deal.getAccountType().getValue());
        }
        if (deal.getStatus() != null) {
            dto.setAccountStatus(deal.getStatus().getValue());
        }
        return dto;
    }
}
