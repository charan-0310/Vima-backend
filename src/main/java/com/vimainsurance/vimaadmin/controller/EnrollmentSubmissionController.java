package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IEnrollmentSubmissionService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/EnrollmentSubmission")
public class EnrollmentSubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentSubmissionController.class);

    @Autowired
    private IEnrollmentSubmissionService enrollmentSubmissionService;

    @PostMapping("/insertUpdate")
    public ResponseEntity<ResponseDto<String>> insertUpdate(@RequestBody EnrollmentSubmissionRequestDto requestDto) {
        logger.info("[correlationId:{}] /api/v1/EnrollmentSubmission/insertUpdate (POST) called", MDC.get("correlationId"));
        return enrollmentSubmissionService.insertOrUpdate(requestDto);
    }

    @GetMapping("/getList")
    public ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getList() {
        logger.info("[correlationId:{}] /api/v1/EnrollmentSubmission/getList (GET) called", MDC.get("correlationId"));
        return enrollmentSubmissionService.getList();
    }
}
