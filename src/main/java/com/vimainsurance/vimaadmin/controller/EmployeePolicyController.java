package com.vimainsurance.vimaadmin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.EmployeePolicyWordingChecklistDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.service.IEmployeeInsuranceService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/employee/policies")
public class EmployeePolicyController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeePolicyController.class);

    @Autowired
    private IEmployeeInsuranceService employeeInsuranceService;

    @GetMapping("/{policyId}/wording-checklist")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ResponseDto<EmployeePolicyWordingChecklistDto>> getPolicyWordingChecklist(
            @PathVariable Long policyId) {
        String correlationId = MDC.get("correlationId");
        logger.info("[correlationId:{}] GET /api/v1/employee/policies/{}/wording-checklist", correlationId, policyId);
        try {
            EmployeePolicyWordingChecklistDto payload = employeeInsuranceService.getEmployeePolicyWordingChecklist(policyId);
            return ResponseEntity.ok(new ResponseDto<>("Policy wording/checklist retrieved successfully", payload));
        } catch (BadRequestException e) {
            logger.warn("[correlationId:{}] Failed to get policy wording/checklist for {}: {}", correlationId, policyId, e.getMessage());
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching policy wording/checklist for {}", correlationId, policyId, e);
            return ResponseEntity.internalServerError()
                    .body(new ResponseDto<>(500, "Failed to retrieve policy wording/checklist: " + e.getMessage()));
        }
    }
}
