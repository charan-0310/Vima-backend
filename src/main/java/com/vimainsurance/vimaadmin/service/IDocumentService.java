package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Document management
 * 
 * Focused on KYC, Policy, and Claims document operations:
 * - KYC document upload and verification
 * - Policy document management
 * - Claims document processing
 */
public interface IDocumentService {

    // KYC Document Operations
    /**
     * Upload KYC documents for a customer/lead
     * 
     * @param files Array of KYC files to upload
     * @param entityId Customer/Lead ID
     * @param documentType Type of document (decided by frontend)
     * @param uploadedBy UUID of the user uploading
     * @param uploadedByRole Role of the user uploading
     * @return Response with list of uploaded KYC documents
     */
    ResponseEntity<ResponseDto<List<Document>>> uploadKYCDocuments(
            MultipartFile[] files,
            String entityId,
            DocumentEntityType entityType,
            DocumentType documentType,
            UUID uploadedBy,
            com.vimainsurance.vimaadmin.enums.UserRole uploadedByRole,
            String notes
    );

    /**
     * Get KYC documents for a customer/lead
     * 
     * @param entityId Customer/Lead ID
     * @param pageable Pagination parameters
     * @return Response with paginated KYC documents
     */
    ResponseEntity<ResponseDto<Page<Document>>> getKYCDocuments(
            String entityId, 
            Pageable pageable
    );

    // Policy Document Operations
    /**
     * Upload policy documents
     * 
     * @param files Array of policy files to upload
     * @param policyId Policy ID
     * @param documentType Type of document (decided by frontend)
     * @param uploadedBy UUID of the user uploading
     * @param uploadedByRole Role of the user uploading
     * @return Response with list of uploaded policy documents
     */
    ResponseEntity<ResponseDto<List<Document>>> uploadPolicyDocuments(
            MultipartFile[] files,
            String policyId,
            DocumentType documentType,
            UUID uploadedBy,
            com.vimainsurance.vimaadmin.enums.UserRole uploadedByRole
    );

    /**
     * Get policy documents
     * 
     * @param policyId Policy ID
     * @param pageable Pagination parameters
     * @return Response with paginated policy documents
     */
    ResponseEntity<ResponseDto<Page<Document>>> getPolicyDocuments(
            String policyId, 
            Pageable pageable
    );

    // Claims Document Operations
    /**
     * Upload claims documents
     * 
     * @param files Array of claims files to upload
     * @param claimId Claim ID
     * @param documentType Type of document (decided by frontend)
     * @param uploadedBy UUID of the user uploading
     * @param uploadedByRole Role of the user uploading
     * @return Response with list of uploaded claims documents
     */
    ResponseEntity<ResponseDto<List<Document>>> uploadClaimsDocuments(
            MultipartFile[] files,
            String claimId,
            DocumentType documentType,
            UUID uploadedBy,
            com.vimainsurance.vimaadmin.enums.UserRole uploadedByRole
    );

    /**
     * Get claims documents
     * 
     * @param claimId Claim ID
     * @param pageable Pagination parameters
     * @return Response with paginated claims documents
     */
    ResponseEntity<ResponseDto<Page<Document>>> getClaimsDocuments(
            String claimId, 
            Pageable pageable
    );

    // Common Document Operations
    /**
     * Get document by ID
     * 
     * @param documentId The document ID
     * @return Response with document metadata
     */
    ResponseEntity<ResponseDto<Document>> getDocumentById(UUID documentId);

    /**
     * Get document download URL
     * 
     * @param documentId Document ID
     * @param requestedBy UUID of the user requesting
     * @return Response with pre-signed download URL
     */
    ResponseEntity<ResponseDto<String>> getDocumentDownloadUrl(UUID documentId, UUID requestedBy);

    /**
     * Delete document (removes from S3 and database)
     * 
     * @param documentId Document ID to delete
     * @param deletedBy UUID of the user deleting
     * @return Response with deletion status
     */
    ResponseEntity<ResponseDto<String>> deleteDocument(UUID documentId, UUID deletedBy);

    /**
     * Update document notes
     * 
     * @param documentId Document ID to update
     * @param notes New notes
     * @return Response with updated document
     */
    ResponseEntity<ResponseDto<Document>> updateDocumentNotes(UUID documentId, String notes);

    // Document Count Operations
    /**
     * Get KYC document count for customer/lead
     * 
     * @param entityId Customer/Lead ID
     * @return Response with KYC document count
     */
    ResponseEntity<ResponseDto<Long>> getKYCDocumentCount(String entityId);

    /**
     * Get policy document count
     * 
     * @param policyId Policy ID
     * @return Response with policy document count
     */
    ResponseEntity<ResponseDto<Long>> getPolicyDocumentCount(String policyId);

    /**
     * Get claims document count
     * 
     * @param claimId Claim ID
     * @return Response with claims document count
     */
    ResponseEntity<ResponseDto<Long>> getClaimsDocumentCount(String claimId);

    // Validation Operations
    /**
     * Validate file before upload
     * 
     * @param file The file to validate
     * @param documentType Expected document type
     * @return Response with validation result
     */
    ResponseEntity<ResponseDto<String>> validateFile(MultipartFile file, DocumentType documentType);
}
