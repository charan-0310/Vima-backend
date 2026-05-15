package com.vimainsurance.vimaadmin.notification;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Low-level HTTP wrapper that posts a JSON body to a Slack Incoming Webhook
 * URL. URL resolution is the caller's responsibility — in this codebase the
 * caller is always {@code NotificationDispatcher} (unified flow) or
 * {@code SlackNotificationUtil} (legacy flow), and they both go through
 * {@link com.vimainsurance.vimaadmin.notification.slack.SlackChannelRouter}
 * to choose the URL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSlackWebhookClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public boolean postMessageToWebhookUrl(String text, String url) {
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
