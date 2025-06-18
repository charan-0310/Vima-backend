package com.vimainsurance.vimaadmin.controller;

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

    @Autowired
    private IZohoCRMService zohoCRMService;

    @GetMapping("/sync")
    public ResponseEntity<ResponseDto<ZohoSyncResponseDto>> syncZohoData() {
        return zohoCRMService.syncZohoData();
    }

    @GetMapping("/leads/{leadId}")
    public ResponseEntity<ResponseDto<Object>> getLeadById(@PathVariable String leadId) {
        return zohoCRMService.getLeadById(leadId);
    }

    @GetMapping("/leads")
    public ResponseEntity<ResponseDto<Object>> getAllLeads(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return zohoCRMService.getAllLeadsByAgents(page, size, "884155000000351000");
    }
} 