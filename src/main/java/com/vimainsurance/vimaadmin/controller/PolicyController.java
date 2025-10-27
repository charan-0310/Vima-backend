package com.vimainsurance.vimaadmin.controller;

import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller for Policy operations
 */
@RestController
@RequestMapping("/api/v1/policies")
@PreAuthorize("hasAnyAuthority( 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
public class PolicyController {

    @Autowired
    private IPolicyService policyService;

    /**
     * Create a new policy
     */
    @PostMapping
    public ResponseEntity<ResponseDto<String>> createPolicy(@RequestBody PolicyRequestDto requestDto) {
        return policyService.createPolicy(requestDto);
    }

    /**
     * Update an existing policy
     */
    @PutMapping("/{policyId}")
    public ResponseEntity<ResponseDto<String>> updatePolicy(@PathVariable Long policyId, 
                                                           @RequestBody PolicyRequestDto requestDto) {
        return policyService.updatePolicy(policyId, requestDto);
    }

    /**
     * Get policy by ID
     */
    @GetMapping("/{policyId}")
    public ResponseEntity<ResponseDto<PolicyResponseDto>> getPolicyById(@PathVariable Long policyId) {
        return policyService.getPolicyById(policyId);
    }

    /**
     * Get policy by policy number
     */
    @GetMapping("/number/{policyNumber}")
    public ResponseEntity<ResponseDto<PolicyResponseDto>> getPolicyByNumber(@PathVariable String policyNumber) {
        return policyService.getPolicyByNumber(policyNumber);
    }

    /**
     * Get all policies with pagination
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Page<PolicyResponseDto>>> getAllPolicies(Pageable pageable) {
        return policyService.getAllPolicies(pageable);
    }

    /**
     * Get policies by primary individual ID
     */
    @GetMapping("/individual/{individualId}")
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByIndividualId(@PathVariable UUID individualId) {
        return policyService.getPoliciesByIndividualId(individualId);
    }

    /**
     * Get policies by insurance provider ID
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByProviderId(@PathVariable UUID providerId) {
        return policyService.getPoliciesByProviderId(providerId);
    }

    /**
     * Get policies by organization ID
     */
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByOrganizationId(@PathVariable UUID organizationId) {
        return policyService.getPoliciesByOrganizationId(organizationId);
    }

    /**
     * Get policies by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByStatus(@PathVariable String status) {
        return policyService.getPoliciesByStatus(status);
    }

    /**
     * Get policies by lead ID
     */
    @GetMapping("/lead/{leadId}")
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByLeadId(@PathVariable UUID leadId) {
        return policyService.getPoliciesByLeadId(leadId);
    }

    /**
     * Get active policies
     */
    @GetMapping("/active")
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getActivePolicies() {
        return policyService.getActivePolicies();
    }

    /**
     * Get policies expiring within specified days
     */
    @GetMapping("/expiring")
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesExpiringBy(@RequestParam LocalDate expiryDate) {
        return policyService.getPoliciesExpiringBy(expiryDate);
    }

    /**
     * Update policy status
     */
    @PatchMapping("/{policyId}/status")
    public ResponseEntity<ResponseDto<String>> updatePolicyStatus(@PathVariable Long policyId, 
                                                                @RequestParam String status) {
        return policyService.updatePolicyStatus(policyId, status);
    }

    /**
     * Delete policy (soft delete)
     */
    @DeleteMapping("/{policyId}")
    public ResponseEntity<ResponseDto<String>> deletePolicy(@PathVariable Long policyId) {
        return policyService.deletePolicy(policyId);
    }

    /**
     * Check if policy number exists
     */
    @GetMapping("/exists/{policyNumber}")
    public ResponseEntity<ResponseDto<Boolean>> checkPolicyNumberExists(@PathVariable String policyNumber) {
        return policyService.checkPolicyNumberExists(policyNumber);
    }

    /**
     * Get policy statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ResponseDto<Object>> getPolicyStatistics() {
        return policyService.getPolicyStatistics();
    }
}
