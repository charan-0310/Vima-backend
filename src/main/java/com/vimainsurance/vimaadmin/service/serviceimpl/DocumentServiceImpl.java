package com.vimainsurance.vimaadmin.service.serviceimpl;

import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import com.vimainsurance.vimaadmin.util.Constants;
import com.vimainsurance.vimaadmin.util.IMaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.io.File;

/**
 * Service implementation for Document management
 * 
 * Focused on KYC, Policy, and Claims document operations:
 * - KYC document upload and verification
 * - Policy document management
 * - Claims document processing
 */
@Service
@Transactional
public class DocumentServiceImpl implements IDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceImpl.class);
    
    // Constants for logging
    private static final String CORRELATION_ID = "correlationId";
    private static final String FILE_VALIDATION_FAILED = "[correlationId:{}] File validation failed: {}";
    private static final String DOCUMENT_NOT_FOUND = "Document not found";
    private static final String ERROR_FETCHING_DOCUMENT_COUNT = "Error fetching document count: ";
    
    // File configuration from properties
    @Value("${document.upload.max-file-size}")
    private long maxFileSize;
    
    @Value("${document.upload.max-image-size}")
    private long maxImageSize;
    
    @Value("#{'${document.upload.allowed-extensions}'.split(',')}")
    private List<String> allowedExtensions;
    
    @Value("#{'${document.upload.allowed-mime-types}'.split(',')}")
    private List<String> allowedMimeTypes;

    private final IDocumentRepository documentRepository;
    private final com.vimainsurance.vimaadmin.service.IS3Service s3Service;
    private final com.vimainsurance.vimaadmin.config.S3Config s3Config;

    private final IMaskService maskService;

    @Autowired
    public DocumentServiceImpl(IDocumentRepository documentRepository, 
                              com.vimainsurance.vimaadmin.service.IS3Service s3Service,
                              com.vimainsurance.vimaadmin.config.S3Config s3Config,
                              IMaskService maskService) {
        this.documentRepository = documentRepository;
        this.s3Service = s3Service;
        this.s3Config = s3Config;
        this.maskService = maskService;
    }

    // KYC Document Operations
    @Override
    public ResponseEntity<ResponseDto<List<Document>>> uploadKYCDocuments(
            MultipartFile[] files,
            String entityId,
            DocumentEntityType entityType,
            DocumentType documentType,
            UUID uploadedBy,
            com.vimainsurance.vimaadmin.enums.UserRole uploadedByRole, String notes, DocumentCategory documentCategory) {
        
        logger.info("[correlationId:{}] uploadKYCDocuments called for entity: {} with {} files by user: {}", 
                   MDC.get("correlationId"), entityId, files.length, uploadedBy);
        
        BaseResponse<List<Document>> responseObj = new BaseResponse<>();
        List<Document> uploadedDocuments = new ArrayList<>();
        File maskedFile = null;
        try {
            for (MultipartFile file : files) {
                // Validate file
                ResponseEntity<ResponseDto<String>> validationResult = validateFile(file, null);
                if (validationResult.getBody() != null && validationResult.getBody().getErrorCode() != null) {
                    return responseObj.render(responseObj.formErrorResponse("Error uploading KYC documents: " ));
                }
                if(!documentType.equals(DocumentType.OTHER) && !documentType.equals(DocumentType.POLICY_CERTIFICATE) && documentRepository.findByEntityAndType(entityType, entityId, documentType).size() > 0){
                    return responseObj.render(responseObj.formErrorResponse(documentType.getValue()+" already uploaded"));
                }
                if(documentType.equals(DocumentType.OTHER) && documentRepository.findByEntityAndType(entityType, entityId, documentType).size() > 4){
                    return responseObj.render(responseObj.formErrorResponse("Only 4 other documents can be uploaded"));
                }
                if(documentType.equals(DocumentType.AADHAAR_CARD) && (file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/png") || file.getContentType().equals("image/jpg"))){
                    maskedFile = maskService.maskAADHARImage(file);
                } else if(documentType.equals(DocumentType.PAN_CARD) && (file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/png") || file.getContentType().equals("image/jpg"))){
                    maskedFile = maskService.maskPANImage(file);
                } else if(documentType.equals(DocumentType.PAN_CARD) && (file.getContentType().equals("application/pdf"))){
                    maskedFile = maskService.maskPANPdf(file);
                } else if(documentType.equals(DocumentType.AADHAAR_CARD) && (file.getContentType().equals("application/pdf"))){
                    maskedFile = maskService.maskAADHARPdf(file);
                }
                if(maskedFile == null && documentType.equals(DocumentType.PAN_CARD) && documentType.equals(DocumentType.AADHAAR_CARD)){
                    return responseObj.render(responseObj.formErrorResponse("Error Uploading document"));
                }
                // Generate S3 key
                if(documentType.equals(DocumentType.PAN_CARD) || documentType.equals(DocumentType.AADHAAR_CARD)){
                String unmaskedS3Key = generateS3Key(entityType, entityId, documentType, file.getOriginalFilename(), false);
                String maskedS3Key = generateS3Key(entityType, entityId, documentType, file.getOriginalFilename(), true);
                
                // Upload to S3
                String s3Url = s3Service.uploadFile(maskedFile, maskedS3Key, file.getContentType());
                String unmaskedS3Url = s3Service.uploadFile(file, unmaskedS3Key);
                
                // Create document entity
                Document document = createDocument(
                    entityType, entityId, documentType, documentCategory,
                    maskedS3Key, file, uploadedBy, uploadedByRole, notes
                );
                Document savedDocument = documentRepository.save(document);
                uploadedDocuments.add(savedDocument);
                }
                else {
                    String s3Key = generateS3Key(entityType, entityId, documentType, file.getOriginalFilename(), false);
                    String s3Url = s3Service.uploadFile(file, s3Key);
                    Document document = createDocument(
                        entityType, entityId, documentType, documentCategory,
                        s3Key, file, uploadedBy, uploadedByRole, notes
                    );
                    Document savedDocument = documentRepository.save(document);
                    uploadedDocuments.add(savedDocument);
                }
               
            }
            
            logger.info("[correlationId:{}] KYC documents uploaded successfully: {} out of {}", 
                       MDC.get("correlationId"), uploadedDocuments.size(), files.length);
            
            return responseObj.render(responseObj.formSuccessResponse(
                Constants.SUCCESS, uploadedDocuments, (long) uploadedDocuments.size()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error uploading KYC documents", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse("Error uploading KYC documents: " + e.getMessage()));
        }
    }
  
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Page<Document>>> getKYCDocuments(String entityId, Pageable pageable) {
        logger.info("[correlationId:{}] getKYCDocuments called for entity: {}", MDC.get("correlationId"), entityId);
        
        BaseResponse<Page<Document>> responseObj = new BaseResponse<>();
        
        try {
            List<Document> documents = documentRepository.findByEntityAndCategory(
                DocumentEntityType.LEAD, entityId, DocumentCategory.KYC_DOCUMENTS);
            // Convert to Page manually since repository doesn't support pagination for this method
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), documents.size());
            List<Document> pageContent = documents.subList(start, end);
            Page<Document> page = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, documents.size());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, page, page.getTotalElements()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching KYC documents for entity: {}", 
                        MDC.get("correlationId"), entityId, e);
            return responseObj.render(responseObj.formErrorResponse("Error fetching KYC documents: " + e.getMessage()));
        }
    }

    // Policy Document Operations
    @Override
    public ResponseEntity<ResponseDto<List<Document>>> uploadPolicyDocuments(
            MultipartFile[] files,
            String policyId,
            DocumentType documentType,
            UUID uploadedBy,
            com.vimainsurance.vimaadmin.enums.UserRole uploadedByRole) {
        
        logger.info("[correlationId:{}] uploadPolicyDocuments called for policy: {} with {} files by user: {}", 
                   MDC.get("correlationId"), policyId, files.length, uploadedBy);
        
        BaseResponse<List<Document>> responseObj = new BaseResponse<>();
        List<Document> uploadedDocuments = new ArrayList<>();
        
        try {
            for (MultipartFile file : files) {
                // Validate file
                ResponseEntity<ResponseDto<String>> validationResult = validateFile(file, null);
                if (validationResult.getBody() != null && validationResult.getBody().getErrorCode() != null) {
                    logger.warn(FILE_VALIDATION_FAILED, 
                               MDC.get(CORRELATION_ID), validationResult.getBody().getMessage());
                    continue;
                }
                
                // Generate S3 key
                String s3Key = generateS3Key(DocumentEntityType.POLICY, policyId, documentType, file.getOriginalFilename(), false);
                
                // Upload to S3
                String s3Url = s3Service.uploadFile(file, s3Key);
                
                // Create document entity
                Document document = createDocument(
                    DocumentEntityType.POLICY, policyId, documentType, DocumentCategory.POLICY_DOCUMENTS,
                    s3Key, file, uploadedBy, uploadedByRole, null
                );
                
                // Save to database
                Document savedDocument = documentRepository.save(document);
                uploadedDocuments.add(savedDocument);
            }
            
            logger.info("[correlationId:{}] Policy documents uploaded successfully: {} out of {}", 
                       MDC.get("correlationId"), uploadedDocuments.size(), files.length);
            
            return responseObj.render(responseObj.formSuccessResponse(
                Constants.SUCCESS, uploadedDocuments, (long) uploadedDocuments.size()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error uploading policy documents", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse("Error uploading policy documents: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Page<Document>>> getPolicyDocuments(String policyId, Pageable pageable) {
        logger.info("[correlationId:{}] getPolicyDocuments called for policy: {}", MDC.get("correlationId"), policyId);
        
        BaseResponse<Page<Document>> responseObj = new BaseResponse<>();
        
        try {
            List<Document> documents = documentRepository.findByEntityAndCategory(
                DocumentEntityType.POLICY, policyId, DocumentCategory.POLICY_DOCUMENTS);
            // Convert to Page manually
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), documents.size());
            List<Document> pageContent = documents.subList(start, end);
            Page<Document> page = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, documents.size());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, page, page.getTotalElements()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching policy documents for policy: {}", 
                        MDC.get("correlationId"), policyId, e);
            return responseObj.render(responseObj.formErrorResponse("Error fetching policy documents: " + e.getMessage()));
        }
    }

    // Claims Document Operations
    @Override
    public ResponseEntity<ResponseDto<List<Document>>> uploadClaimsDocuments(
            MultipartFile[] files,
            String claimId,
            DocumentType documentType,
            UUID uploadedBy,
            com.vimainsurance.vimaadmin.enums.UserRole uploadedByRole) {
        
        logger.info("[correlationId:{}] uploadClaimsDocuments called for claim: {} with {} files by user: {}", 
                   MDC.get("correlationId"), claimId, files.length, uploadedBy);
        
        BaseResponse<List<Document>> responseObj = new BaseResponse<>();
        List<Document> uploadedDocuments = new ArrayList<>();
        
        try {
            for (MultipartFile file : files) {
                // Validate file
                ResponseEntity<ResponseDto<String>> validationResult = validateFile(file, null);
                if (validationResult.getBody() != null && validationResult.getBody().getErrorCode() != null) {
                    logger.warn(FILE_VALIDATION_FAILED, 
                               MDC.get(CORRELATION_ID), validationResult.getBody().getMessage());
                    continue;
                }
                
                // Generate S3 key
                String s3Key = generateS3Key(DocumentEntityType.CLAIM, claimId, documentType, file.getOriginalFilename(), false);
                
                // Upload to S3
                String s3Url = s3Service.uploadFile(file, s3Key);
                
                // Create document entity
                Document document = createDocument(
                    DocumentEntityType.CLAIM, claimId, documentType, DocumentCategory.CLAIM_DOCUMENTS,
                    s3Key, file, uploadedBy, uploadedByRole, null
                );
                
                // Save to database
                Document savedDocument = documentRepository.save(document);
                uploadedDocuments.add(savedDocument);
            }
            
            logger.info("[correlationId:{}] Claims documents uploaded successfully: {} out of {}", 
                       MDC.get("correlationId"), uploadedDocuments.size(), files.length);
            
            return responseObj.render(responseObj.formSuccessResponse(
                Constants.SUCCESS, uploadedDocuments, (long) uploadedDocuments.size()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error uploading claims documents", MDC.get("correlationId"), e);
            return responseObj.render(responseObj.formErrorResponse("Error uploading claims documents: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Page<Document>>> getClaimsDocuments(String claimId, Pageable pageable) {
        logger.info("[correlationId:{}] getClaimsDocuments called for claim: {}", MDC.get("correlationId"), claimId);
        
        BaseResponse<Page<Document>> responseObj = new BaseResponse<>();
        
        try {
            List<Document> documents = documentRepository.findByEntityAndCategory(
                DocumentEntityType.CLAIM, claimId, DocumentCategory.CLAIM_DOCUMENTS);
            // Convert to Page manually
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), documents.size());
            List<Document> pageContent = documents.subList(start, end);
            Page<Document> page = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, documents.size());
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, page, page.getTotalElements()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching claims documents for claim: {}", 
                        MDC.get("correlationId"), claimId, e);
            return responseObj.render(responseObj.formErrorResponse("Error fetching claims documents: " + e.getMessage()));
        }
    }

    // Common Document Operations
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Document>> getDocumentById(UUID documentId) {
        logger.info("[correlationId:{}] getDocumentById called for ID: {}", MDC.get("correlationId"), documentId);
        
        BaseResponse<Document> responseObj = new BaseResponse<>();
        
        try {
            Optional<Document> document = documentRepository.findByDocumentId(documentId);
            if (document.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(DOCUMENT_NOT_FOUND));
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, document.get()));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching document by ID: {}", MDC.get("correlationId"), documentId, e);
            return responseObj.render(responseObj.formErrorResponse("Error fetching document: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<String>> getDocumentDownloadUrl(UUID documentId, UUID requestedBy) {
        logger.info("[correlationId:{}] getDocumentDownloadUrl called for ID: {} by user: {}", 
                   MDC.get("correlationId"), documentId, requestedBy);
        
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(documentId);
            if (documentOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(DOCUMENT_NOT_FOUND));
            }
            
            Document document = documentOpt.get();
            
            // Generate pre-signed URL from S3
            String downloadUrl = s3Service.generatePresignedUrl(document.getS3Key());
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, downloadUrl));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error generating download URL for document: {}", 
                        MDC.get("correlationId"), documentId, e);
            return responseObj.render(responseObj.formErrorResponse("Error generating download URL: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<String>> deleteDocument(UUID documentId, UUID deletedBy) {
        logger.info("[correlationId:{}] deleteDocument called for ID: {} by user: {}", 
                   MDC.get("correlationId"), documentId, deletedBy);
        
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(documentId);
            if (documentOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(DOCUMENT_NOT_FOUND));
            }
            
            Document document = documentOpt.get();
            
            // Delete from S3
            s3Service.deleteFile(document.getS3Key());
            
            // Delete from database
            documentRepository.delete(document);
            
            logger.info("[correlationId:{}] Document deleted successfully: {}", 
                       MDC.get("correlationId"), documentId);
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "Document deleted successfully"));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error deleting document: {}", 
                        MDC.get("correlationId"), documentId, e);
            return responseObj.render(responseObj.formErrorResponse("Error deleting document: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<Document>> updateDocumentNotes(UUID documentId, String notes) {
        logger.info("[correlationId:{}] updateDocumentNotes called for ID: {}", 
                   MDC.get("correlationId"), documentId);
        
        BaseResponse<Document> responseObj = new BaseResponse<>();
        
        try {
            Optional<Document> documentOpt = documentRepository.findByDocumentId(documentId);
            if (documentOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse(DOCUMENT_NOT_FOUND));
            }
            
            Document document = documentOpt.get();
            document.setNotes(notes);
            
            Document updatedDocument = documentRepository.save(document);
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, updatedDocument));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error updating document notes: {}", 
                        MDC.get("correlationId"), documentId, e);
            return responseObj.render(responseObj.formErrorResponse("Error updating document: " + e.getMessage()));
        }
    }

    // Document Count Operations
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Long>> getKYCDocumentCount(String entityId) {
        logger.info("[correlationId:{}] getKYCDocumentCount called for entity: {}", MDC.get("correlationId"), entityId);
        
        BaseResponse<Long> responseObj = new BaseResponse<>();
        
        try {
            Long count = documentRepository.countByEntityAndCategory(
                DocumentEntityType.LEAD, entityId, DocumentCategory.KYC_DOCUMENTS);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, count));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching KYC document count for entity: {}", 
                        MDC.get("correlationId"), entityId, e);
            return responseObj.render(responseObj.formErrorResponse(ERROR_FETCHING_DOCUMENT_COUNT + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Long>> getPolicyDocumentCount(String policyId) {
        logger.info("[correlationId:{}] getPolicyDocumentCount called for policy: {}", MDC.get("correlationId"), policyId);
        
        BaseResponse<Long> responseObj = new BaseResponse<>();
        
        try {
            Long count = documentRepository.countByEntityAndCategory(
                DocumentEntityType.POLICY, policyId, DocumentCategory.POLICY_DOCUMENTS);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, count));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching policy document count for policy: {}", 
                        MDC.get("correlationId"), policyId, e);
            return responseObj.render(responseObj.formErrorResponse(ERROR_FETCHING_DOCUMENT_COUNT + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<Long>> getClaimsDocumentCount(String claimId) {
        logger.info("[correlationId:{}] getClaimsDocumentCount called for claim: {}", MDC.get("correlationId"), claimId);
        
        BaseResponse<Long> responseObj = new BaseResponse<>();
        
        try {
            Long count = documentRepository.countByEntityAndCategory(
                DocumentEntityType.CLAIM, claimId, DocumentCategory.CLAIM_DOCUMENTS);
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, count));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error fetching claims document count for claim: {}", 
                        MDC.get("correlationId"), claimId, e);
            return responseObj.render(responseObj.formErrorResponse(ERROR_FETCHING_DOCUMENT_COUNT + e.getMessage()));
        }
    }

    // Validation Operations
    @Override
    public ResponseEntity<ResponseDto<String>> validateFile(MultipartFile file, DocumentType documentType) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        
        try {
            // Check if file is empty
            if (file.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("File is empty"));
            }
            
            // Check file size
            long maxSize = isImageFile(file.getContentType()) ? maxImageSize : maxFileSize;
            if (file.getSize() > maxSize) {
                return responseObj.render(responseObj.formErrorResponse(
                    "File size exceeds maximum allowed size of " + (maxSize / (1024 * 1024)) + "MB"));
            }
            
            // Check file extension
            String filename = file.getOriginalFilename();
            if (filename == null || !hasValidExtension(filename)) {
                return responseObj.render(responseObj.formErrorResponse("Invalid file extension"));
            }
            
            // Check MIME type
            if (!isValidMimeType(file.getContentType())) {
                return responseObj.render(responseObj.formErrorResponse("Invalid file type"));
            }
            
            return responseObj.render(responseObj.formSuccessResponse(Constants.SUCCESS, "File is valid"));
            
        } catch (Exception e) {
            logger.error("[correlationId:{}] Error validating file: {}", MDC.get("correlationId"), e.getMessage());
            return responseObj.render(responseObj.formErrorResponse("Error validating file: " + e.getMessage()));
        }
    }

    // Private utility methods
    private Document createDocument(
            DocumentEntityType entityType, String entityId, DocumentType documentType, 
            DocumentCategory category, String s3Key, MultipartFile file, 
            UUID uploadedBy, com.vimainsurance.vimaadmin.enums.UserRole uploadedByRole, String notes) {
        
        Document document = new Document();
        document.setEntityType(entityType);
        document.setEntityId(entityId);
        document.setDocumentType(documentType);
        document.setDocumentCategory(category);
        document.setS3Bucket(s3Config.getBucketName());
        document.setS3Key(s3Key);
        document.setOriginalFilename(file.getOriginalFilename());
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setUploadedBy(uploadedBy);
        document.setUploadedByRole(uploadedByRole);
        document.setUploadedAt(LocalDateTime.now());
        document.setNotes(notes);
        return document;
    }
    
    private String generateS3Key(DocumentEntityType entityType, String entityId, DocumentType documentType, String originalFilename, Boolean isMasked) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(originalFilename);
        if(!isMasked){
            return String.format("%s/%s/original/%s_%s%s", entityType.getValue().toLowerCase(), entityId, documentType.getValue(), timestamp, extension);
        }
        return String.format("%s/%s/masked/%s_%s%s", entityType.getValue().toLowerCase(), entityId, documentType.getValue(), timestamp, extension);
    }
    
    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
    
    private boolean hasValidExtension(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return allowedExtensions.contains(extension);
    }
    
    private boolean isValidMimeType(String mimeType) {
        return mimeType != null && allowedMimeTypes.contains(mimeType.toLowerCase());
    }
    
    private boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
    
}