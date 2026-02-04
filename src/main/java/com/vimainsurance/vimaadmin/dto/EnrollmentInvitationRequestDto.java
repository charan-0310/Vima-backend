package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class EnrollmentInvitationRequestDto {

    private UUID enrollmentWindowId;
    private UUID employeeId;
    @JsonIgnore
    private String tokenHash;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime openedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private Integer reminderCount;
    private LocalDateTime lastReminderAt;
}
