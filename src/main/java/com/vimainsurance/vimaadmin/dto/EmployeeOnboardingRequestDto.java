package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class EmployeeOnboardingRequestDto {
    private UUID endorsementId;
    private List<UUID> individualIds;
}

