package com.vimainsurance.vimaadmin.dto;

import java.util.List;

import lombok.Data;

@Data
public class EmployeeOnboardingResponseDto {

    private List<String> successUsers;
    private List<String> failedUsers;
    private int successCount;
    private int failedCount;

    /**
     * Emails where the Keycloak user already existed; ROLE_EMPLOYEE / org group were applied (no new account).
     */
    private List<String> existingKeycloakUserEmails;

    /**
     * When true, Keycloak provisioning runs after the HTTP response (bulk endorsement onboarding only).
     * Counts in this DTO are not final; refresh the endorsement to see updated login status.
     */
    private boolean backgroundProcessing;

    /**
     * Primary (SELF) employees queued when {@link #backgroundProcessing} is true; otherwise 0.
     */
    private int totalEmployees;
}
