package com.vimainsurance.vimaadmin.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
public class SlackNotificationUtil {
    private static final Logger logger = LoggerFactory.getLogger(SlackNotificationUtil.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${slack.webhook.url:https://hooks.slack.com/services/REDACTED}")
    private String slackWebhookUrl;
    
    public SlackNotificationUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public void sendSlackMessage(String message) {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            logger.warn("Slack webhook URL is not configured. Skipping Slack notification.");
            return;
        }
        
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("text", message);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(slackWebhookUrl, request, String.class);
            logger.info("Slack notification sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send Slack notification: {}", e.getMessage(), e);
        }
    }
    
    public void sendSlackMessage(String title, String message) {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            logger.warn("Slack webhook URL is not configured. Skipping Slack notification.");
            return;
        }
        
        try {
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("color", "good");
            attachment.put("title", title);
            attachment.put("text", message);
            attachment.put("ts", System.currentTimeMillis() / 1000);
            
            payload.put("attachments", new Object[]{attachment});
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(slackWebhookUrl, request, String.class);
            logger.info("Slack notification sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send Slack notification: {}", e.getMessage(), e);
        }
    }
}

