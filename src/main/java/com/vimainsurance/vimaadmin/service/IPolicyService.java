package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.PolicyDocumentRefDto;
import com.vimainsurance.vimaadmin.dto.PolicyRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.enums.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for Policy operations
 */
public interface IPolicyService {

    /**
     * Create a new policy
     */
    ResponseEntity<ResponseDto<String>> createPolicy(PolicyRequestDto requestDto);

    /**
     * Update an existing policy
     */
    ResponseEntity<ResponseDto<String>> updatePolicy(Long policyId, PolicyRequestDto requestDto);

    /**
     * Get policy by ID
     */
    ResponseEntity<ResponseDto<PolicyResponseDto>> getPolicyById(Long policyId);

    /**
     * Get policy by policy number
     */
    ResponseEntity<ResponseDto<PolicyResponseDto>> getPolicyByNumber(String policyNumber);

    /**
     * Get all policies with pagination
     */
    ResponseEntity<ResponseDto<Page<PolicyResponseDto>>> getAllPolicies(Pageable pageable);

    /**
     * Get policies by primary individual ID
     */
    ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByIndividualId(UUID individualId);

    /**
     * Get policies by insurance provider ID
     */
    ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByProviderId(UUID providerId);

    /**
     * Get policies by organization ID
     */
    ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByOrganizationId(UUID organizationId);

    /**
     * Get policies by status
     */
    ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByStatus(String status);

    /**
     * Get policies by lead ID
     */
    ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesByLeadId(UUID leadId);

    /**
     * Get active policies
     */
    ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getActivePolicies();

    /**
     * Get policies expiring within specified days
     */
    ResponseEntity<ResponseDto<List<PolicyResponseDto>>> getPoliciesExpiringBy(LocalDate expiryDate);

    /**
     * Update policy status
     */
    ResponseEntity<ResponseDto<String>> updatePolicyStatus(Long policyId, String status);

    /**
     * Delete policy (soft delete by changing status)
     */
    ResponseEntity<ResponseDto<String>> deletePolicy(Long policyId);

    /**
     * Check if policy number exists
     */
    ResponseEntity<ResponseDto<Boolean>> checkPolicyNumberExists(String policyNumber);

    /**
     * Get policy statistics
     */
    ResponseEntity<ResponseDto<Object>> getPolicyStatistics();
    
    /**
     * Upload policy with details for an organization
     */
    ResponseEntity<ResponseDto<String>> uploadPolicyForOrganization(UUID organizationId, PolicyUploadRequestDto requestDto);

    /**
     * Upload (or replace) the policy wording PDF for the given policy. The previously
     * attached wording document, if any, is removed from S3 and the documents table.
     * The operation is rejected if the policy does not belong to {@code organizationId}.
     */
    ResponseEntity<ResponseDto<PolicyDocumentRefDto>> attachPolicyWordingDocument(UUID organizationId, Long policyId, MultipartFile file);

    /**
     * Upload (or replace) the claim checklist PDF for the given policy. The previously
     * attached claim checklist, if any, is removed from S3 and the documents table.
     * The operation is rejected if the policy does not belong to {@code organizationId}.
     */
    ResponseEntity<ResponseDto<PolicyDocumentRefDto>> attachClaimChecklistDocument(UUID organizationId, Long policyId, MultipartFile file);

    /**
     * Detach and delete the currently uploaded policy wording PDF (no-op when absent).
     * The operation is rejected if the policy does not belong to {@code organizationId}.
     */
    ResponseEntity<ResponseDto<String>> removePolicyWordingDocument(UUID organizationId, Long policyId);

    /**
     * Detach and delete the currently uploaded claim checklist PDF (no-op when absent).
     * The operation is rejected if the policy does not belong to {@code organizationId}.
     */
    ResponseEntity<ResponseDto<String>> removeClaimChecklistDocument(UUID organizationId, Long policyId);
}
