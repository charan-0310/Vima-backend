package com.vimainsurance.vimaadmin.dto;

import com.vimainsurance.vimaadmin.enums.DocumentType;

import java.time.LocalDateTime;

import com.vimainsurance.vimaadmin.enums.DocumentEntityType;

import lombok.Data;

@Data
public class DocumentResponseDto {
String documentId;      
DocumentType documentType;
LocalDateTime uploadedAt;
String documentMimeType;
String notes;
}
