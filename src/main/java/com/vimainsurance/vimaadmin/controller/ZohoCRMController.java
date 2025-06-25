package com.vimainsurance.vimaadmin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.ZohoSyncResponseDto;
import com.vimainsurance.vimaadmin.service.IZohoCRMService;

@RestController
@CrossOrigin(allowedHeaders = "*")
@RequestMapping("/api/v1/zoho")
@PreAuthorize("hasRole('ADMIN')")
public class ZohoCRMController {

    private static final Logger logger = LoggerFactory.getLogger(ZohoCRMController.class);

    @Autowired
    private IZohoCRMService zohoCRMService;

    @GetMapping("/sync")
    public ResponseEntity<ResponseDto<ZohoSyncResponseDto>> syncZohoData() {
        logger.info("[correlationId:{}] /sync endpoint called", MDC.get("correlationId"));
        return zohoCRMService.syncZohoData();
    }

    @GetMapping("/leads/{leadId}")
    public ResponseEntity<ResponseDto<Object>> getLeadById(@PathVariable String leadId) {
        logger.info("[correlationId:{}] /leads/{} endpoint called", MDC.get("correlationId"), leadId);
        return zohoCRMService.getLeadById(leadId);
    }

    @GetMapping("/leads")
    public ResponseEntity<ResponseDto<Object>> getAllLeads(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("[correlationId:{}] /leads endpoint called", MDC.get("correlationId"));
        return zohoCRMService.getAllLeadsByAgents(page, size, "884155000000351000");
    }
} 