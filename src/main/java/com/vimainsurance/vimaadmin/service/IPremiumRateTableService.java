package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.vimainsurance.vimaadmin.dto.PremiumRateTableCsvUploadResultDto;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableRequestDto;
import com.vimainsurance.vimaadmin.dto.PremiumRateTableResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface IPremiumRateTableService {

    ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> create(PremiumRateTableRequestDto dto);

    ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> update(UUID id, PremiumRateTableRequestDto dto);

    ResponseEntity<ResponseDto<String>> softDelete(UUID id);

    ResponseEntity<ResponseDto<PremiumRateTableResponseDto>> getById(UUID id);

    ResponseEntity<ResponseDto<Page<PremiumRateTableResponseDto>>> list(
            UUID companyId,
            String planType,
            String pricingModel,
            Pageable pageable);

    ResponseEntity<ResponseDto<PremiumRateTableCsvUploadResultDto>> uploadCsv(UUID companyId, MultipartFile file);
}
