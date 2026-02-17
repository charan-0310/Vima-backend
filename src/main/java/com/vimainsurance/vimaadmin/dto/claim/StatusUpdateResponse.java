package com.vimainsurance.vimaadmin.dto.claim;

import java.time.LocalDateTime;
import java.util.UUID;

import com.vimainsurance.vimaadmin.enums.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response for PUT /api/v1/admin/claims/{claimId}/status. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateResponse {
    private UUID claimId;
    private ClaimStatus oldStatus;
    private ClaimStatus newStatus;
    private LocalDateTime updatedAt;
}
