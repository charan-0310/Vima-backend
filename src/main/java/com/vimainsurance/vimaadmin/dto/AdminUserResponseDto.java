package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class AdminUserResponseDto {

    private String username;
    private String email;
    private String fullName;
    private String role;
    private List<String> roles;
    private List<String> organizations;
    private Boolean isActive;
    private String oauthProvider;
    private String oauthProviderId;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private String agentId;
    private String reportingTo;
} 