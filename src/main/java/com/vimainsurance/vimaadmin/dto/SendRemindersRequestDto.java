package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class SendRemindersRequestDto {

    @NotNull(message = "Window ID is required")
    private UUID windowId;
}
