package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationFeatureCountDto {

    private String organizationId;
    private long featureCount;
}
