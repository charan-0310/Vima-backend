package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /api/v1/admin/enrollments/windows/{windowId}/activate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivateWindowResponseDto {

    private String windowStatus;
    private int sent;
    private int failed;
    private List<FailedInvitationDto> failedDetails;
}
