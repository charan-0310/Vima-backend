package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class SendBulkInvitationsRequestDto {

    private UUID windowId;
    private List<UUID> employeeIds;
}
