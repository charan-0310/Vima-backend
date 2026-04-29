package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationBroadcastEmailRequestDto {
    private UUID organizationId;
    private String subject;
    private String bodyHtml;
    private boolean sendToAll;
    private List<UUID> employeeIds;
    private boolean dryRun;
}
