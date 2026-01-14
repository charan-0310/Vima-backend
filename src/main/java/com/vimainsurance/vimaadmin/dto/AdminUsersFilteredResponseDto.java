package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import lombok.Data;

@Data
public class AdminUsersFilteredResponseDto {
    private List<AdminUserResponseDto> users;
    private Long totalUsers;
    private Long totalVimaAdmins;
    private Long totalOrganizations;
    private Long totalHRAdmins;
}

