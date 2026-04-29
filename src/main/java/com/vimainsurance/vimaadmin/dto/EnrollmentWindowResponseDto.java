package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class EnrollmentWindowResponseDto {

    private UUID id;
    private UUID organizationId;
    private String organizationName;
    /** User-facing label (may differ from legal organizationName). */
    private String organizationDisplayName;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String config;
    private UUID createdBy;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    /** Progress: completion rate (submittedCount * 100 / totalEmployees). Same as getEnrollmentProgress. */
    private Double completionRate;
    /** Progress: total employees (max of employees linked to window vs invitations). */
    private Integer totalEmployees;
    /** Progress: count of submissions with status SUBMITTED/APPROVED/REJECTED/ENDORSED. */
    private Integer submittedCount;
}
