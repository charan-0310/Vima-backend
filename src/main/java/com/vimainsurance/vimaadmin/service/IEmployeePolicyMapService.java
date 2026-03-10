package com.vimainsurance.vimaadmin.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.BulkEmployeePolicyMapRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IEmployeePolicyMapService {

    ResponseEntity<ResponseDto<EmployeePolicyMapResponseDto>> createMapping(EmployeePolicyMapRequestDto dto);

    ResponseEntity<ResponseDto<String>> createBulkMappings(BulkEmployeePolicyMapRequestDto dto, String source);

    ResponseEntity<ResponseDto<String>> cancelMapping(UUID mappingId, String reason);

    ResponseEntity<ResponseDto<String>> cancelAllForEmployee(UUID employeeId, LocalDate effectiveDate, String reason);

    ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForIndividual(UUID individualId);

    ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForPolicy(Long policyId);

    ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForEmployeeFamily(UUID employeeId);

    ResponseEntity<ResponseDto<Page<EmployeePolicyMapResponseDto>>> getMappingsForOrganization(
            UUID organizationId, int page, int size, String status);

    boolean isIndividualCoveredByPolicy(UUID individualId, Long policyId);

    void createMappingsFromBulkUpload(UUID organizationId, List<UUID> employeeIds, String source);

    void createMappingsFromEnrollmentSubmission(UUID submissionId);

    /**
     * Create employee_policy_map rows for top-up opt-in from submission plan_selections (TOP_UP/SUPER_TOP_UP with topupPlanOptionId).
     */
    void createMappingsForTopupFromSubmission(UUID submissionId);

    /**
     * Create employee_policy_map rows for parent coverage (Scenario 2) for each parent/in-law dependent.
     */
    void createMappingsForParentFromSubmission(UUID submissionId);

    void createMappingsFromEndorsement(UUID endorsementId, String endorsementType);

    void cancelMappingsFromEndorsement(UUID endorsementId);
}
