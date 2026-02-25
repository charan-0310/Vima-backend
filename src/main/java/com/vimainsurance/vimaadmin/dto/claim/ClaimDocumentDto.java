package com.vimainsurance.vimaadmin.dto.claim;

import java.time.LocalDateTime;

import com.vimainsurance.vimaadmin.enums.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claim document list item with pre-signed download URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDocumentDto {
    private String id;
    private String fileName;
    private DocumentType documentType;
    private Long size;
    /** Pre-signed download URL (expires after configured TTL). */
    private String downloadUrl;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
