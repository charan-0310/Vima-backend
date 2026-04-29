package com.vimainsurance.vimaadmin.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationBroadcastFailedRecipientDto {
    private UUID employeeId;
    private String email;
    private String reason;
}
