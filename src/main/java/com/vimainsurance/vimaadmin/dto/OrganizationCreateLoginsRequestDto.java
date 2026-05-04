package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class OrganizationCreateLoginsRequestDto {

    private List<UUID> individualIds;
    private Boolean allEligible;
}
