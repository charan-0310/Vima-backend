package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentWindowStatsDto {

    private long invitationCount;
    private long invitationSentCount;
    private long invitationCompletedCount;
    private long invitationExpiredCount;
    private long submissionCount;
    private long submissionDraftCount;
    private long submissionSubmittedCount;
    private long submissionApprovedCount;
    private long submissionRejectedCount;
}
