package com.vimainsurance.vimaadmin.dto;

import lombok.Data;

@Data
public class OrganizationEmployeeLoginSummaryDto {

    private int totalActive;
    private int withLogin;
    private int withoutLogin;
    private int invalid;
}
