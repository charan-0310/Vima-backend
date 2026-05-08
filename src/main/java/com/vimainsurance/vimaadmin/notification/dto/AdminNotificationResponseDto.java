package com.vimainsurance.vimaadmin.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vimainsurance.vimaadmin.notification.enums.NotificationCategory;
import com.vimainsurance.vimaadmin.notification.enums.NotificationEventType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminNotificationResponseDto {
    private UUID id;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private NotificationEventType eventType;
    private NotificationCategory category;
    private String title;
    private String body;
    private String deepLinkUrl;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;
    private UUID companyId;
    private Boolean starred;
    private String actorName;
    private String organizationName;
    private String receiverEmail;
    private String receiverName;
    private String receiverRole;
    private String creatorEmail;
    private String creatorName;
    private String creatorRole;
}
