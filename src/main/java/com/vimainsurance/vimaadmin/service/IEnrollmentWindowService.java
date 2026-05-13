package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.DependentEnrollmentUpdateDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.vimainsurance.vimaadmin.dto.EnrollmentWindowRequestDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowResponseDto;
import com.vimainsurance.vimaadmin.dto.EnrollmentWindowStatsDto;
import com.vimainsurance.vimaadmin.dto.SelfEmployeeEnrollmentRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IEnrollmentWindowService {

    ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> create(EnrollmentWindowRequestDto requestDto);

    /**
     * Validate employees for enrollment (no window or employees created).
     * Returns success with empty payload when valid, or error with payload = list of error messages.
     */
    ResponseEntity<ResponseDto<List<String>>> validateEmployees(
            UUID organizationId,
            List<SelfEmployeeEnrollmentRequestDto> selfEmployeeEnrollmentRequestDtos,
            Map<String, List<DependentEnrollmentUpdateDto>> dependentsByEmployeeId);

    /**
     * Validate uploaded enrollment file (SELF + dependents) before creating window.
     * Returns success with empty payload when valid, or error with payload = list of error messages.
     */
    ResponseEntity<ResponseDto<List<String>>> validateUploadFile(UUID organizationId, MultipartFile file);

    ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> uploadEmployees(UUID windowId, List<SelfEmployeeEnrollmentRequestDto> selfEmployeeEnrollmentRequestDtos, MultipartFile file);

    ResponseEntity<ResponseDto<List<EnrollmentWindowResponseDto>>> getAllWithFilters(
            UUID organizationId,
            String status,
            String name,
            String fromDate,
            String toDate,
            int page,
            int size,
            String sortBy,
            String sortDirection);

    ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> getById(UUID id);

    ResponseEntity<ResponseDto<EnrollmentWindowResponseDto>> update(UUID id, EnrollmentWindowRequestDto requestDto);

    ResponseEntity<?> activate(UUID id);

    ResponseEntity<ResponseDto<String>> close(UUID id);

    ResponseEntity<ResponseDto<String>> delete(UUID id);

    ResponseEntity<ResponseDto<EnrollmentWindowStatsDto>> getStats(UUID id);

    /**
     * Full employee + dependent rows for the window as CSV (DB-backed), streamed response.
     */
    ResponseEntity<StreamingResponseBody> exportEmployeesCsv(UUID windowId);
}
