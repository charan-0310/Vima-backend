package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for a single uploaded policy document (policy wording PDF or
 * claim checklist PDF). Returned to admin policy responses so the UI can
 * show the currently uploaded filename + size. PDFs are intentionally not
 * exposed to employees, so no presigned URL is included here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDocumentRefDto {
    private UUID documentId;
    private String filename;
    private Long fileSizeBytes;
    private String mimeType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;
}
