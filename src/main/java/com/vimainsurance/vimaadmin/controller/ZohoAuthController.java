package com.vimainsurance.vimaadmin.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.vimainsurance.vimaadmin.config.ZohoConfig;
import com.vimainsurance.vimaadmin.dto.ZohoTokenResponseDto;
import com.vimainsurance.vimaadmin.entity.ZohoToken;
import com.vimainsurance.vimaadmin.repository.ITokenRepository;

@RestController
@RequestMapping("/api/v1/zoho/auth")
public class ZohoAuthController {

    private static final Logger logger = LoggerFactory.getLogger(ZohoAuthController.class);

    @Autowired
    private ZohoConfig zohoConfig;
    @Autowired
    private ITokenRepository tokenRepository;

    @Value("${zoho.crm.client-id}")
    private String clientId;

    @Value("${zoho.crm.client-secret}")
    private String clientSecret;
    
    // ✅ FIX: Inject singleton RestTemplate instead of creating new one per request
    // This prevents memory leaks from per-request RestTemplate creation
    @Autowired
    private RestTemplate restTemplate;

    private final String REDIRECT_URI = "http://localhost:7219/api/v1/zoho/auth/callback";
    private final String AUTH_URL = "https://accounts.zoho.in/oauth/v2/auth";
    private final String TOKEN_URL = "https://accounts.zoho.in/oauth/v2/token";

    @GetMapping("/init")
    public ResponseEntity<String> initializeAuth() {
        logger.info("[correlationId:{}] /init endpoint called", MDC.get("correlationId"));
        String authUrl = AUTH_URL + 
            "?scope=ZohoCRM.modules.ALL,ZohoCRM.settings.ALL" +
            "&client_id=" + clientId +
            "&response_type=code" +
            "&access_type=offline" +
            "&redirect_uri=" + REDIRECT_URI;

        return ResponseEntity.ok(authUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<ZohoTokenResponseDto> handleCallback(@RequestParam String code) {
        logger.info("[correlationId:{}] /callback endpoint called", MDC.get("correlationId"));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("redirect_uri", REDIRECT_URI);
        map.add("code", code);
    
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        
        try {
            ResponseEntity<ZohoTokenResponseDto> tokenResponse = restTemplate.postForEntity(
                TOKEN_URL, 
                request, 
                ZohoTokenResponseDto.class
            );
            ZohoToken zohoToken = new ZohoToken();
            zohoToken.setAccessToken(tokenResponse.getBody().getAccessToken());
            zohoToken.setRefreshToken(tokenResponse.getBody().getRefreshToken());
            zohoToken.setExpiryTime(LocalDateTime.now().plusSeconds(tokenResponse.getBody().getExpiresIn()));
            zohoToken.setId(1L);
            tokenRepository.save(zohoToken);
            return ResponseEntity.ok(tokenResponse.getBody());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 