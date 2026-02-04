package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ExtendDeadlineRequestDto {

    @NotNull(message = "Window ID is required")
    private UUID windowId;

    @NotNull(message = "New expiration date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime newExpiresAt;
}
