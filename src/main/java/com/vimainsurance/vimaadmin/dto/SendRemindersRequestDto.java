package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class SendRemindersRequestDto {

    @NotNull(message = "Window ID is required")
    private UUID windowId;

    /** Optional: when non-empty, send reminders only to these employees. */
    private List<UUID> employeeIds;
}
