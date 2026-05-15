package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class OrganizationCreateHrAdminRequestDto {
    /** Primary employee in this organization to promote to HR admin. */
    @JsonAlias({ "individual_id" })
    private UUID individualId;

    /**
     * Optional display name from the UI when the employee row has a composed name but DB full_name is empty.
     */
    private String fullName;
}
