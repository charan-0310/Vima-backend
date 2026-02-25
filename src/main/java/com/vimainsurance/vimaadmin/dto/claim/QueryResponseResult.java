package com.vimainsurance.vimaadmin.dto.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponseResult {
    private boolean success;
    private boolean requiresManualForward;
    private String message;
}
