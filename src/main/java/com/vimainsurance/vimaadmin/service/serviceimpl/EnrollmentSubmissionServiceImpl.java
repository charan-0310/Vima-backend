package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.Deals;
import com.vimainsurance.vimaadmin.entity.EnrollmentInvitation;
import com.vimainsurance.vimaadmin.entity.EnrollmentSubmission;
import com.vimainsurance.vimaadmin.entity.EnrollmentWindows;
import com.vimainsurance.vimaadmin.entity.Endorsement;
import com.vimainsurance.vimaadmin.mapper.EnrollmentSubmissionMapper;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentInvitationRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentSubmissionRepository;
import com.vimainsurance.vimaadmin.repository.IEnrollmentWindowsRepository;
import com.vimainsurance.vimaadmin.repository.IEndorsementRepository;
import com.vimainsurance.vimaadmin.service.IEnrollmentSubmissionService;
import com.vimainsurance.vimaadmin.util.Constants;

@Service
public class EnrollmentSubmissionServiceImpl implements IEnrollmentSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentSubmissionServiceImpl.class);

    @Autowired
    private IEnrollmentSubmissionRepository enrollmentSubmissionRepository;

    @Autowired
    private IDealsRepository dealsRepository;

    @Autowired
    private IEnrollmentWindowsRepository enrollmentWindowsRepository;

    @Autowired
    private IEnrollmentInvitationRepository enrollmentInvitationRepository;

    @Autowired
    private IEndorsementRepository endorsementRepository;

    @Autowired
    private IAdminUserRepository adminUserRepository;

    @Override
    @Transactional
    public ResponseEntity<ResponseDto<String>> insertOrUpdate(EnrollmentSubmissionRequestDto requestDto) {
        logger.info("[correlationId:{}] EnrollmentSubmission insertOrUpdate called", MDC.get("correlationId"));
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (requestDto == null) {
                return responseObj.render(responseObj.formErrorResponse("Request body is required"));
            }
            // If id is present -> update existing row; if id is absent -> insert new row
            if (requestDto.getId() != null) {
                return updateExisting(responseObj, requestDto);
            }
            return insertNew(responseObj, requestDto);
        } catch (IllegalArgumentException e) {
            logger.error("[correlationId:{}] Invalid value in EnrollmentSubmission insertOrUpdate: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse("Invalid value: " + e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            logger.warn("[correlationId:{}] Constraint violation in EnrollmentSubmission insertOrUpdate: {}", MDC.get("correlationId"), e.getMessage());
            String message = e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
            if (message != null && message.contains("unique_employee_submission")) {
                message = "A submission already exists for this employee and enrollment window.";
            } else if (message != null && message.contains("reference_number")) {
                message = "Reference number already exists. Use a different reference number.";
            } else if (message != null && message.contains("idempotency_key")) {
                message = "Idempotency key already exists.";
            }
            return responseObj.render(responseObj.formErrorResponse(message));
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String message = root.getMessage() != null ? root.getMessage() : e.getMessage();
            if (message == null) {
                message = e.getClass().getSimpleName();
            }
            logger.error("[correlationId:{}] EnrollmentSubmission insertOrUpdate failed: {}", MDC.get("correlationId"), message, e);
            return responseObj.render(responseObj.formErrorResponse(message));
        }
    }

    private ResponseEntity<ResponseDto<String>> updateExisting(BaseResponse<String> responseObj, EnrollmentSubmissionRequestDto requestDto) {
        Optional<EnrollmentSubmission> opt = enrollmentSubmissionRepository.findById(requestDto.getId());
        if (opt.isEmpty()) {
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
        EnrollmentSubmission entity = opt.get();

        Deals employee = null;
        if (requestDto.getEmployeeId() != null) {
            employee = dealsRepository.findById(requestDto.getEmployeeId()).orElse(null);
            if (employee == null) {
                return responseObj.render(responseObj.formErrorResponse("Employee not found"));
            }
        } else {
            employee = entity.getEmployee();
        }

        EnrollmentWindows enrollmentWindow = null;
        if (requestDto.getEnrollmentWindowId() != null) {
            enrollmentWindow = enrollmentWindowsRepository.findById(requestDto.getEnrollmentWindowId()).orElse(null);
            if (enrollmentWindow == null) {
                return responseObj.render(responseObj.formErrorResponse("Enrollment window not found"));
            }
        } else {
            enrollmentWindow = entity.getEnrollmentWindow();
        }

        EnrollmentInvitation invitation = null;
        if (requestDto.getInvitationId() != null) {
            invitation = enrollmentInvitationRepository.findById(requestDto.getInvitationId()).orElse(null);
        } else {
            invitation = entity.getInvitation();
        }

        Endorsement endorsement = null;
        if (requestDto.getEndorsementId() != null) {
            endorsement = endorsementRepository.findById(requestDto.getEndorsementId()).orElse(null);
        } else {
            endorsement = entity.getEndorsement();
        }

        AdminUser reviewedBy = null;
        if (requestDto.getReviewedById() != null) {
            reviewedBy = adminUserRepository.findById(requestDto.getReviewedById()).orElse(null);
        } else {
            reviewedBy = entity.getReviewedBy();
        }

        EnrollmentSubmissionMapper.updateEntityFromDto(entity, requestDto, employee, enrollmentWindow, invitation, endorsement, reviewedBy);
        enrollmentSubmissionRepository.save(entity);
        return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, Constants.UPDATE_SUCCESS));
    }

    private ResponseEntity<ResponseDto<String>> insertNew(BaseResponse<String> responseObj, EnrollmentSubmissionRequestDto requestDto) {
        if (requestDto.getEmployeeId() == null) {
            return responseObj.render(responseObj.formErrorResponse("Employee ID is required"));
        }
        if (requestDto.getEnrollmentWindowId() == null) {
            return responseObj.render(responseObj.formErrorResponse("Enrollment window ID is required"));
        }

        Deals employee = dealsRepository.findById(requestDto.getEmployeeId()).orElse(null);
        if (employee == null) {
            return responseObj.render(responseObj.formErrorResponse("Employee not found"));
        }

        EnrollmentWindows enrollmentWindow = enrollmentWindowsRepository.findById(requestDto.getEnrollmentWindowId()).orElse(null);
        if (enrollmentWindow == null) {
            return responseObj.render(responseObj.formErrorResponse("Enrollment window not found"));
        }

        if (enrollmentSubmissionRepository.existsByEmployee_IndividualIdAndEnrollmentWindow_Id(
                requestDto.getEmployeeId(), requestDto.getEnrollmentWindowId())) {
            return responseObj.render(responseObj.formErrorResponse(
                "A submission already exists for this employee and enrollment window."));
        }

        EnrollmentInvitation invitation = null;
        if (requestDto.getInvitationId() != null) {
            invitation = enrollmentInvitationRepository.findById(requestDto.getInvitationId()).orElse(null);
        }

        Endorsement endorsement = null;
        if (requestDto.getEndorsementId() != null) {
            endorsement = endorsementRepository.findById(requestDto.getEndorsementId()).orElse(null);
        }

        AdminUser reviewedBy = null;
        if (requestDto.getReviewedById() != null) {
            reviewedBy = adminUserRepository.findById(requestDto.getReviewedById()).orElse(null);
        }

        EnrollmentSubmission entity = EnrollmentSubmissionMapper.mapToEntity(requestDto, employee, enrollmentWindow, invitation, endorsement, reviewedBy);
        // Flush so the DB assigns the UUID before we read it (getId() is null until insert is executed)
        EnrollmentSubmission saved = enrollmentSubmissionRepository.saveAndFlush(entity);
        UUID id = saved.getId();
        if (id == null) {
            logger.warn("[correlationId:{}] EnrollmentSubmission insert returned entity with null id", MDC.get("correlationId"));
            return responseObj.render(responseObj.formErrorResponse("Failed to create submission: ID not assigned"));
        }
        return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, id.toString()));
    }

    @Override
    public ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getList() {
        logger.info("[correlationId:{}] EnrollmentSubmission getList called", MDC.get("correlationId"));
        BaseResponse<List<EnrollmentSubmissionResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<EnrollmentSubmission> list = enrollmentSubmissionRepository.findAll();
            List<EnrollmentSubmissionResponseDto> out = new ArrayList<>();
            for (EnrollmentSubmission submission : list) {
                out.add(EnrollmentSubmissionMapper.mapToResponseDto(submission));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentSubmission getList: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getByEmployeeId(UUID employeeId) {
        logger.info("[correlationId:{}] EnrollmentSubmission getByEmployeeId called for {}", MDC.get("correlationId"), employeeId);
        BaseResponse<List<EnrollmentSubmissionResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<EnrollmentSubmission> list = enrollmentSubmissionRepository.findAllByEmployee_IndividualId(employeeId);
            List<EnrollmentSubmissionResponseDto> out = new ArrayList<>();
            for (EnrollmentSubmission submission : list) {
                out.add(EnrollmentSubmissionMapper.mapToResponseDto(submission));
            }
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, out, out.size()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Exception in EnrollmentSubmission getByEmployeeId: {}", MDC.get("correlationId"), e.getMessage(), e);
            return responseObj.render(responseObj.formErrorResponse(Constants.RECORD_NOT_FOUND_MESSAGE));
        }
    }
}
