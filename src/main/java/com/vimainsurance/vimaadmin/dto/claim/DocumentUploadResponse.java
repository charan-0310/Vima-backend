package com.vimainsurance.vimaadmin.dto.claim;

import java.util.List;

import com.vimainsurance.vimaadmin.enums.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for claim document upload.
 * API Contract: uploaded list + totalDocuments count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    /** List of successfully uploaded documents (id, fileName, documentType, size). */
    private List<UploadedDocumentItem> uploaded;
    /** Total document count for the claim after this upload. */
    private int totalDocuments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadedDocumentItem {
        private String id;
        private String fileName;
        private DocumentType documentType;
        private Long size;
    }
}
