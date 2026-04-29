package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationRequestDto {
    private UUID organizationId; // for update/delete

    @NotBlank
    private String organizationName;

    @NotBlank
    private String organizationDisplayName;

    @Size(max = 15)
    private String gstin;

    @Size(max = 10)
    private String panNumber;

    @Size(max = 20)
    private String primaryContactName;

    @Email
    private String primaryContactEmail;

    @Size(max = 20)
    private String primaryContactPhone;

    private String status; // ACTIVE/INACTIVE

    private String registeredAddress;

    private String industry;
}


