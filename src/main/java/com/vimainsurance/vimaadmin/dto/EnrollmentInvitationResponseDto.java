package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrollmentInvitationResponseDto {

    private UUID id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Enrollment URL for this invitation (included when available, e.g. after send/resend so admin can copy if email not received) */
    private String magicLink;
}
