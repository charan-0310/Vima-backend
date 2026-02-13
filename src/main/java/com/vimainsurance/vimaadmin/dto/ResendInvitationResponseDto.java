package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /api/v1/admin/enrollments/{invitationId}/resend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendInvitationResponseDto {

    private boolean sent;
    private String email;
    /** Enrollment URL so admin can copy if email not received */
    private String magicLink;
}
