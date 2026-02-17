package com.vimainsurance.vimaadmin.dto.claim;

import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeClaimSubmitResponseDto {

    private UUID id;
    private String claimNumber;
    private ClaimStatus internalStatus;
    private String message;
}
