package com.vimainsurance.vimaadmin.dto.claim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusResult {
    private String status;
    private String source;
    private String message;
    private String rawInsurerStatus;
}
