package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.annotation.CurrentOrganization;
import com.vimainsurance.vimaadmin.dto.PolicyDocumentRefDto;
import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IPolicyService;

/**
 * Controller for Policy operations
 */
@RestController
@RequestMapping("/api/v1/policies")
@PreAuthorize("hasAnyRole('VIMA_ADMIN',  'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN', 'HR_ADMIN')")
public class PolicyController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyController.class);

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
    public ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByOrganizationId(@CurrentOrganization UUID organizationId) {
        return policyService.getPoliciesByOrganizationId(organizationId);
    }

    /**
     * Upload policy with details for an organization
     */
    @PostMapping(value = "/organization/{organizationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> uploadPolicyForOrganization(
            @CurrentOrganization UUID organizationId,
            @ModelAttribute PolicyUploadRequestDto requestDto) {
        logger.info("[correlationId:{}] /policies/organization/{} (POST) endpoint called", MDC.get("correlationId"), organizationId);
        return policyService.uploadPolicyForOrganization(organizationId, requestDto);
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
    @PreAuthorize("hasRole('VIMA_ADMIN')")
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

    /**
     * Upload (or replace) the policy wording PDF for the given policy.
     * The {@code @CurrentOrganization} parameter scopes the operation to the
     * caller's organization; the service refuses if the policy belongs to a
     * different org.
     */
    @PostMapping(value = "/{policyId}/wording-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDto<PolicyDocumentRefDto>> uploadPolicyWordingDocument(
            @CurrentOrganization UUID organizationId,
            @PathVariable Long policyId,
            @RequestParam("file") MultipartFile file) {
        logger.info("[correlationId:{}] /policies/{}/wording-document (POST) endpoint called", MDC.get("correlationId"), policyId);
        return policyService.attachPolicyWordingDocument(organizationId, policyId, file);
    }

    /**
     * Remove the policy wording PDF for the given policy (no-op if none attached).
     */
    @DeleteMapping("/{policyId}/wording-document")
    public ResponseEntity<ResponseDto<String>> deletePolicyWordingDocument(
            @CurrentOrganization UUID organizationId,
            @PathVariable Long policyId) {
        logger.info("[correlationId:{}] /policies/{}/wording-document (DELETE) endpoint called", MDC.get("correlationId"), policyId);
        return policyService.removePolicyWordingDocument(organizationId, policyId);
    }

    /**
     * Upload (or replace) the claim checklist PDF for the given policy.
     */
    @PostMapping(value = "/{policyId}/claim-checklist-document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDto<PolicyDocumentRefDto>> uploadClaimChecklistDocument(
            @CurrentOrganization UUID organizationId,
            @PathVariable Long policyId,
            @RequestParam("file") MultipartFile file) {
        logger.info("[correlationId:{}] /policies/{}/claim-checklist-document (POST) endpoint called", MDC.get("correlationId"), policyId);
        return policyService.attachClaimChecklistDocument(organizationId, policyId, file);
    }

    /**
     * Remove the claim checklist PDF for the given policy (no-op if none attached).
     */
    @DeleteMapping("/{policyId}/claim-checklist-document")
    public ResponseEntity<ResponseDto<String>> deleteClaimChecklistDocument(
            @CurrentOrganization UUID organizationId,
            @PathVariable Long policyId) {
        logger.info("[correlationId:{}] /policies/{}/claim-checklist-document (DELETE) endpoint called", MDC.get("correlationId"), policyId);
        return policyService.removeClaimChecklistDocument(organizationId, policyId);
    }
}
