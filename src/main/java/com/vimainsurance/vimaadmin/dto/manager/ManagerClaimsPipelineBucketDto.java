package com.vimainsurance.vimaadmin.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerClaimsPipelineBucketDto {
    private String status;
    private long count;
    private long agedCount;
}
