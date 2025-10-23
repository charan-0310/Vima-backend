package com.vimainsurance.vimaadmin.dto;

import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.enums.DocumentType;
import com.vimainsurance.vimaadmin.enums.UserRole;
import com.vimainsurance.vimaadmin.enums.DocumentEntityType;


import lombok.Data;

import java.util.UUID;

@Data
public class DocumentRequestDto {
    private MultipartFile[] files;
    private String entityId;
    private String documentType;
    private UUID uploadedBy;
    private UserRole uploadedByRole;
    private String notes;
}
