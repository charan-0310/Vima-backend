package com.vimainsurance.vimaadmin.dto.claim;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response for GET /api/v1/hr/claims/activity. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyActivityResponse {
    private List<MonthlyActivityItem> activity;
}
