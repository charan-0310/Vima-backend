package com.vimainsurance.vimaadmin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRDashBoardResponseDto {

    private OrganizationActivityDto organizationActivityDto;
    private ClaimsActivityDto claimsActivityDto;
    
}
