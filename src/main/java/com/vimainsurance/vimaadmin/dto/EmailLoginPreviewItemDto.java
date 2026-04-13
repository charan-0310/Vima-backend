package com.vimainsurance.vimaadmin.dto;

import lombok.Data;

/**
 * One row when previewing Keycloak login creation for an endorsement.
 * Status: NEW_USER, EXISTING_KEYCLOAK_USER, INVALID_EMAIL, DUPLICATE_IN_BATCH
 */
@Data
public class EmailLoginPreviewItemDto {

    private String email;
    private String fullName;
    private String individualId;
    /**
     * NEW_USER | EXISTING_KEYCLOAK_USER | INVALID_EMAIL | DUPLICATE_IN_BATCH
     */
    private String status;
    private String detail;
}
