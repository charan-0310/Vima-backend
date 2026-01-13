package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import lombok.Data;

@Data
public class AuthentikGroupsResponseDto {
    private List<RoleDto> roles;
    private List<OrganizationDto> organizations;
}

