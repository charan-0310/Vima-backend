package com.vimainsurance.vimaadmin.service;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IEnrollmentService {

    /**
     * Validates the enrollment token and returns enrollment_window_id and employee_id if valid.
     */
    ResponseEntity<ResponseDto<EnrollmentContextDto>> validateTokenAndGetContext(String token);
}
