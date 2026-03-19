package com.vimainsurance.vimaadmin.service;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigRequestDto;
import com.vimainsurance.vimaadmin.dto.CompanyEnrollmentConfigResponseDto;
import com.vimainsurance.vimaadmin.dto.ResponseDto;

public interface ICompanyEnrollmentConfigService {

    ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> getByCompanyId(UUID companyId);

    ResponseEntity<ResponseDto<CompanyEnrollmentConfigResponseDto>> createOrUpdate(UUID companyId, CompanyEnrollmentConfigRequestDto dto);

    /**
     * Returns config for company; if not present returns defaults (all false/zero) without persisting.
     */
    CompanyEnrollmentConfigResponseDto getConfigForCompany(UUID companyId);
}
