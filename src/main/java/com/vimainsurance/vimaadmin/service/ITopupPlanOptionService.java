package com.vimainsurance.vimaadmin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupPlanOptionRequestDto;
import com.vimainsurance.vimaadmin.dto.TopupPlanOptionResponseDto;
import com.vimainsurance.vimaadmin.dto.TopupOptionsResponseDto;

public interface ITopupPlanOptionService {

    ResponseEntity<ResponseDto<List<TopupPlanOptionResponseDto>>> listByCompany(UUID companyId, String planType);

    ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> getById(UUID id);

    ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> create(TopupPlanOptionRequestDto dto);

    ResponseEntity<ResponseDto<TopupPlanOptionResponseDto>> update(UUID id, UUID companyId, TopupPlanOptionRequestDto dto);

    ResponseEntity<ResponseDto<String>> softDelete(UUID id, UUID companyId);

    /**
     * For employee enrollment: active options with premium preview per sum-insured.
     * Validates enrollment token and checks base GMC before returning options.
     */
    ResponseEntity<ResponseDto<TopupOptionsResponseDto>> getActiveOptionsWithPremiumPreview(String enrollmentToken);
}
