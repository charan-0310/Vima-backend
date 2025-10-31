package com.vimainsurance.vimaadmin.controller;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.DealsDashboardResponseDto;
import com.vimainsurance.vimaadmin.dto.DealsRequestDto;
import com.vimainsurance.vimaadmin.dto.DealsResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.PolicyUploadRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.IDealsService;



@RestController
@RequestMapping("/api/v1/deals")
public class DealController {
    private static final Logger logger = LoggerFactory.getLogger(DealController.class);
    
    @Autowired
    private IDealsService dealsService;

    

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<List<DealsResponseDto>>> getAllDeals() {
        return dealsService.getAllDeals();
    }
    @GetMapping("/{individualId}")
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<DealsResponseDto>> getDealsById(@PathVariable UUID individualId) {
        return dealsService.getDealsById(individualId);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> createDeals(@RequestBody DealsRequestDto dealsRequestDto) {
        return dealsService.createDeals(dealsRequestDto);
    }

    @PutMapping("/{individualId}")
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> updateDeals(@PathVariable UUID individualId, @RequestBody DealsRequestDto dealsRequestDto) {
        return dealsService.updateDeals(individualId, dealsRequestDto);
    }

    @DeleteMapping("/{individualId}")
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> deleteDeals(@PathVariable UUID individualId) {
        return dealsService.deleteDeals(individualId);
    }

    @GetMapping("/{individualId}/documents")
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(@PathVariable UUID individualId) {
        return dealsService.getDocuments(individualId);
    }

    @PostMapping("/{individualId}/documents/upload")
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) {
        return dealsService.downloadDocument(documentId);
    }

    @DeleteMapping("/documents/{individualId}/{documentId}")
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> deleteDocument(@PathVariable String documentId) {
        return dealsService.deleteDocument(documentId);
    }

    @PostMapping(value = "/policy/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> uploadPolicyDocument(
            @ModelAttribute PolicyUploadRequestDto requestDto) {
        logger.info("[correlationId:{}] /policy/upload endpoint called", MDC.get("correlationId"));
        
        return dealsService.uploadPolicyWithDetails(requestDto);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('VIMA_ADMIN', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<DealsDashboardResponseDto>> getDashboardMetrics() {
        logger.info("[correlationId:{}] /dashboard endpoint called", MDC.get("correlationId"));
        return dealsService.getDashboardMetrics();
    }

}
