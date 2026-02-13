package com.vimainsurance.vimaadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for GET /api/v1/admin/enrollments/invitations/{id}/link
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationLinkResponseDto {

    private String magicLink;
}
