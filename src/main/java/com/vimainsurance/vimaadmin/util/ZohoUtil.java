package com.vimainsurance.vimaadmin.util;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.vimainsurance.vimaadmin.dto.ZohoTokenResponseDto;
import com.vimainsurance.vimaadmin.entity.ZohoToken;
import com.vimainsurance.vimaadmin.repository.ITokenRepository;

@Service
public class ZohoUtil {

    @Autowired
    private ITokenRepository tokenRepository;

    @Value("${zoho.crm.client-id}")
    private String clientId;

    @Value("${zoho.crm.client-secret}")
    private String clientSecret;


    public  void refreshZohoAccessToken() {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        
        try {
            String refreshToken = tokenRepository.findById(1L).get().getRefreshToken();
            String url = "https://accounts.zoho.in/oauth/v2/token" +
             "?refresh_token=" + refreshToken +
             "&client_id=" + clientId +
             "&client_secret=" + clientSecret +
             "&grant_type=refresh_token";

            ResponseEntity<ZohoTokenResponseDto> tokenResponse = restTemplate.postForEntity(url, null, ZohoTokenResponseDto.class);
            ZohoToken zohoToken = new ZohoToken();
            zohoToken.setAccessToken(tokenResponse.getBody().getAccessToken());
            zohoToken.setRefreshToken(refreshToken);
            zohoToken.setExpiryTime(LocalDateTime.now().plusSeconds(tokenResponse.getBody().getExpiresIn()));
            zohoToken.setId(1L);
            tokenRepository.save(zohoToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
