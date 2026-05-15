package com.vimainsurance.vimaadmin.dto.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerPolicyExpirySegmentDto {
    private String segment;
    private String label;
    private long count;
}
