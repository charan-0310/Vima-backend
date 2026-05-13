package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Paginated wrapper for {@link FeatureFlagsOrganizationResponse} when the client requests page slicing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagsOrganizationsPageResponse {

    private List<FeatureFlagsOrganizationResponse> items;
    private long totalCount;
    private int page;
    private int pageSize;
    private int totalPages;
}
