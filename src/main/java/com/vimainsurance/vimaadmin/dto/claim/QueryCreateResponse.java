package com.vimainsurance.vimaadmin.dto.claim;

import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;
import com.vimainsurance.vimaadmin.enums.QueryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryCreateResponse {
    private UUID id;
    private QueryStatus queryStatus;
    private ClaimStatus claimStatus;
}
