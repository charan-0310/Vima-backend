package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IEnrollmentService {

    /**
     * Validates the enrollment token and returns enrollment_window_id and employee_id if valid.
     */
    ResponseEntity<ResponseDto<EnrollmentContextDto>> validateTokenAndGetContext(String token);

    /**
     * Validates the enrollment token and returns all submissions for the associated employee.
     * Public endpoint – authorization is based on a valid enrollment token.
     */
    ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getSubmissionsByToken(String token);

    /**
     * Validates the enrollment token and returns company enrollment config (parent coverage etc.) for the enrollment's organization.
     */
    ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> getEnrollmentConfigByToken(String token);
}
