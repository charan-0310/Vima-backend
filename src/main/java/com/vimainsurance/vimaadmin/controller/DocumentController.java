package com.vimainsurance.vimaadmin.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Document;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import java.util.List;
import com.vimainsurance.vimaadmin.service.IDocumentService;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {


    @Autowired
    private IDocumentService documentService;

    // @PostMapping("/kyc")
    // public ResponseEntity<ResponseDto<List<Document>>> uploadKYCDocuments(@RequestBody DocumentRequestDto requestDto) {
    //     return documentService.uploadKYCDocuments(requestDto.getFiles(), requestDto.getEntityId().toString(), requestDto.getDocumentType(), requestDto.getUploadedByRole());
    // }
}
