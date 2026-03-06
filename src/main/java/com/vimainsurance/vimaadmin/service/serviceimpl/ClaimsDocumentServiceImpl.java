package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.config.S3Config;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDocumentDto;
import com.vimainsurance.vimaadmin.dto.claim.ClaimDocumentListResponse;
import com.vimainsurance.vimaadmin.dto.claim.DocumentUploadResponse;
import com.vimainsurance.vimaadmin.entity.Claim;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.exception.BadRequestException;
import com.vimainsurance.vimaadmin.repository.IClaimRepository;
import com.vimainsurance.vimaadmin.repository.IDealsRepository;
import com.vimainsurance.vimaadmin.repository.IDocumentRepository;
import com.vimainsurance.vimaadmin.service.IClaimsDocumentService;
import com.vimainsurance.vimaadmin.service.IS3Service;
import com.vimainsurance.vimaadmin.service.claim.ClaimAuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaimsDocumentServiceImpl implements IClaimsDocumentService {

    private static final int MAX_CLAIM_DOCUMENTS = 10;
    private static final long MAX_FILE_SIZE_CLAIM_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> CLAIM_ALLOWED_EXTENSIONS = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> CLAIM_ALLOWED_MIME = Set.of(
            "application/pdf", "image/jpeg", "image/jpg", "image/png", "image/webp");

    /** Infer MIME from extension for mobile clients that send null Content-Type. */
    private static String mimeFromExtension(String ext) {
        if (ext == null) return null;
        return switch (ext.toLowerCase()) {
            case ".pdf" -> "application/pdf";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            default -> null;
        };
    }

    /** Extension for S3/key when filename is missing (from validated MIME). */
    private static String extensionFromMime(String mime) {
        if (mime == null) return ".jpg";
        return switch (mime.toLowerCase()) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private static final Set<ClaimStatus> TERMINAL_STATUSES = Set.of(
            ClaimStatus.SETTLED, ClaimStatus.REJECTED, ClaimStatus.REJECTED_BY_ADMIN, ClaimStatus.CLOSED);

    private static final Set<DocumentType> EMPLOYEE_ALLOWED_DOC_TYPES = Set.of(
            DocumentType.CLAIM_FORM, DocumentType.MEDICAL_BILL, DocumentType.DISCHARGE_SUMMARY,
            DocumentType.PRESCRIPTION, DocumentType.LAB_REPORT, DocumentType.DIAGNOSTIC_REPORT,
            DocumentType.QUERY_RESPONSE_DOC);

    private static final Set<DocumentType> ADMIN_EXTRA_DOC_TYPES = Set.of(
            DocumentType.CLAIM_LETTER_APPROVAL, DocumentType.CLAIM_LETTER_REJECTION,
            DocumentType.CLAIM_LETTER_QUERY, DocumentType.CLAIM_LETTER_PAID,
            DocumentType.SETTLEMENT_DOCUMENT);

    private final IClaimRepository claimRepository;
    private final IDocumentRepository documentRepository;
    private final IDealsRepository dealsRepository;
    private final IS3Service s3Service;
    private final S3Config s3Config;
    private final ClaimAuditService claimAuditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<DocumentUploadResponse>> uploadClaimsDocuments(
            MultipartFile[] files,
            UUID claimId,
            DocumentType documentType,
            UUID uploadedBy,
            String role) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "At least one file is required"));
        }
        Optional<Claim> claimOpt = claimRepository.findById(claimId);
        if (claimOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseDto<>(404, "Claim not found"));
        }
        Claim claim = claimOpt.get();
        if (claim.getInternalStatus() == ClaimStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>(403, "Claim is closed and cannot be modified"));
        }
        boolean isAdmin = isAdminRole(role);
        if (!isAdmin) {
            if (!Objects.equals(claim.getEmployee().getIndividualId(), uploadedBy)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ResponseDto<>(403, "You can only upload documents to your own claims"));
            }
            if (TERMINAL_STATUSES.contains(claim.getInternalStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ResponseDto<>(403, "Cannot upload documents to a claim in terminal status"));
            }
        }
        validateDocumentTypeForRole(documentType, role);
        long currentCount = documentRepository.countByEntityAndCategory(
                DocumentEntityType.CLAIM, claimId.toString(), DocumentCategory.CLAIM_DOCUMENTS);
        if (currentCount + files.length > MAX_CLAIM_DOCUMENTS) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400,
                    "BR-DOC-001: Maximum " + MAX_CLAIM_DOCUMENTS + " documents per claim. Current: " + currentCount));
        }
        List<DocumentUploadResponse.UploadedDocumentItem> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            validateClaimFile(file);
            String origName = file.getOriginalFilename();
            String mime = file.getContentType();
            String ext;
            String effectiveFilename;
            if (origName != null && !origName.isBlank()) {
                ext = getFileExtension(origName).toLowerCase();
                effectiveFilename = origName;
            } else {
                ext = extensionFromMime(mime != null ? mime : "image/jpeg");
                effectiveFilename = "document-" + UUID.randomUUID() + ext;
            }
            String s3Key = "claims/" + claimId + "/" + documentType.name() + "/" + UUID.randomUUID() + ext;
            s3Service.uploadFile(file, s3Key);
            String effectiveMime = (mime != null && !mime.isBlank()) ? mime : mimeFromExtension(ext);
            if (effectiveMime == null) effectiveMime = "image/jpeg";
            Document doc = new Document();
            doc.setEntityType(DocumentEntityType.CLAIM);
            doc.setEntityId(claimId.toString());
            doc.setDocumentType(documentType);
            doc.setDocumentCategory(DocumentCategory.CLAIM_DOCUMENTS);
            doc.setS3Bucket(s3Config.getBucketName());
            doc.setS3Key(s3Key);
            doc.setOriginalFilename(effectiveFilename);
            doc.setFileSize(file.getSize());
            doc.setMimeType(effectiveMime);
            doc.setUploadedBy(uploadedBy);
            doc.setUploadedByRole(isAdmin ? parseUserRole(role) : null);
            doc.setSyncedToInsurer(false);
            doc.setUploadedAt(LocalDateTime.now());
            doc = documentRepository.save(doc);
            uploaded.add(DocumentUploadResponse.UploadedDocumentItem.builder()
                    .id(doc.getDocumentId().toString())
                    .fileName(doc.getOriginalFilename())
                    .documentType(doc.getDocumentType())
                    .size(doc.getFileSize())
                    .build());
        }
        claimAuditService.logAction(claimId, "DOCUMENT_UPLOADED", null, null, uploadedBy, isAdmin ? role : "EMPLOYEE",
                "Uploaded " + uploaded.size() + " document(s)");
        long total = currentCount + uploaded.size();
        DocumentUploadResponse response = DocumentUploadResponse.builder()
                .uploaded(uploaded)
                .totalDocuments((int) total)
                .build();
        return ResponseEntity.ok(new ResponseDto<>("Documents uploaded successfully", response));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseDto<ClaimDocumentListResponse>> getClaimDocuments(UUID claimId, UUID requestedBy, boolean isAdmin) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        if (!isAdmin && !Objects.equals(claim.getEmployee().getIndividualId(), requestedBy)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>(403, "You can only view documents for your own claims"));
        }
        List<Document> docs = documentRepository.findByEntityAndCategory(
                DocumentEntityType.CLAIM, claimId.toString(), DocumentCategory.CLAIM_DOCUMENTS);
        List<ClaimDocumentDto> dtos = docs.stream()
                .map(d -> toClaimDocumentDto(d))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ResponseDto<>("Success", ClaimDocumentListResponse.builder().documents(dtos).build()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ResponseDto<String>> deleteDocument(UUID claimId, UUID docId, UUID actorId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new BadRequestException("Claim not found"));
        if (claim.getInternalStatus() == ClaimStatus.CLOSED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ResponseDto<>(403, "Claim is closed and cannot be modified"));
        }
        Document doc = documentRepository.findByDocumentId(docId)
                .orElseThrow(() -> new BadRequestException("Document not found"));
        if (!DocumentEntityType.CLAIM.equals(doc.getEntityType()) || !claimId.toString().equals(doc.getEntityId())) {
            return ResponseEntity.badRequest().body(new ResponseDto<>(400, "Document does not belong to this claim"));
        }
        s3Service.deleteFile(doc.getS3Key());
        documentRepository.delete(doc);
        claimAuditService.logAction(claimId, "DOCUMENT_REMOVED", null, null, actorId, "ADMIN",
                "Document removed: " + doc.getOriginalFilename());
        return ResponseEntity.ok(new ResponseDto<>("Document removed successfully", "OK"));
    }

    private void validateClaimFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_CLAIM_BYTES) {
            throw new BadRequestException("File size must not exceed 5 MB");
        }
        String filename = file.getOriginalFilename();
        String ext = (filename != null && !filename.isBlank())
                ? getFileExtension(filename).toLowerCase()
                : "";
        String mime = file.getContentType();
        if (mime == null || mime.isBlank()) {
            mime = mimeFromExtension(ext);
            if (mime == null) {
                mime = "image/jpeg";
            }
        } else {
            mime = mime.toLowerCase();
        }
        if (!CLAIM_ALLOWED_MIME.contains(mime)) {
            throw new BadRequestException("Invalid file type. Allowed: PDF, JPG, JPEG, PNG, WebP");
        }
        if (filename != null && !filename.isBlank()) {
            if (!CLAIM_ALLOWED_EXTENSIONS.contains(ext)) {
                throw new BadRequestException("Invalid file type. Allowed: PDF, JPG, JPEG, PNG, WebP");
            }
        }
    }

    private void validateDocumentTypeForRole(DocumentType documentType, String role) {
        if (documentType == null) {
            throw new BadRequestException("Document type is required");
        }
        boolean isAdmin = isAdminRole(role);
        if (EMPLOYEE_ALLOWED_DOC_TYPES.contains(documentType)) {
            return;
        }
        if (isAdmin && ADMIN_EXTRA_DOC_TYPES.contains(documentType)) {
            return;
        }
        throw new BadRequestException("Document type " + documentType + " is not allowed for your role");
    }

    private boolean isAdminRole(String role) {
        if (role == null || role.isBlank()) return false;
        return !"EMPLOYEE".equalsIgnoreCase(role);
    }

    private UserRole parseUserRole(String role) {
        try {
            return UserRole.fromValue(role.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    private ClaimDocumentDto toClaimDocumentDto(Document d) {
        String downloadUrl = s3Service.generatePresignedUrl(d.getS3Key());
        String uploadedByName = resolveUploadedByName(d.getUploadedBy(), d.getUploadedByRole());
        return ClaimDocumentDto.builder()
                .id(d.getDocumentId().toString())
                .fileName(d.getOriginalFilename())
                .documentType(d.getDocumentType())
                .size(d.getFileSize())
                .downloadUrl(downloadUrl)
                .uploadedBy(uploadedByName)
                .uploadedAt(d.getUploadedAt())
                .build();
    }

    private String resolveUploadedByName(UUID uploadedBy, UserRole role) {
        if (uploadedBy == null) return "Unknown";
        if (role == null) {
            return dealsRepository.findById(uploadedBy)
                    .map(d -> d.getFullName() != null ? d.getFullName() : (d.getFirstName() + " " + (d.getLastName() != null ? d.getLastName() : "")).trim())
                    .filter(s -> !s.isBlank())
                    .orElse("Employee");
        }
        return role.getValue();
    }
}
