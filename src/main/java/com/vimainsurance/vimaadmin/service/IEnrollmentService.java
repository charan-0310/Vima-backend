package com.vimainsurance.vimaadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

import jakarta.servlet.http.HttpServletRequest;

public interface IEnrollmentService {

    /**
     * Validates the enrollment token and returns enrollment context if valid.
     */
    ResponseEntity<ResponseDto<EnrollmentContextDto>> validateTokenAndGetContext(
            String token, HttpServletRequest request);

    /**
     * Validates the enrollment token and returns all submissions for the associated employee.
     */
    ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getSubmissionsByToken(
            String token, HttpServletRequest request);

    /**
     * Validates the enrollment token and returns company enrollment config for the organization.
     */
    ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> getEnrollmentConfigByToken(
            String token, HttpServletRequest request);
}
