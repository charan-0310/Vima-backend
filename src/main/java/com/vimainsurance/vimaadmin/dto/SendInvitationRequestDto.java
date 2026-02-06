package com.vimainsurance.vimaadmin.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class SendInvitationRequestDto {

    private UUID windowId;

    /** Optional: when non-empty, invitations are sent to all these employees (bulk). */
    private List<UUID> employeeIds;
}
