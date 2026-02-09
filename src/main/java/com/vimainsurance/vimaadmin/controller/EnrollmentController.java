package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.EnrollmentContextDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IEnrollmentService;

/**
 * Controller for public enrollment flows (token-based validation, no JWT required).
 */
@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/enrollment")
public class EnrollmentController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);

    @Autowired
    private IEnrollmentService enrollmentService;

    /**
     * Validate token and retrieve enrollment context (enrollment_window_id, employee_id).
     * Public endpoint – no authentication required.
     */
    @GetMapping("/{token}")
    public ResponseEntity<ResponseDto<EnrollmentContextDto>> getEnrollmentContext(@PathVariable String token) {
        logger.info("[correlationId:{}] GET /api/v1/enrollment/{} called", MDC.get("correlationId"), token);
        return enrollmentService.validateTokenAndGetContext(token);
    }

    /**
     * Get enrollment submissions for the employee associated with the given token.
     * Public endpoint – authorization is based on a valid enrollment token.
     */
    @GetMapping("/{token}/submissions")
    public ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getSubmissionsByToken(
            @PathVariable String token) {
        logger.info("[correlationId:{}] GET /api/v1/enrollment/{}/submissions called", MDC.get("correlationId"), token);
        return enrollmentService.getSubmissionsByToken(token);
    }
}
