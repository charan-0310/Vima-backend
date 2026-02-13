package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentSubmissionResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IEnrollmentSubmissionService {

    ResponseEntity<ResponseDto<String>> insertOrUpdate(EnrollmentSubmissionRequestDto requestDto);

    ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getList();

    ResponseEntity<ResponseDto<List<EnrollmentSubmissionResponseDto>>> getByEmployeeId(UUID employeeId);
}
