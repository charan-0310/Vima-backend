package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class AdminUserRequestDto {
    private String username;
    private String email;
    private String fullName;
    private String role;
    private List<String> organizations;
    private Boolean isActive;
    private String oauthProvider;
    private String oauthProviderId;
    private String zohoCrmId;
    private LocalDateTime lastLogin;
    private String reportingTo;
} 