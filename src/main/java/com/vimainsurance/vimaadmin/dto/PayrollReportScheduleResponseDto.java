package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollReportScheduleResponseDto {

    private UUID id;
    private UUID organizationId;
    private UUID enrollmentWindowId;
    private LocalDate nextRunDate;
    private String frequency;
    private String recipientEmails;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
