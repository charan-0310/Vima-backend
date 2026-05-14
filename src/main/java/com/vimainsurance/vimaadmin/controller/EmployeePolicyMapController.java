package com.vimainsurance.vimaadmin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.EmployeePolicyMapResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IEmployeePolicyMapService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/employee-policy-map")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIMA_ADMIN', 'HR_ADMIN')")
public class EmployeePolicyMapController {

    @Autowired
    private IEmployeePolicyMapService employeePolicyMapService;

    @GetMapping("/employee/{employeeId}/family")
    public ResponseEntity<ResponseDto<List<EmployeePolicyMapResponseDto>>> getMappingsForFamily(
            @PathVariable UUID employeeId) {
        return employeePolicyMapService.getMappingsForEmployeeFamily(employeeId);
    }
}
