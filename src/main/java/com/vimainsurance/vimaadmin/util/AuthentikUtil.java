package com.vimainsurance.vimaadmin.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.dto.AuthentikGroupCreationDto;

@Service
public class AuthentikUtil {


    @Autowired
    private RestTemplate restTemplate;

    @Value("${authentik.url}")
    private String authentikUrl;

    @Value("${authentik.token}")
    private String authentikToken;

    public void createGroup(AuthentikGroupCreationDto groupCreationDto) {
        String url = authentikUrl + "/core/groups/";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authentikToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<AuthentikGroupCreationDto> request = new HttpEntity<>(groupCreationDto, headers);
        restTemplate.postForEntity(url, request, Void.class);
    }
}
