package com.vimainsurance.vimaadmin.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class OrganizationEmployeeLoginItemDto {

    private UUID individualId;
    private String fullName;
    private String employeeNumber;
    private String email;
    /** NEW_USER | EXISTING_KEYCLOAK_USER | INVALID_EMAIL | DUPLICATE_IN_BATCH */
    private String status;
    private Instant lastLoginAt;
    private String detail;
    /**
     * Total number of non-blank Health IDs across the primary employee and their active dependents.
     * Used by the Logins page to enable/disable the "Email health cards" action without an extra round trip.
     */
    private int healthIdCount;
}
