package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessAccessLogResponseDto;
import com.vimainsurance.vimaadmin.dto.WellnessDashboardDto;

public interface IWellnessAccessService {

    void logAccess(UUID orgId, UUID partnerId, UUID employeeId, String userIdentifier, String accessType, String status,
            String errorMessage);

    ResponseEntity<ResponseDto<Page<WellnessAccessLogResponseDto>>> getAccessLogs(UUID orgId, int page, int size);

    ResponseEntity<ResponseDto<WellnessDashboardDto>> getDashboardStats(UUID orgId);
}
