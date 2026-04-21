package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DemoSetupResponseDto {
    private UUID organizationId;
    private String organizationName;
    private String clientEmail;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean reused;
}
