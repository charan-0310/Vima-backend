package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for enrollment submission list (HR portal).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionListItemDto {

    private UUID id;
    private String referenceNumber;
    private String status;
    private String employeeName;
    private String employeeEmail;
    private String employeeNumber;
    private UUID employeeId;
    private String windowName;
    private UUID windowId;
    private String organizationName;
    private UUID organizationId;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
