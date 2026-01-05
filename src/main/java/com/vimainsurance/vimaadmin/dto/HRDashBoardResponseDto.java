package com.vimainsurance.vimaadmin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.vimainsurance.vimaadmin.dto.OrganizationActivityDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HRDashBoardResponseDto {

    private OrganizationActivityDto organizationActivityDto;
    
}
