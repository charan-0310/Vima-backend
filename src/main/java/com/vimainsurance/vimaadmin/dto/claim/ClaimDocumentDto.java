package com.vimainsurance.vimaadmin.dto.claim;

import java.time.LocalDateTime;

import com.vimainsurance.vimaadmin.enums.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claim document list item. Download via GET {@code .../claims/{claimId}/documents/{id}/download}.
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
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
