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

import com.vimainsurance.vimaadmin.dto.QuoteStatusUpdateDto;
import com.vimainsurance.vimaadmin.dto.QuotesRequestDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.entity.Quotes;
import com.vimainsurance.vimaadmin.service.IQuoteService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1")
public class QuoteController {

    private static final Logger logger = LoggerFactory.getLogger(QuoteController.class);

    @Autowired
    private IQuoteService iQuoteService;

    @PostMapping("/quote")
    @PreAuthorize("hasAnyAuthority('SALES_ADMIN',  'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> create(@RequestBody QuotesRequestDto requestDto) {
        logger.info("[correlationId:{}] /quote (POST) endpoint called", MDC.get("correlationId"));
        return iQuoteService.create(requestDto);
    }

    @PostMapping("/quotes")
    @PreAuthorize("hasAnyAuthority('SALES_ADMIN',  'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> createBulk(@RequestBody List<QuotesRequestDto> requestDtos) {
        logger.info("[correlationId:{}] /quotes/bulk (POST) endpoint called with {} quotes", 
                MDC.get("correlationId"), requestDtos.size());
        return iQuoteService.createBulk(requestDtos);
    }

    @PutMapping("/quote")
    @PreAuthorize("hasAnyAuthority('SALES_ADMIN',  'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> update(@RequestBody QuotesRequestDto requestDto) {
        logger.info("[correlationId:{}] /quote (PUT) endpoint called", MDC.get("correlationId"));
        return iQuoteService.update(requestDto);
    }

    @DeleteMapping("/quote")
    @PreAuthorize("hasAnyAuthority('SALES_ADMIN',  'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> delete(@RequestBody QuotesRequestDto requestDto) {
        logger.info("[correlationId:{}] /quote (DELETE) endpoint called", MDC.get("correlationId"));
        return iQuoteService.delete(requestDto);
    }

    @GetMapping("/customer/{customer}/quotes")
    @PreAuthorize("hasAnyAuthority('SALES_ADMIN',  'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<List<Quotes>>> getQuotesByCustomer(@PathVariable String customer) {
        logger.info("[correlationId:{}] /customer/{}/quotes endpoint called", MDC.get("correlationId"), customer);
        return iQuoteService.findByCustomer(customer);
    }

    @GetMapping("/quotes")
    @PreAuthorize("hasAnyAuthority('SALES_ADMIN',  'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<List<Quotes>>> getAllQuotes(
            @RequestParam int page, 
            @RequestParam int rec) {
        logger.info("[correlationId:{}] /quotes endpoint called with page={}, rec={}", 
                MDC.get("correlationId"), page, rec);
        return iQuoteService.getAllQuotes(page, rec);
    }

    @PutMapping("/quote/status")
    @PreAuthorize("hasAnyAuthority('SALES_ADMIN',  'SALES_AGENT', 'SALES_MANAGER', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ResponseDto<String>> updateQuoteStatus(
            @RequestBody QuoteStatusUpdateDto statusUpdateDto) {
        logger.info("[correlationId:{}] /quote/{}/status (PUT) endpoint called", 
                MDC.get("correlationId"));
        
        return iQuoteService.updateQuoteStatus(statusUpdateDto);
    }
}
