package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class OrganizationEmployeeLoginPreviewDto {

    private boolean eventsTrackingEnabled;
    private OrganizationEmployeeLoginSummaryDto summary;
    private List<OrganizationEmployeeLoginItemDto> items;
}
