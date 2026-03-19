package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEnrollmentConfigRequestDto {

    private UUID organizationId;
    private Boolean parentCoverageEnabled;
    private Boolean inLawCoverageEnabled;
    private Integer maxParents;
    private Integer maxInLaws;
    private Integer parentAgeLimit;
}
