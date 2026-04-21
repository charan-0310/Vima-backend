package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WellnessAccessLogResponseDto {
    private UUID id;
    private String partnerName;
    private String partnerSlug;
    private UUID employeeId;
    private String employeeNumber;
    private String userIdentifier;
    private String accessType;
    private String status;
    private String errorMessage;
    private LocalDateTime accessedAt;
}
