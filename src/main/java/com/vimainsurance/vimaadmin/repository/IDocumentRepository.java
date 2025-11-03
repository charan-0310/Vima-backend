package com.vimainsurance.vimaadmin.repository;

import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.enums.DocumentCategory;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;
import com.vimainsurance.vimaadmin.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Document entity
 * 
 * Provides data access methods for document management
 */
@Repository
public interface IDocumentRepository extends JpaRepository<Document, UUID> {

    // Find documents by entity
    List<Document> findByEntityTypeAndEntityId(DocumentEntityType entityType, String entityId);
    
    @Query("SELECT d FROM Document d WHERE d.entityId = :entityId")
    List<Document> findByEntityId(@Param("entityId") String entityId);
    Page<Document> findByEntityTypeAndEntityId(DocumentEntityType entityType, String entityId, Pageable pageable);

    // Find documents by category
    List<Document> findByDocumentCategory(DocumentCategory category);
    
    Page<Document> findByDocumentCategory(DocumentCategory category, Pageable pageable);

    // Find documents by type
    List<Document> findByDocumentType(DocumentType documentType);
    
    Page<Document> findByDocumentType(DocumentType documentType, Pageable pageable);

    // Find documents by uploader
    List<Document> findByUploadedBy(UUID uploadedBy);
    
    Page<Document> findByUploadedBy(UUID uploadedBy, Pageable pageable);

    // Find documents by uploader role
    List<Document> findByUploadedByRole(com.vimainsurance.vimaadmin.enums.UserRole role);
    
    Page<Document> findByUploadedByRole(com.vimainsurance.vimaadmin.enums.UserRole role, Pageable pageable);

    // Find documents uploaded within date range
    List<Document> findByUploadedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    Page<Document> findByUploadedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Complex queries
    @Query("SELECT d FROM Document d WHERE d.entityType = :entityType AND d.entityId = :entityId AND d.documentCategory = :category")
    List<Document> findByEntityAndCategory(@Param("entityType") DocumentEntityType entityType, 
                                          @Param("entityId") String entityId, 
                                          @Param("category") DocumentCategory category);

    @Query("SELECT d FROM Document d WHERE d.entityType = :entityType AND d.entityId = :entityId AND d.documentType = :documentType")
    List<Document> findByEntityAndType(@Param("entityType") DocumentEntityType entityType, 
                                      @Param("entityId") String entityId, 
                                      @Param("documentType") DocumentType documentType);

    // Count queries for analytics
    @Query("SELECT COUNT(d) FROM Document d WHERE d.entityType = :entityType AND d.entityId = :entityId")
    Long countByEntity(@Param("entityType") DocumentEntityType entityType, @Param("entityId") String entityId);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.entityType = :entityType AND d.entityId = :entityId AND d.documentCategory = :category")
    Long countByEntityAndCategory(@Param("entityType") DocumentEntityType entityType, 
                                 @Param("entityId") String entityId, 
                                 @Param("category") DocumentCategory category);

    // Find documents by S3 key (for duplicate detection)
    List<Document> findByS3Key(String s3Key);

    // Find documents by original filename
    List<Document> findByOriginalFilename(String originalFilename);

    // Search documents by notes
    @Query("SELECT d FROM Document d WHERE d.notes LIKE %:searchTerm%")
    Page<Document> findByNotesContaining(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Get document statistics
    @Query("SELECT d.documentCategory, COUNT(d) FROM Document d GROUP BY d.documentCategory")
    List<Object[]> getDocumentCountByCategory();

    @Query("SELECT d.documentType, COUNT(d) FROM Document d GROUP BY d.documentType")
    List<Object[]> getDocumentCountByType();

    @Query("SELECT d.uploadedByRole, COUNT(d) FROM Document d GROUP BY d.uploadedByRole")
    List<Object[]> getDocumentCountByUploaderRole();

    // Find recent documents
    @Query("SELECT d FROM Document d ORDER BY d.uploadedAt DESC")
    Page<Document> findRecentDocuments(Pageable pageable);

    // Optimized query to find documents by entity ID and multiple categories
    @Query("SELECT d FROM Document d WHERE d.entityId = :entityId AND d.documentCategory IN :categories")
    List<Document> findByEntityIdAndCategoryIn(@Param("entityId") String entityId, 
                                              @Param("categories") List<DocumentCategory> categories);

    // Find documents by file size range
    @Query("SELECT d FROM Document d WHERE d.fileSize BETWEEN :minSize AND :maxSize")
    List<Document> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    // Find documents by MIME type
    List<Document> findByMimeType(String mimeType);
    
    @Query("SELECT d FROM Document d WHERE d.mimeType LIKE :mimeTypePattern")
    List<Document> findByMimeTypeLike(@Param("mimeTypePattern") String mimeTypePattern);

    Optional<Document> findByDocumentId(UUID fromString);
}
