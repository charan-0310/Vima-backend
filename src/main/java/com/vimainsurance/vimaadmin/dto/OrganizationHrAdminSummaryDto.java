package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class OrganizationHrAdminSummaryDto {
    private UUID individualId;
    private String email;
    private String fullName;
}
