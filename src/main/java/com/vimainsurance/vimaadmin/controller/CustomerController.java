package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
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

import com.vimainsurance.vimaadmin.dto.ConvertToDealRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerBulkDeleteRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.DocumentRequestDto;
import com.vimainsurance.vimaadmin.dto.DocumentResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.ICustomerService;


@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private ICustomerService iCustomerService;

    @PutMapping("/customer")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> update(@RequestBody CustomerRequestDto requestDto){
        logger.info("[correlationId:{}] /customer (PUT) endpoint called", MDC.get("correlationId"));
        return iCustomerService.update(requestDto);
    }

    @DeleteMapping("/customers")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> bulkDelete(@RequestBody CustomerBulkDeleteRequestDto requestDto) {
        logger.info("[correlationId:{}] /customers (DELETE) endpoint called for bulk deletion", MDC.get("correlationId"));
        return iCustomerService.bulkDelete(requestDto);
    }

    @GetMapping("/customers/{custid}")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<CustomerResponseDto>> getCustomerById(@PathVariable String custid){
        logger.info("[correlationId:{}] /customers/{} endpoint called", MDC.get("correlationId"), custid);
        return iCustomerService.getByCustId(custid);
    }

    /**
     * Get all customers with advanced filtering and sorting
     * 
     * Query Optimization:
     * - Uses dedicated searchAllCustomers when search is provided
     * - Uses basic findAll when no search is applied
     * - Special handling for premium sorting with in-memory processing
     * 
     * Supported sortBy values:
     * - pipelineStage/status: Sort by customer pipeline stage
     * - premium: Sort by highest premium from quotes
     * - lastActivity/updatedAt: Sort by last activity (default)
     * - fullName: Sort by customer name
     * - createdAt: Sort by creation date
     * - city, state, email, phoneNumber: Sort by respective fields
     */
    @GetMapping("/admin/customers")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getAll(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int rec,
            @RequestParam(defaultValue = "", required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc", required = false) String sortDirection){
        logger.info("[correlationId:{}] /admin/customers endpoint called", MDC.get("correlationId"));
        return iCustomerService.getAllCustomers(search, page, rec, sortBy, sortDirection);
    }

    @PostMapping(value = "/customer/{customerId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> uploadDocument(@ModelAttribute DocumentRequestDto requestDto, @PathVariable String customerId) {
        logger.info("[correlationId:{}] /customer/{}/document endpoint called", MDC.get("correlationId"), customerId);
        return iCustomerService.uploadDocument(requestDto, customerId);
    }

    @GetMapping("/customer/{customerId}/documents")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<List<DocumentResponseDto>>> getDocuments(@PathVariable String customerId) {
        logger.info("[correlationId:{}] /customer/{}/documents endpoint called", MDC.get("correlationId"), customerId);
        return iCustomerService.getDocuments(customerId);
    }

    @GetMapping("/customer/{customerId}/{documentId}/download")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Resource> getDocumentDownloadUrl(@PathVariable String customerId, @PathVariable String documentId) {
        logger.info("[correlationId:{}] /customer/{}/{}/download endpoint called", MDC.get("correlationId"), customerId, documentId);
        return iCustomerService.downloadDocument(documentId);
    }

    @DeleteMapping("/customer/{customerId}/{documentId}")
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> deleteDocument(@PathVariable String customerId, @PathVariable String documentId) {
        logger.info("[correlationId:{}] /customer/{}/{}/delete endpoint called", MDC.get("correlationId"), customerId, documentId);
        return iCustomerService.deleteDocument(documentId);
    }

    @PostMapping(value = "/customer/{customerId}/todeal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('VIMA_ADMIN', 'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> customerToDeals(@PathVariable String customerId, @ModelAttribute ConvertToDealRequestDto requestDto) {
        logger.info("[correlationId:{}] /customer/{}/to-deals endpoint called", MDC.get("correlationId"), customerId);
        return iCustomerService.customerToDeals(requestDto, customerId);
    }
}
