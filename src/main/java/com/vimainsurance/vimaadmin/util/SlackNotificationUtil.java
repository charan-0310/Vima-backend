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

import com.vimainsurance.vimaadmin.entity.Endorsement;


@Component
public class SlackNotificationUtil {
    private static final Logger logger = LoggerFactory.getLogger(SlackNotificationUtil.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${slack.webhook.url:https://hooks.slack.com/services/REDACTED}")
    private String slackWebhookUrl;

    @Value("${slack.reminder.channel.url:https://hooks.slack.com/services/REDACTED}")
    private String slackReminderChannelUrl;

    @Value("${slack.sendmessage}")
    private Boolean slackSendMessage;
    
    public SlackNotificationUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public void sendSlackMessage(String message, Boolean isPolicyWin) {
        if(!slackSendMessage) {
            logger.warn("Slack notification is disabled. Skipping Slack notification.");
            return;
        }
        if (isPolicyWin && slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            logger.warn("Slack webhook URL is not configured. Skipping Slack notification.");
            return;
        }
        else if (!isPolicyWin && slackReminderChannelUrl == null || slackReminderChannelUrl.isEmpty()) {
            logger.warn("Slack reminder channel URL is not configured. Skipping Slack notification.");
            return;
        }
        else if (!isPolicyWin) {
            slackWebhookUrl = slackReminderChannelUrl;
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
    
    public void sendSlackMessage(String title, String message, Boolean isPolicyWin) {
        if(!slackSendMessage) {
            logger.warn("Slack notification is disabled. Skipping Slack notification.");
            return;
        }
        if (isPolicyWin && slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            logger.warn("Slack webhook URL is not configured. Skipping Slack notification.");
            return;
        }
        else if (!isPolicyWin && slackReminderChannelUrl == null || slackReminderChannelUrl.isEmpty()) {
            logger.warn("Slack reminder channel URL is not configured. Skipping Slack notification.");
            return;
        }
        else if (!isPolicyWin) {
            slackWebhookUrl = slackReminderChannelUrl;
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

    public String buildEndorsementNotificationMessage(Endorsement endorsement) {
        if (endorsement == null) {
            return "";
        }
        
        StringBuilder message = new StringBuilder();
        
        // Format endorsement type
        String endorsementTypeDisplay = formatEndorsementType(endorsement.getEndorsementType() != null ? endorsement.getEndorsementType().name() : "UNKNOWN");
        
        // Get organization name
        String organizationName = endorsement.getOrganization() != null 
            ? endorsement.getOrganization().getOrganizationName() 
            : "N/A";
        
        // Get uploaded by name
        String uploadedByName = "N/A";
        if (endorsement.getUploadedBy() != null) {
            uploadedByName = endorsement.getUploadedBy().getFullName() != null 
                ? endorsement.getUploadedBy().getFullName() 
                : (endorsement.getUploadedBy().getUsername() != null 
                    ? endorsement.getUploadedBy().getUsername() 
                    : "N/A");
        }
        
        // Build message with emojis
        message.append(":clipboard: Type: ").append(endorsementTypeDisplay).append("\n");
        message.append(":busts_in_silhouette: Total Employees: ").append(endorsement.getTotalEmployees() != null ? endorsement.getTotalEmployees() : 0).append("\n");
        message.append(":family: Total Dependents: ").append(endorsement.getTotalDependents() != null ? endorsement.getTotalDependents() : 0).append("\n");
        message.append(":bust_in_silhouette: Uploaded By: ").append(uploadedByName).append("\n");
        message.append(":office: Organization: ").append(organizationName);
        
        return message.toString();
    }
    
    private String formatEndorsementType(String endorsementType) {
        if (endorsementType == null || endorsementType.isEmpty()) {
            return "Unknown";
        }
        
        // Convert enum names to readable format
        // e.g., ADDITION -> Addition, DELETION -> Deletion
        String formatted = endorsementType.replace("_", " ");
        String[] words = formatted.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
        }
        
        return result.toString();
    }
    
}

