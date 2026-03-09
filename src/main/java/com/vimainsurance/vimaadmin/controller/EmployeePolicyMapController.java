package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.BulkEmployeePolicyMapRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapRequestDto;
import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/employee-policy-map")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
public class EmployeePolicyMapController {

    @Autowired
    private IEmployeePolicyMapService employeePolicyMapService;

    @GetMapping("/individual/{individualId}")
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForIndividual(
            @PathVariable UUID individualId) {
        return employeePolicyMapService.getMappingsForIndividual(individualId);
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForPolicy(
            @PathVariable Long policyId) {
        return employeePolicyMapService.getMappingsForPolicy(policyId);
    }

    @GetMapping("/employee/{employeeId}/family")
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForFamily(
            @PathVariable UUID employeeId) {
        return employeePolicyMapService.getMappingsForEmployeeFamily(employeeId);
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ResponseDto<org.springframework.data.domain.Page<EmployeePolicyMapResponseDto>>> getMappingsForOrganization(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "ACTIVE") String status) {
        return employeePolicyMapService.getMappingsForOrganization(organizationId, page, size, status);
    }

    @PostMapping
    public ResponseEntity<ResponseDto<EmployeePolicyMapResponseDto>> createMapping(
            @RequestBody @Valid EmployeePolicyMapRequestDto dto) {
        return employeePolicyMapService.createMapping(dto);
    }

    @PostMapping("/bulk")
    public ResponseEntity<ResponseDto<String>> createBulkMappings(
            @RequestBody @Valid BulkEmployeePolicyMapRequestDto dto,
            @RequestParam(defaultValue = "MANUAL") String source) {
        return employeePolicyMapService.createBulkMappings(dto, source);
    }

    @DeleteMapping("/{mappingId}")
    public ResponseEntity<ResponseDto<String>> cancelMapping(
            @PathVariable UUID mappingId,
            @RequestParam String reason) {
        return employeePolicyMapService.cancelMapping(mappingId, reason);
    }
}
