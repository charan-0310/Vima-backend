package com.vimainsurance.vimaadmin.controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.vimainsurance.vimaadmin.service.IDealsService;

import java.util.List;
import java.util.UUID;

import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.ConvertToDealRequestDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;



@RestController
@RequestMapping("/api/v1/deals")
@PreAuthorize("hasRole('SALES_AGENT')")
public class DealController {
    private static final Logger logger = LoggerFactory.getLogger(DealController.class);
    
    @Autowired
    private IDealsService dealsService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<DealsResponseDto>>> getAllDeals() {
        return dealsService.getAllDeals();
    }
    @GetMapping("/{individualId}")
    public ResponseEntity<ResponseDto<DealsResponseDto>> getDealsById(@PathVariable UUID individualId) {
        return dealsService.getDealsById(individualId);
    }

    @PostMapping
    public ResponseEntity<ResponseDto<String>> createDeals(@RequestBody DealsRequestDto dealsRequestDto) {
        return dealsService.createDeals(dealsRequestDto);
    }

    @PutMapping("/{individualId}")
    public ResponseEntity<ResponseDto<String>> updateDeals(@PathVariable UUID individualId, @RequestBody DealsRequestDto dealsRequestDto) {
        return dealsService.updateDeals(individualId, dealsRequestDto);
    }

    @DeleteMapping("/{individualId}")
    public ResponseEntity<ResponseDto<String>> deleteDeals(@PathVariable UUID individualId) {
        return dealsService.deleteDeals(individualId);
    }

    @GetMapping("/{individualId}/documents")
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(@PathVariable UUID individualId) {
        return dealsService.getDocuments(individualId);
    }

    @PostMapping("/{individualId}/documents/upload")
    public ResponseEntity<ResponseDto<String>> uploadDocument(
            @PathVariable UUID individualId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "notes", required = false) String notes) {
        DocumentRequestDto requestDto = new DocumentRequestDto();
        requestDto.setFiles(files);
        requestDto.setDocumentType(documentType);
        requestDto.setNotes(notes);
        return dealsService.uploadDocument(requestDto, individualId);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) {
        return dealsService.downloadDocument(documentId);
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ResponseDto<String>> deleteDocument(@PathVariable String documentId) {
        return dealsService.deleteDocument(documentId);
    }

    @PostMapping(value = "/policy/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDto<String>> uploadPolicyDocument(
            @ModelAttribute PolicyUploadRequestDto requestDto) {
        logger.info("[correlationId:{}] /policy/upload endpoint called", MDC.get("correlationId"));
        
        return dealsService.uploadPolicyWithDetails(requestDto);
    }

}
