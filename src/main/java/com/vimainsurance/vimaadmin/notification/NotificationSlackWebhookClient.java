package com.vimainsurance.vimaadmin.notification;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vimainsurance.vimaadmin.notification.config.NotificationsProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSlackWebhookClient {

    private final RestTemplate restTemplate;
    private final NotificationsProperties notificationsProperties;
    private final ObjectMapper objectMapper;

    public boolean postMessage(String text) {
        String url = notificationsProperties.getSlackWebhookUrl();
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            String json = objectMapper.writeValueAsString(Map.of("text", text));
            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Slack webhook post failed: {}", e.getMessage());
            return false;
        }
    }
}
