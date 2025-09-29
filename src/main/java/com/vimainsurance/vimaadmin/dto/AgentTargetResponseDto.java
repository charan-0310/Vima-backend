package com.vimainsurance.vimaadmin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AgentTargetResponseDto {
    private Long id;
    private String agentUsername;
    private Integer month;
    private Integer year;
    private Long packageId;
    private OffsetDateTime createdAt;
} 