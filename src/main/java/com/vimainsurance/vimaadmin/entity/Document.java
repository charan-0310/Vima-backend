package com.vimainsurance.vimaadmin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document Entity
 * 
 * Maps to: document.documents table
 * 
 * Database Schema:
 * CREATE TABLE document.documents (
 *     document_id BIGSERIAL PRIMARY KEY,
 *     entity_type document.document_entity_type_enum NOT NULL,
 *     entity_id VARCHAR(50) NOT NULL,
 *     document_type document.document_type_enum NOT NULL,
 *     document_category document.document_category_enum NOT NULL,
 *     s3_bucket VARCHAR(255) NOT NULL,
 *     s3_key TEXT NOT NULL,
 *     original_filename VARCHAR(255),
 *     file_size BIGINT,
 *     mime_type VARCHAR(100),
 *     uploaded_by UUID,
 *     uploaded_by_role admin.user_role_enum,
 *     uploaded_at TIMESTAMP DEFAULT now(),
 *     notes TEXT,
 *     created_at TIMESTAMP DEFAULT now(),
 *     updated_at TIMESTAMP DEFAULT now()
 * );
 */
@Entity
@Table(name = "documents", schema = "document")
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue
    @Column(name="document_id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID documentId;

    // Entity Reference
    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private com.vimainsurance.vimaadmin.enums.DocumentEntityType entityType;

    @Column(name = "entity_id", nullable = false, length = 50)
    private String entityId;

    // Document Classification
    @Column(name = "document_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private com.vimainsurance.vimaadmin.enums.DocumentType documentType;

    @Column(name = "document_category", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private com.vimainsurance.vimaadmin.enums.DocumentCategory documentCategory;

    // S3 Storage
    @Column(name = "s3_bucket", nullable = false, length = 255)
    private String s3Bucket;

    @Column(name = "s3_key", nullable = false, columnDefinition = "TEXT")
    private String s3Key;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // Upload Metadata
    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "uploaded_by_role")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private com.vimainsurance.vimaadmin.enums.UserRole uploadedByRole;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    // Optional Notes
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Audit Fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Document() {
        this.uploadedAt = LocalDateTime.now();
    }

    public Document(com.vimainsurance.vimaadmin.enums.DocumentEntityType entityType,
                   String entityId,
                   com.vimainsurance.vimaadmin.enums.DocumentType documentType,
                   com.vimainsurance.vimaadmin.enums.DocumentCategory documentCategory,
                   String s3Bucket,
                   String s3Key) {
        this();
        this.entityType = entityType;
        this.entityId = entityId;
        this.documentType = documentType;
        this.documentCategory = documentCategory;
        this.s3Bucket = s3Bucket;
        this.s3Key = s3Key;
    }

    // Utility methods
    public String getFullS3Path() {
        return s3Bucket + "/" + s3Key;
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isPdf() {
        return mimeType != null && mimeType.equals("application/pdf");
    }

    public String getFileExtension() {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "Document{" +
                "documentId=" + documentId +
                ", entityType=" + entityType +
                ", entityId='" + entityId + '\'' +
                ", documentType=" + documentType +
                ", documentCategory=" + documentCategory +
                ", originalFilename='" + originalFilename + '\'' +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
