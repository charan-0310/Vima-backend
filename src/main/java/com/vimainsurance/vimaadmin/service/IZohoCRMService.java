package com.vimainsurance.vimaadmin.service;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.ZohoSyncResponseDto;
import com.vimainsurance.vimaadmin.dto.DealStageResponseDto;

public interface IZohoCRMService {
    ResponseEntity<ResponseDto<ZohoSyncResponseDto>> syncZohoData();
    ResponseEntity<ResponseDto<Object>> getLeadById(String leadId);
    ResponseEntity<ResponseDto<Object>> getAllLeads(int page, int size);
    ResponseEntity<ResponseDto<Object>> getAllLeadsByAgents(int page, int size, String agentId);
    ResponseEntity<ResponseDto<DealStageResponseDto>> getDealStageByPhone(String phoneNumber);
} 