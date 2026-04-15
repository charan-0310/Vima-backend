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
}
