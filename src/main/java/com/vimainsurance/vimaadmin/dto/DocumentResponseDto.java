package com.vimainsurance.vimaadmin.dto;

import com.vimainsurance.vimaadmin.enums.DocumentType;

import java.time.LocalDateTime;


import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentResponseDto {
String documentId;      
DocumentType documentType;
LocalDateTime uploadedAt;
String documentMimeType;
String notes;
String documentName;
String category;
String size;
}
