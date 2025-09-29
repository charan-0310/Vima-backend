package com.vimainsurance.vimaadmin.service;

import com.vimainsurance.vimaadmin.dto.CommonResponseDto;
import com.vimainsurance.vimaadmin.dto.GenericRequestDto;
import org.springframework.http.ResponseEntity;

public interface VendorApiService {
    ResponseEntity<CommonResponseDto> callVendorApi(String vendorName, String apiKey, Boolean isAuth, GenericRequestDto requestDto);
} 