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
    private static final String SLACK_CHAT_POST_MESSAGE_URL = "https://slack.com/api/chat.postMessage";

    private final RestTemplate restTemplate;
    private final NotificationsProperties notificationsProperties;
    private final ObjectMapper objectMapper;

    public boolean postMessage(String text) {
        String url = notificationsProperties.getSlackWebhookUrl();
        return postMessageToWebhookUrl(text, url);
    }

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

    public boolean postMessageToChannel(String channelId, String text) {
        String botToken = notificationsProperties.getSlackBotToken();
        if (botToken == null || botToken.isBlank() || channelId == null || channelId.isBlank()) {
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + botToken.trim());
            String json = objectMapper.writeValueAsString(Map.of("channel", channelId.trim(), "text", text));
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    SLACK_CHAT_POST_MESSAGE_URL,
                    new HttpEntity<>(json, headers),
                    String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(resp.getBody(), Map.class);
            return Boolean.TRUE.equals(body.get("ok"));
        } catch (Exception e) {
            log.warn("Slack bot post failed: {}", e.getMessage());
            return false;
        }
    }
}
