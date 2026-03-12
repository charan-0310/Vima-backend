package com.vimainsurance.vimaadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEnrollmentConfigResponseDto {

    private UUID id;
    private UUID organizationId;
    private Boolean parentCoverageEnabled;
    private Boolean inLawCoverageEnabled;
    private Integer maxParents;
    private Integer maxInLaws;
    private Integer parentAgeLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
