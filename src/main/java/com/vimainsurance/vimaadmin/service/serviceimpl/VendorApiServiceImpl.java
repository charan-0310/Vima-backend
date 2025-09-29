package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.dto.CommonResponseDto;
import com.vimainsurance.vimaadmin.dto.GenericRequestDto;
import com.vimainsurance.vimaadmin.entity.Vendor;
import com.vimainsurance.vimaadmin.entity.VendorApiEndpoint;
import com.vimainsurance.vimaadmin.entity.VendorApiHeader;
import com.vimainsurance.vimaadmin.entity.VendorToken;
import com.vimainsurance.vimaadmin.mapper.VendorApiMapper;
import com.vimainsurance.vimaadmin.repository.VendorApiEndpointRepository;
import com.vimainsurance.vimaadmin.repository.VendorApiHeaderRepository;
import com.vimainsurance.vimaadmin.repository.VendorRepository;
import com.vimainsurance.vimaadmin.repository.VendorTokenRepository;
import com.vimainsurance.vimaadmin.service.VendorApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorApiServiceImpl implements VendorApiService {
    
    @Autowired
    private final VendorRepository vendorRepository;
    
    @Autowired
    private final VendorApiEndpointRepository endpointRepository;
    
    @Autowired
    private final VendorApiHeaderRepository headerRepository;
    
    @Autowired
    private final VendorTokenRepository tokenRepository;
    
    @Autowired
    private final VendorApiMapper vendorApiMapper;
    
    @Autowired
    private final ObjectMapper objectMapper;
    
    @Autowired
    private final RestTemplate restTemplate;

    @Override
    public ResponseEntity<CommonResponseDto> callVendorApi(String vendorName, String apiKey, Boolean isAuth, GenericRequestDto requestDto) {
        // 1. Fetch vendor
        Vendor vendor = vendorRepository.findByNameAndActiveTrue(vendorName)
                .orElseThrow(() -> new RuntimeException("Vendor not found or inactive"));

        // 2. Fetch endpoint
        VendorApiEndpoint endpoint = endpointRepository.findByVendorIdAndApiKeyAndActiveTrue(vendor.getId(), apiKey)
                .orElseThrow(() -> new RuntimeException("API endpoint not found or inactive"));

        // 3. Fetch headers
        List<VendorApiHeader> headers = headerRepository.findByEndpointId(endpoint.getId());

        // 4. Prepare authentication (example for OAuth)
        String accessToken = null;
        if (!isAuth && "OAUTH".equalsIgnoreCase(vendor.getType())) {
            VendorToken token = tokenRepository.findTopByVendorIdAndExpiresAtAfterOrderByExpiresAtDesc(
             vendor.getId(), LocalDateTime.now())
            .orElseThrow(() -> new RuntimeException("No valid token for vendor"));

            if (token.getAccessToken() == null || token.getAccessToken().isBlank()) {
                throw new RuntimeException("Access token missing for vendor: " + vendor.getId());
            }

            accessToken = token.getAccessToken();
        }

        // 5. Map request
        Object vendorRequest = vendorApiMapper.mapToVendorRequest(vendorName, apiKey, requestDto);

        // 6. Build headers
        HttpHeaders httpHeaders = new HttpHeaders();
        for (VendorApiHeader header : headers) {
            if(header.getHeaderKey().equals("Authorization")){
                header.setHeaderValue(header.getHeaderValue().replace("{access_token}", accessToken));
                System.out.println(header.getHeaderValue());
            }
            httpHeaders.add(header.getHeaderKey(), header.getHeaderValue());
        }
        if (accessToken != null) {
            httpHeaders.add("Authorization", "Bearer " + accessToken);
        }
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        // 7. Build HttpEntity
        HttpEntity<Object> httpEntity = new HttpEntity<>(vendorRequest, httpHeaders);

        // 8. Make the call
        ResponseEntity<Object> vendorResponse = restTemplate.exchange(
                vendor.getBaseUrl() + endpoint.getPath(),
                HttpMethod.valueOf(endpoint.getMethod()),
                httpEntity,
                Object.class
        );

        // 9. Map response
        CommonResponseDto response = vendorApiMapper.mapFromVendorResponse(vendorName, apiKey, vendorResponse.getBody());

        return ResponseEntity.status(response.getStatus() != null ? response.getStatus() : 200).body(response);
    }
} 