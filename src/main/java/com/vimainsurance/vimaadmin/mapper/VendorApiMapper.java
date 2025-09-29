package com.vimainsurance.vimaadmin.mapper;

import com.vimainsurance.vimaadmin.dto.CommonResponseDto;
import com.vimainsurance.vimaadmin.dto.GenericRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VendorApiMapper {
    public Object mapToVendorRequest(String vendorName, String apiKey, GenericRequestDto requestDto) {
        // Implement mapping logic per vendor/apiKey
        return requestDto.getPayload();
    }

    public CommonResponseDto mapFromVendorResponse(String vendorName, String apiKey, Object rawResponse) {
        CommonResponseDto dto = new CommonResponseDto();
        dto.setPayload((java.util.Map<String, Object>) rawResponse);
        dto.setMessage("Mapped response");
        dto.setStatus(200);
        return dto;
    }
} 