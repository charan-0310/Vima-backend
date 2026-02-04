package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentWindowRequestDto {

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @NotNull(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /** Optional on create; for update. Values: SCHEDULED, ACTIVE, CLOSED, CANCELLED */
    private String status;

    /** JSON string for config (reminderEnabled, maxDependents, etc.) */
    private String config = "{}";
}
