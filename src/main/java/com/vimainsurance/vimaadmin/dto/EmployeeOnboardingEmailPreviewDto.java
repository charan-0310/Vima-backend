package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class EmployeeOnboardingEmailPreviewDto {

    private List<EmailLoginPreviewItemDto> items;
    private int totalListed;
    private int newUserCount;
    private int existingKeycloakUserCount;
    private int invalidEmailCount;
    private int duplicateInBatchCount;
}
