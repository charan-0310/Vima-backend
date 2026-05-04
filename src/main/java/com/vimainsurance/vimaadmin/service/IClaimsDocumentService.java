package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDocumentListResponse;
import com.vimainsurance.vimaadmin.dto.claim.DocumentUploadResponse;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.UserRole;

/**
 * Claims document upload, list, streamed download, and delete.
 * Enforces BR-DOC-001 (max 10 docs per claim), file type/size, and role-based document types.
 */
public interface IClaimsDocumentService {

    /**
     * Upload documents to a claim. Validates file type (PDF, JPG, JPEG, PNG, WebP), max 5MB per file,
     * max 10 documents per claim (BR-DOC-001), and document type allowed for the given role.
     * Employee: own claims only and claim not in terminal status.
     * Admin: any claim (BR-DOC-005).
     *
     * @param files       Files to upload
     * @param claimId     Claim UUID
     * @param documentType Document type (must be allowed for role)
     * @param uploadedBy  Uploader UUID (employee or admin user)
     * @param role        "EMPLOYEE" or admin role (VIMA_ADMIN, HR_ADMIN, etc.)
     * @return Upload response with uploaded items and total document count
     */
    ResponseEntity<ResponseDto<DocumentUploadResponse>> uploadClaimsDocuments(
            MultipartFile[] files,
            UUID claimId,
            DocumentType documentType,
            UUID uploadedBy,
            String role);

    /**
     * List claim documents (metadata only; use {@link #downloadClaimDocument} for file bytes).
     * Employee: own claims only. Admin: any claim.
     */
    ResponseEntity<ResponseDto<ClaimDocumentListResponse>> getClaimDocuments(UUID claimId, UUID requestedBy, boolean isAdmin);

    /**
     * Stream document bytes from S3 (same authorization as {@link #getClaimDocuments}).
     */
    ResponseEntity<Resource> downloadClaimDocument(UUID claimId, UUID documentId, UUID requestedBy, boolean isAdmin);

    /**
     * Remove a document (admin only). Deletes from S3 and DB and writes audit log.
     */
    ResponseEntity<ResponseDto<String>> deleteDocument(UUID claimId, UUID docId, UUID actorId);
}
