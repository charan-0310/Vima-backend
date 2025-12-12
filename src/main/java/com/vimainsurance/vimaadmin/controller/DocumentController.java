package com.vimainsurance.vimaadmin.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IDocumentService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    @Autowired
    private IDocumentService documentService;

    @PostMapping("/{documentEntityType}/{entityId}/upload")
    public ResponseEntity<ResponseDto<String>> uploadDocument(
        @PathVariable String documentEntityType,
        @PathVariable String entityId,
        @RequestParam("file") MultipartFile file,
        @RequestParam("documentType") String documentType,
        @RequestParam("documentCategory") String documentCategory,
        @RequestParam(value = "notes", required = false) String notes
    ) {
        return documentService.uploadDocument(file, documentType, documentCategory, documentEntityType, entityId, notes);
    }
}
