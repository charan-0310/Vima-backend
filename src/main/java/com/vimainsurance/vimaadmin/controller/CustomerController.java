package com.vimainsurance.vimaadmin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.CustomerRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerResponseDto;
import com.vimainsurance.vimaadmin.dto.CustomerPipelineRequestDto;
import com.vimainsurance.vimaadmin.dto.CustomerBulkDeleteRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.service.ICustomerService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private ICustomerService iCustomerService;

    @PostMapping("/agent/{username}/customer")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<String>> create(@RequestBody CustomerRequestDto requestDto, @PathVariable String username){
        logger.info("[correlationId:{}] /agent/{}/customer endpoint called", MDC.get("correlationId"), username);
        return iCustomerService.create(requestDto, username);
    }

    @PutMapping("/customer")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<String>> update(@RequestBody CustomerRequestDto requestDto){
        logger.info("[correlationId:{}] /customer (PUT) endpoint called", MDC.get("correlationId"));
        return iCustomerService.update(requestDto);
    }

    @DeleteMapping("/customers")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<String>> bulkDelete(@RequestBody CustomerBulkDeleteRequestDto requestDto) {
        logger.info("[correlationId:{}] /customers (DELETE) endpoint called for bulk deletion", MDC.get("correlationId"));
        return iCustomerService.bulkDelete(requestDto);
    }

    /**
     * Get customers for a specific agent with advanced filtering and sorting
     * 
     * Query Optimization:
     * - Uses dedicated searchCustomersByCreatedBy when search is provided
     * - Uses basic findActiveByCreatedBy when no search is applied
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
    @GetMapping("/agent/{username}/customers")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getCustomerByAgent(
            @PathVariable String username,
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "10", required = false) int rec,
            @RequestParam(defaultValue = "", required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc", required = false) String sortDirection) {
        logger.info("[correlationId:{}] /agent/{}/customers endpoint called", MDC.get("correlationId"), username);
        return iCustomerService.findByAgent(username, search, page, rec, sortBy, sortDirection);
    }

    @GetMapping("/customers/{custid}")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<CustomerResponseDto>> getCustomerById(@PathVariable String custid){
        logger.info("[correlationId:{}] /customers/{} endpoint called", MDC.get("correlationId"), custid);
        return iCustomerService.getByCustId(custid);
    }

    @GetMapping("/admin/customers")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<List<CustomerResponseDto>>> getAll(@RequestParam int page, @RequestParam int rec){
        logger.info("/admin/customers", MDC.get("correlationId"));
        return iCustomerService.getAllCustomers(page, rec);
    }

    @PutMapping("/agent/{username}/customer/{customerId}/pipeline")
    @PreAuthorize("hasRole('SALES_AGENT')")
    public ResponseEntity<ResponseDto<String>> updatePipelineStatus(
            @PathVariable String username,
            @PathVariable String customerId,
            @RequestBody CustomerPipelineRequestDto requestDto) {
        logger.info("[correlationId:{}] /agent/{}/customer/{}/pipeline endpoint called", MDC.get("correlationId"), username, customerId);
        return iCustomerService.updatePipelineStatus(username, customerId, requestDto);
    }
}
