package com.vimainsurance.vimaadmin.dto;

import lombok.Data;

/**
 * Dashboard counts for User Management, scoped by the same filters as {@code GET /admin-users/filtered}
 * but without loading a user page.
 */
@Data
public class UserManagementStatsDto {
    private Long totalUsers;
    private Long totalVimaAdmins;
    private Long totalOrganizations;
    private Long totalHRAdmins;
}
